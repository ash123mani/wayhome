package com.wayhome.nearby

import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.domain.model.Message
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.PeerConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Demo transport for emulator / single-device runs where real BLE
 * discovery is unavailable. Advertises two canned peers and echoes
 * direct messages back so the UI flow is fully clickable.
 */
@Singleton
class FakeNearbyTransport @Inject constructor(
    @com.wayhome.di.ApplicationScope private val appScope: CoroutineScope
) : NearbyTransport {
    private val peers = MutableStateFlow<List<Peer>>(emptyList())
    private val incoming = MutableSharedFlow<IncomingEnvelope>(extraBufferCapacity = 64)
    private var advertising = false
    private var discovering = false

    override fun startDiscovery() {
        discovering = true
        appScope.launch {
            delay(800)
            if (!discovering) return@launch
            peers.value = listOf(
                Peer(
                    endpointId = "fake-ep-281",
                    tempId = "Traveller 281",
                    destination = Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499)),
                    lookingForPartners = true
                ),
                Peer(
                    endpointId = "fake-ep-923",
                    tempId = "Traveller 923",
                    destination = Destination("Bengaluru", "Marathahalli", Location(12.9591, 77.6974)),
                    lookingForPartners = true
                )
            )
        }
    }

    override fun stopDiscovery() { discovering = false }

    override fun startAdvertising(tempId: String, destination: Destination, looking: Boolean) {
        advertising = true
    }

    override fun stopAdvertising() { advertising = false }

    override fun connect(peerEndpointId: String) {
        appScope.launch {
            peers.value = peers.value.map {
                if (it.endpointId == peerEndpointId) it.copy(connectionState = PeerConnectionState.CONNECTING)
                else it
            }
            delay(600)
            peers.value = peers.value.map {
                if (it.endpointId == peerEndpointId) it.copy(connectionState = PeerConnectionState.CONNECTED)
                else it
            }
        }
    }

    override fun disconnect(peerEndpointId: String) {
        peers.value = peers.value.map {
            if (it.endpointId == peerEndpointId) it.copy(connectionState = PeerConnectionState.FOUND)
            else it
        }
    }

    override fun send(peerEndpointId: String, message: Message) {
        appScope.launch {
            // Echo a canned reply for Text so single-device demo feels alive.
            if (message is Message.Text) {
                delay(900)
                val peer = peers.value.firstOrNull { it.endpointId == peerEndpointId }
                incoming.emit(
                    IncomingEnvelope(
                        Message.Text(
                            id = UUID.randomUUID().toString(),
                            senderId = peer?.tempId ?: "Traveller 281",
                            timestamp = System.currentTimeMillis(),
                            conversationId = message.conversationId,
                            text = "Got it! I'm near ${peer?.destination?.area ?: "Whitefield"}. Let's share a cab 👍"
                        ),
                        peerEndpointId
                    )
                )
            } else {
                incoming.emit(IncomingEnvelope(message, peerEndpointId))
            }
        }
    }

    override fun broadcast(message: Message) {
        appScope.launch {
            peers.value.filter { it.connectionState == PeerConnectionState.CONNECTED }
                .forEach { incoming.emit(IncomingEnvelope(message, it.endpointId)) }
            // Loop back own group messages so sender sees them instantly.
            if (message is Message.GroupText || message is Message.GroupCreated) {
                incoming.emit(IncomingEnvelope(message, "self"))
            }
        }
    }

    override fun observePeers(): Flow<List<Peer>> = peers.asStateFlow()
    override fun observeIncoming(): Flow<IncomingEnvelope> = incoming.asSharedFlow()
}
