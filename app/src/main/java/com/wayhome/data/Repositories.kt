package com.wayhome.data

import android.content.Context
import com.wayhome.core.IdentityManager
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.wayhome.data.local.GroupMemberEntity
import com.wayhome.data.local.MessageEntity
import com.wayhome.data.local.PeerEntity
import com.wayhome.data.local.WayHomeDb
import com.wayhome.di.ApplicationScope
import com.wayhome.domain.matcher.DestinationMatcher
import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Message
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.PeerConnectionState
import com.wayhome.domain.model.RideGroup
import com.wayhome.domain.model.UserProfile
import com.wayhome.domain.repository.ChatRepository
import com.wayhome.domain.repository.DiscoveryRepository
import com.wayhome.domain.repository.ProfileRepository
import com.wayhome.nearby.IncomingEnvelope
import com.wayhome.nearby.NearbyTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val identity: IdentityManager,
    private val transport: NearbyTransport
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = identity.observeProfile()
    override suspend fun getOrCreateTempIdentity(): UserProfile = identity.getOrCreate()
    override suspend fun updateDestination(destination: Destination) = identity.updateDestination(destination)
    override suspend fun setDiscoverable(looking: Boolean) = identity.setLooking(looking)
    override suspend fun stopSharing() {
        identity.setLooking(false)
        transport.stopAdvertising()
        transport.stopDiscovery()
    }
}

