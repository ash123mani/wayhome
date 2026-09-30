package com.wayhome.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wayhome.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun observeThread(threadId: String): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE id = :id")
    suspend fun exists(id: String): Int
}
