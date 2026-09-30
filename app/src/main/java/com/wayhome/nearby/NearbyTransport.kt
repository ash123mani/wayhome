package com.wayhome.nearby

import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Message
import com.wayhome.domain.model.Peer
import kotlinx.coroutines.flow.Flow

const val WAYHOME_SERVICE_ID = "com.wayhome.NEARBY"

/** Transport abstraction — app code never touches GMS Nearby directly. */
interface NearbyTransport {
    fun startDiscovery()
    fun stopDiscovery()
    fun startAdvertising(tempId: String, destination: Destination, looking: Boolean)
    fun stopAdvertising()
    fun connect(peerEndpointId: String)
    fun disconnect(peerEndpointId: String)
    fun send(peerEndpointId: String, message: Message)
    fun broadcast(message: Message)
    fun observePeers(): Flow<List<Peer>>
    fun observeIncoming(): Flow<IncomingEnvelope>
}

data class IncomingEnvelope(val message: Message, val fromEndpointId: String)

/** Daos / repositories implement this to receive decoded wire messages. */
interface NearbyMessageSink {
    suspend fun onMessageReceived(message: Message, fromEndpointId: String)
}
