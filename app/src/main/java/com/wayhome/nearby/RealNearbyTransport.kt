package com.wayhome.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.domain.model.Message
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.PeerConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real offline transport over Google Nearby Connections (P2P_CLUSTER).
 * Phone A <-> Nearby (BT/BLE + WiFi) <-> Phone B. No internet required.
 *
 * Platform notes (isolated here):
 * - Requires Play Services, BT+WiFi radios ON (API no longer auto-enables them, late 2026).
 * - Foreground only in MVP; Doze/background kills advertising.
 * - Endpoint name carries ONLY tempId|city|area (no exact location/identity).
 * - Full Advertise payload (lat/lng/looking) is exchanged post-connection.
 */
@Singleton
class RealNearbyTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wire: WireProtocol,
    @com.wayhome.di.ApplicationScope private val appScope: CoroutineScope
) : NearbyTransport {

    private val client: ConnectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val peers = MutableStateFlow<Map<String, Peer>>(emptyMap())
    private val peersPublic = MutableStateFlow<List<Peer>>(emptyList())
    private val incoming = MutableSharedFlow<IncomingEnvelope>(extraBufferCapacity = 128)
    private val connected = mutableSetOf<String>()

    private var selfTempId: String = "Traveller ???"
    private var selfDestination: Destination = Destination()
    private var selfLooking: Boolean = true

    private fun refresh() {
        peersPublic.value = peers.value.values.toList()
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Auto-accept: MVP trust model is explicit Connect tap + temporary IDs.
            client.acceptConnection(endpointId, payloadCallback)
            peers.update { map ->
                val existing = map[endpointId]
                val parsed = parseEndpointName(endpointId, info.endpointName)
                map + (endpointId to (existing ?: parsed).copy(connectionState = PeerConnectionState.CONNECTING))
            }
            refresh()
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                synchronized(connected) { connected.add(endpointId) }
                peers.update { map ->
                    map + (endpointId to ((map[endpointId] ?: Peer(endpointId, endpointId, Destination()))
                        .copy(connectionState = PeerConnectionState.CONNECTED)))
                }
                // Exchange full profile immediately after connect.
                send(
                    endpointId,
                    Message.Advertise(
                        id = UUID.randomUUID().toString(),
                        senderId = selfTempId,
                        timestamp = System.currentTimeMillis(),
                        tempName = selfTempId,
                        city = selfDestination.city,
                        area = selfDestination.area,
                        lat = selfDestination.location.lat,
                        lng = selfDestination.location.lng,
                        lookingForPartners = selfLooking
                    )
                )
            } else {
                peers.update { map ->
                    map + (endpointId to ((map[endpointId] ?: Peer(endpointId, endpointId, Destination()))
                        .copy(connectionState = PeerConnectionState.FOUND)))
                }
            }
            refresh()
        }

        override fun onDisconnected(endpointId: String) {
            synchronized(connected) { connected.remove(endpointId) }
            peers.update { map ->
                map[endpointId]?.let { map + (endpointId to it.copy(connectionState = PeerConnectionState.FOUND)) } ?: map
            }
            refresh()
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId != WAYHOME_SERVICE_ID) return
            peers.update { map ->
                if (map.containsKey(endpointId)) map
                else map + (endpointId to parseEndpointName(endpointId, info.endpointName))
            }
            refresh()
        }

        override fun onEndpointLost(endpointId: String) {
            peers.update { map -> map - endpointId }
            refresh()
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            val msg = wire.decode(bytes) ?: return
            if (msg is Message.Advertise) {
                peers.update { map ->
                    val existing = map[endpointId]
                    map + (endpointId to Peer(
                        endpointId = endpointId,
                        tempId = msg.tempName,
                        destination = Destination(msg.city, msg.area, Location(msg.lat, msg.lng)),
                        lookingForPartners = msg.lookingForPartners,
                        connectionState = existing?.connectionState ?: PeerConnectionState.CONNECTED
                    ))
                }
                refresh()
            } else {
                appScope.launch { incoming.emit(IncomingEnvelope(msg, endpointId)) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    override fun startDiscovery() {
        runCatching {
            client.startDiscovery(
                WAYHOME_SERVICE_ID, discoveryCallback,
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            )
        }
    }

    override fun stopDiscovery() {
        runCatching { client.stopDiscovery() }
    }

    override fun startAdvertising(tempId: String, destination: Destination, looking: Boolean) {
        selfTempId = tempId
        selfDestination = destination
        selfLooking = looking
        val name = endpointName(tempId, destination)
        runCatching {
            client.startAdvertising(
                name, WAYHOME_SERVICE_ID, lifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            )
        }
    }

    override fun stopAdvertising() {
        runCatching { client.stopAdvertising() }
    }

    override fun connect(peerEndpointId: String) {
        peers.update { map ->
            map[peerEndpointId]?.let { map + (peerEndpointId to it.copy(connectionState = PeerConnectionState.CONNECTING)) } ?: map
        }
        refresh()
        runCatching {
            client.requestConnection(selfTempId, peerEndpointId, lifecycleCallback)
        }
    }

    override fun disconnect(peerEndpointId: String) {
        runCatching { client.disconnectFromEndpoint(peerEndpointId) }
        synchronized(connected) { connected.remove(peerEndpointId) }
    }

    override fun send(peerEndpointId: String, message: Message) {
        val targets = if (peerEndpointId == "broadcast") snapshotConnected() else listOf(peerEndpointId)
        val bytes = wire.encode(message)
        targets.forEach { ep ->
            runCatching { client.sendPayload(ep, Payload.fromBytes(bytes)) }
        }
    }

    override fun broadcast(message: Message) {
        val bytes = wire.encode(message)
        snapshotConnected().forEach { ep ->
            runCatching { client.sendPayload(ep, Payload.fromBytes(bytes)) }
        }
    }

    private fun snapshotConnected(): List<String> = synchronized(connected) { connected.toList() }

    override fun observePeers(): Flow<List<Peer>> = peersPublic.asStateFlow()
    override fun observeIncoming(): Flow<IncomingEnvelope> = incoming.asSharedFlow()

    companion object {
        fun endpointName(tempId: String, d: Destination): String =
            "$tempId|${d.city}|${d.area}".take(100)

        fun parseEndpointName(endpointId: String, raw: String): Peer {
            val parts = raw.split("|")
            return Peer(
                endpointId = endpointId,
                tempId = parts.getOrElse(0) { raw }.ifBlank { endpointId },
                destination = Destination(
                    city = parts.getOrElse(1) { "" },
                    area = parts.getOrElse(2) { "" }
                ),
                lookingForPartners = true,
                connectionState = PeerConnectionState.FOUND
            )
        }
    }
}
