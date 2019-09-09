package com.pigeonmessenger.vo

import androidx.room.Entity

@Entity
class MediaMinimal(
        var  messageId :String,
        var mediaUrl :String?,
        var mediaThumbnail:String?,
        var mediaDuration:String?,
        val createdAt:String?
)