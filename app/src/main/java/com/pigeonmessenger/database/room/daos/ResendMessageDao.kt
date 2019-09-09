package com.pigeonmessenger.database.room.daos

import androidx.room.Dao
import androidx.room.Query
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.vo.ResendMessage

@Dao
interface ResendMessageDao : BaseDao<ResendMessage> {

    @Query("SELECT * FROM resend_messages WHERE user_id = :userId AND message_id = :messageId")
    fun findResendMessage(userId: String, messageId: String): ResendMessage?
}