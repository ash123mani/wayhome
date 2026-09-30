package com.wayhome.domain.repository

import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Message
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.RideGroup
import com.wayhome.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getOrCreateTempIdentity(): UserProfile
    suspend fun updateDestination(destination: Destination)
    suspend fun setDiscoverable(looking: Boolean)
    suspend fun stopSharing()
}

interface DiscoveryRepository {
    fun observePeers(): Flow<List<Peer>>
    fun observeConnectionBanner(): Flow<String>
    suspend fun connect(peer: Peer)
    suspend fun disconnect(endpointId: String)
    fun blockedIds(): Set<String>
    suspend fun blockUser(tempId: String)
}

interface ChatRepository {
    fun observeDirectMessages(conversationId: String): Flow<List<Message.Text>>
    fun observeGroupMessages(groupId: String): Flow<List<Message.GroupText>>
    fun observeGroups(): Flow<List<RideGroup>>
    suspend fun sendDirect(conversationId: String, peerEndpointId: String, text: String)
    suspend fun createGroup(name: String, memberTempIds: List<String>): RideGroup
    suspend fun sendGroupText(groupId: String, text: String)
    suspend fun joinGroup(groupId: String)
    suspend fun leaveGroup(groupId: String)
    /** Stub for future online backend. No-op in offline MVP. */
    suspend fun syncRemote(): Result<Unit>
}
