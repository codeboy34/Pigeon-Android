package com.pigeonmessenger.database.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachmentsession")
class AttachmentSessionEntity (
        @PrimaryKey
        @ColumnInfo(name = "messageId")
        var messageId:String,

        @ColumnInfo(name="sessionUri")
        var sessionUri : String)