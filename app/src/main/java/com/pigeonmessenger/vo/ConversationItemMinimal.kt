package com.pigeonmessenger.vo

import androidx.room.Entity

@Entity
data class ConversationItemMinimal(
    val conversationId: String,
    val groupName: String?,
    val groupIconThumbnail:String?
)