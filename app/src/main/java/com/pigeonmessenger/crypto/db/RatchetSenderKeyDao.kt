package com.pigeonmessenger.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.pigeonmessenger.crypto.vo.RatchetSenderKey
import com.pigeonmessenger.database.room.entities.BaseDao

@Dao
interface RatchetSenderKeyDao : BaseDao<RatchetSenderKey> {

    @Transaction
    @Query("SELECT * FROM ratchet_sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    fun getRatchetSenderKey(groupId: String, senderId: String): RatchetSenderKey?

    @Query("DELETE FROM ratchet_sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    fun delete(groupId: String, senderId: String)
}