@Singleton
class DiscoveryRepositoryImpl @Inject constructor(
    private val transport: NearbyTransport,
    private val db: WayHomeDb,
    @ApplicationScope private val scope: CoroutineScope
) : DiscoveryRepository {
    private val banner = MutableStateFlow("⚪ Nearby off — tap “Find people going my way”")
    private val blocked = mutableSetOf<String>()

    init {
        // Cache live peers locally (offline-first).
        scope.launch {
            transport.observePeers().collect { peers ->
                peers.forEach { p ->
                    db.peerDao().upsert(
                        PeerEntity(p.endpointId, p.tempId, p.destination.city, p.destination.area,
                            p.destination.location.lat, p.destination.location.lng,
                            p.lookingForPartners, System.currentTimeMillis())
                    )
                }
                banner.value = if (peers.isEmpty()) "🔎 Searching nearby..."
                else "🟢 Nearby mode active · ${peers.size} found"
            }
        }
    }

    override fun observePeers(): Flow<List<Peer>> =
        combine(transport.observePeers(), db.peerDao().observePeers()) { live, cached ->
            val liveIds = live.map { it.endpointId }.toSet()
            val merged = live.toMutableList()
            cached.filter { it.endpointId !in liveIds }.forEach {
                merged.add(
                    Peer(it.endpointId, it.tempId, Destination(it.city, it.area,
                        com.wayhome.domain.model.Location(it.lat, it.lng)), it.looking,
                        PeerConnectionState.LOST)
                )
            }
            merged.filter { it.tempId !in blocked && it.lookingForPartners }
        }

    override fun observeConnectionBanner(): Flow<String> = banner
    override suspend fun connect(peer: Peer) { transport.connect(peer.endpointId) }
    override suspend fun disconnect(endpointId: String) { transport.disconnect(endpointId) }
    override fun blockedIds(): Set<String> = blocked.toSet()
    override suspend fun blockUser(tempId: String) {
        blocked.add(tempId)
        banner.value = "🚫 Blocked $tempId"
    }
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val transport: NearbyTransport,
    private val db: WayHomeDb,
    private val identity: IdentityManager,
    @ApplicationScope private val scope: CoroutineScope
) : ChatRepository {
    private val seenIds = mutableSetOf<String>()

    init {
        scope.launch {
            transport.observeIncoming().collect { handleIncoming(it) }
        }
    }

    private suspend fun handleIncoming(env: IncomingEnvelope) {
        val msg = env.message
        synchronized(seenIds) {
            if (!seenIds.add(msg.id)) return // duplicate flood copy
        }
        if (db.messageDao().exists(msg.id) > 0) return
        when (msg) {
            is Message.Text -> db.messageDao().insert(
                MessageEntity(msg.id, msg.conversationId, "TEXT", msg.senderId, msg.text, msg.timestamp, msg.ttl)
            )
            is Message.GroupText -> {
                db.messageDao().insert(
                    MessageEntity(msg.id, msg.groupId, "GROUP_TEXT", msg.senderId, msg.text, msg.timestamp, msg.ttl)
                )
                forward(msg)
            }
            is Message.GroupCreated -> {
                db.groupDao().upsertGroup(
                    com.wayhome.data.local.GroupEntity(msg.groupId, msg.groupName, msg.creatorId)
                )
                msg.members.forEach { db.groupDao().addMember(GroupMemberEntity(msg.groupId, it)) }
                db.messageDao().insert(
                    MessageEntity(msg.id, msg.groupId, "GROUP_CREATED", msg.senderId,
                        "Group '${msg.groupName}' created", msg.timestamp, msg.ttl)
                )
                forward(msg)
            }
            is Message.GroupJoin -> {
                db.groupDao().addMember(GroupMemberEntity(msg.groupId, msg.memberId))
                forward(msg)
            }
            is Message.Advertise -> Unit // handled by transport peer list
        }
    }

    /** Bitchat-style flood: rebroadcast with ttl-1 to all connected peers. */
    private fun forward(msg: Message) {
        val next = when (msg) {
            is Message.GroupText -> if (msg.ttl <= 0) null else msg.copy(ttl = msg.ttl - 1)
            is Message.GroupCreated -> if (msg.ttl <= 0) null else msg.copy(ttl = msg.ttl - 1)
            is Message.GroupJoin -> if (msg.ttl <= 0) null else msg.copy(ttl = msg.ttl - 1)
            else -> null
        } ?: return
        transport.broadcast(next)
    }

    private fun directThreadId(a: String, b: String): String =
        listOf(a, b).sorted().joinToString("|")

    override fun observeDirectMessages(conversationId: String) =
        db.messageDao().observeThread(conversationId).map { list ->
            list.map {
                Message.Text(it.id, it.senderId, it.timestamp, it.ttl, it.threadId, it.text)
            }
        }

    override fun observeGroupMessages(groupId: String) =
        db.messageDao().observeThread(groupId).map { list ->
            list.filter { it.kind == "GROUP_TEXT" }.map {
                Message.GroupText(it.id, it.senderId, it.timestamp, it.ttl, it.threadId, it.text)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeGroups(): Flow<List<RideGroup>> =
        db.groupDao().observeGroups().flatMapLatest { groups ->
            if (groups.isEmpty()) flowOf(emptyList())
            else combine(groups.map { g ->
                db.groupDao().observeMembers(g.groupId).map { members ->
                    RideGroup(g.groupId, g.name, g.creatorId, members.map { it.memberId })
                }
            }) { it.toList() }
        }

    override suspend fun sendDirect(conversationId: String, peerEndpointId: String, text: String) {
        val me = identity.getOrCreate().tempId
        val msg = Message.Text(UUID.randomUUID().toString(), me, System.currentTimeMillis(), 0, conversationId, text)
        db.messageDao().insert(MessageEntity(msg.id, conversationId, "TEXT", me, text, msg.timestamp, 0, "SENT"))
        synchronized(seenIds) { seenIds.add(msg.id) }
        transport.send(peerEndpointId, msg)
    }

    override suspend fun createGroup(name: String, memberTempIds: List<String>): RideGroup {
        val me = identity.getOrCreate().tempId
        val groupId = "grp-${UUID.randomUUID().toString().take(8)}"
        val members = (memberTempIds + me).distinct()
        db.groupDao().upsertGroup(com.wayhome.data.local.GroupEntity(groupId, name, me))
        members.forEach { db.groupDao().addMember(GroupMemberEntity(groupId, it)) }
        val created = Message.GroupCreated(UUID.randomUUID().toString(), me, System.currentTimeMillis(), 3, groupId, name, me, members)
        synchronized(seenIds) { seenIds.add(created.id) }
        transport.broadcast(created)
        return RideGroup(groupId, name, me, members)
    }

    override suspend fun sendGroupText(groupId: String, text: String) {
        val me = identity.getOrCreate().tempId
        val msg = Message.GroupText(UUID.randomUUID().toString(), me, System.currentTimeMillis(), 3, groupId, text)
        db.messageDao().insert(MessageEntity(msg.id, groupId, "GROUP_TEXT", me, text, msg.timestamp, 3, "SENT"))
        synchronized(seenIds) { seenIds.add(msg.id) }
        transport.broadcast(msg)
    }

    override suspend fun joinGroup(groupId: String) {
        val me = identity.getOrCreate().tempId
        db.groupDao().addMember(GroupMemberEntity(groupId, me))
        val join = Message.GroupJoin(UUID.randomUUID().toString(), me, System.currentTimeMillis(), 3, groupId, me)
        transport.broadcast(join)
    }

    override suspend fun leaveGroup(groupId: String) {
        val me = identity.getOrCreate().tempId
        db.groupDao().removeMember(groupId, me)
    }

    override suspend fun syncRemote(): Result<Unit> =
        Result.success(Unit) // RemoteDataSource stub — offline MVP has no backend.
}

/** Multi-city seeded catalog (assets/destinations.json) + custom fallback. */
@Singleton
class DestinationCatalog @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val json: kotlinx.serialization.json.Json
) {
    data class Area(val name: String, val lat: Double?, val lng: Double?)
    data class City(val name: String, val areas: List<Area>)

    val cities: List<City> by lazy { load() }

    private fun load(): List<City> = runCatching {
        val text = context.assets.open("destinations.json").bufferedReader().readText()
        val root = json.parseToJsonElement(text).jsonObject
        root["cities"]!!.jsonArray.map { c ->
            val o = c.jsonObject
            City(
                o["name"]!!.jsonPrimitive.content,
                o["areas"]!!.jsonArray.map { a ->
                    val ao = a.jsonObject
                    Area(ao["name"]!!.jsonPrimitive.content,
                        ao["lat"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                        ao["lng"]?.jsonPrimitive?.content?.toDoubleOrNull())
                }
            )
        }
    }.getOrDefault(emptyList())
}
