package com.wayhome.domain.model

import kotlinx.serialization.Serializable

/**
 * Transport-independent message model. Every message carries a unique [id]
 * so duplicates from bitchat-style flood rebroadcasts can be dropped.
 * `ttl` is decremented on each forward; 0 means do not forward further.
 */
@Serializable
sealed class Message {
    abstract val id: String
    abstract val senderId: String
    abstract val timestamp: Long
    abstract val ttl: Int

    @Serializable
    data class Advertise(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val ttl: Int = 0,
        val tempName: String,
        val city: String,
        val area: String,
        val lat: Double? = null,
        val lng: Double? = null,
        val lookingForPartners: Boolean = true
    ) : Message()

    @Serializable
    data class Text(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val ttl: Int = 3,
        val conversationId: String,
        val text: String
    ) : Message()

    @Serializable
    data class GroupCreated(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val ttl: Int = 3,
        val groupId: String,
        val groupName: String,
        val creatorId: String,
        val members: List<String> = emptyList()
    ) : Message()

    @Serializable
    data class GroupText(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val ttl: Int = 3,
        val groupId: String,
        val text: String
    ) : Message()

    @Serializable
    data class GroupJoin(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val ttl: Int = 3,
        val groupId: String,
        val memberId: String
    ) : Message()
}
