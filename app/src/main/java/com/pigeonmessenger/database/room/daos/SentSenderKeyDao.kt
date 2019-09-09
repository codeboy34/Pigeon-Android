package com.pigeonmessenger.database.room.daos

import androidx.room.Dao
import androidx.room.Query
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.vo.SentSenderKey

@Dao
interface SentSenderKeyDao : BaseDao<SentSenderKey> {
    @Query("DELETE FROM sent_sender_keys WHERE conversation_id = :conversationId")
    fun delete(conversationId: String)
}