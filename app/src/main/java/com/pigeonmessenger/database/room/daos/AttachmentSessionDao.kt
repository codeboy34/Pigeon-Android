package com.pigeonmessenger.database.room.daos

import androidx.room.Dao
import androidx.room.Query
import com.pigeonmessenger.database.room.entities.AttachmentSessionEntity
import com.pigeonmessenger.database.room.entities.BaseDao

@Dao
interface AttachmentSessionDao : BaseDao<AttachmentSessionEntity>{

    @Query("select * from attachmentsession where messageId = :messageId")
    fun getAttachment(messageId:String):AttachmentSessionEntity?

    @Query("delete from attachmentsession where messageId= :messageId")
    fun deleteAttachmentSession(messageId:String)
}