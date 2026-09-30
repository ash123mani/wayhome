package com.wayhome.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(peer: PeerEntity)

    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun observePeers(): Flow<List<PeerEntity>>

    @Query("DELETE FROM peers WHERE endpointId = :endpointId")
    suspend fun remove(endpointId: String)
}

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(member: GroupMemberEntity)

    @Query("SELECT * FROM groups")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT memberId FROM group_members WHERE groupId = :groupId")
    suspend fun membersOf(groupId: String): List<String>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("DELETE FROM groups WHERE groupId = :groupId")
    suspend fun removeGroup(groupId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND memberId = :memberId")
    suspend fun removeMember(groupId: String, memberId: String)
}
