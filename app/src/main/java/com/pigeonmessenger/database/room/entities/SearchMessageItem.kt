package com.pigeonmessenger.database.room.entities

import androidx.room.Entity
import com.pigeonmessenger.viewmodals.isGroup

@Entity
data class SearchMessageItem(
        val messageId: String,
        val conversationId: String,
        val userId: String?,
        val type: String,
        val mediaName: String? = null,
        val message: String? = null,
        val createdAt: String,
        val fullName: String? = null,
        val displayName: String? = null,
        val userAvatarUrl: String? = null,
        val avatarThumbnail: String? = null,
        val conversationName: String? = null,
        val conversationCategory: String? = null,
        val groupIconThumbnail: String? = null
) {

    fun name(): String {
        if (isGroup(conversationId)) return conversationName?:"unknown";
        return displayName ?: fullName?: "unknown"
    }
}
