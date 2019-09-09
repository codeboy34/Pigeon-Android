package com.pigeonmessenger.database.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.vo.ConversationBuilder
import org.threeten.bp.Instant
import java.util.*

@Entity(tableName = "conversations")
data class Conversation(

        @PrimaryKey
        @ColumnInfo(name = "conversation_id")
        val conversationId: String,

        @ColumnInfo(name = "owner_id")
        val ownerId: String? = null,

        @ColumnInfo(name = "category")
        val category: String?,

        @ColumnInfo(name = "name")
        val name: String? = null,

        @ColumnInfo(name = "icon_thumbnail")
        val groupIconThumbnail: String? = null,

        @ColumnInfo(name = "icon_url")
        val iconUrl: String? = null,

        @ColumnInfo(name = "announcement")
        val announcement: String? = null,
        @ColumnInfo(name = "created_at")
        val createdAt: String,
        @ColumnInfo(name = "pin_time")
        val pinTime: String?,
        @ColumnInfo(name = "last_message_id")
        val lastMessageId: String?,
        @ColumnInfo(name = "last_read_message_id")
        val lastReadMessageId: String?,
        @ColumnInfo(name = "unseen_message_count")
        val unseenMessageCount: Int?,

        @ColumnInfo(name = "status")
        val status: Int,

        @ColumnInfo(name = "draft")
        val draft: String? = null,

        @ColumnInfo(name = "mute_until")
        val muteUntil: String? = null,

        @ColumnInfo(name = "icon_update")
        val iconLastUpdate: String? = null
)


fun Conversation.isMute(): Boolean {
    if (muteUntil != null) {
        return Instant.now().isBefore(Instant.parse(muteUntil))
    }
    return false
}

enum class ConversationCategory { CONTACT, GROUP }
enum class ConversationStatus { START, FAILURE, SUCCESS, QUIT }

fun Conversation.isGroup(): Boolean {
    if (category == null) return false
    return category == ConversationCategory.GROUP.name
}

fun createConversation(conversationId: String, category: String?, ownerId: String? = null, status: Int) =
        ConversationBuilder(conversationId, nowInUtc(), status)
                .setCategory(category)
                .setAnnouncement("")
                .setOwnerId(ownerId)
                .setUnseenMessageCount(0)
                .build()

fun generateConversationId(senderId: String, recipientId: String): String {
    val mix = minOf(senderId, recipientId) + maxOf(senderId, recipientId)
    return UUID.nameUUIDFromBytes(mix.toByteArray()).toString()
}
