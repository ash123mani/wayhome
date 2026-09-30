package com.wayhome.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    /** threadId = direct conversationId OR groupId */
    val threadId: String,
    val kind: String, // TEXT, GROUP_TEXT, ADVERTISE, GROUP_CREATED...
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val ttl: Int,
    val status: String = "STORED"
)

@Entity(tableName = "peers", primaryKeys = ["endpointId"])
data class PeerEntity(
    val endpointId: String,
    val tempId: String,
    val city: String,
    val area: String,
    val lat: Double?,
    val lng: Double?,
    val looking: Boolean,
    val lastSeen: Long
)

@Entity(tableName = "groups", primaryKeys = ["groupId"])
data class GroupEntity(
    val groupId: String,
    val name: String,
    val creatorId: String
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "memberId"])
data class GroupMemberEntity(
    val groupId: String,
    val memberId: String
)
