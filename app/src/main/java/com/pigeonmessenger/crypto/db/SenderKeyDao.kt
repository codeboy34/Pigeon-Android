package com.pigeonmessenger.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.pigeonmessenger.crypto.vo.SenderKey
import com.pigeonmessenger.database.room.entities.BaseDao


@Dao
interface SenderKeyDao : BaseDao<SenderKey> {

    @Transaction
    @Query("SELECT * FROM sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    fun getSenderKey(groupId: String, senderId: String): SenderKey?

    @Query("DELETE FROM sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    fun delete(groupId: String, senderId: String)
}