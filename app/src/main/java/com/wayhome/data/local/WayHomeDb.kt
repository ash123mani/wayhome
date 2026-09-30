package com.wayhome.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, PeerEntity::class, GroupEntity::class, GroupMemberEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WayHomeDb : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun peerDao(): PeerDao
    abstract fun groupDao(): GroupDao
}
