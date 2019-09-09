package com.pigeonmessenger.viewmodals

import androidx.room.Entity
import com.pigeonmessenger.vo.MessageCategory
import org.threeten.bp.Instant

@Entity
data class ConversationItem(
        var conversationId: String,
        val ownerId: String?,
        var groupIcon: String? = null,
        var groupThumbnail: String? = null,
        var category: String? = null,
        var groupStatus: Int? = null,
        var action: String? = null,
        var participantId: String? = null,
        var participantDisplayName: String?,
        var participantFullName: String?,
        var groupName: String? = null,
        var unseenCount: Int? = null,
        var displayName: String? = null,
        var fullName: String? = null,
        var senderId: String? = null,
        var senderDisplayName: String? = null,
        var senderFullName: String? = null,
        var relationship: String? = null,
        var avatarThumbnail: String? = null,
        var message: String? = null,
        var createdAt: String? = null,
        var messageStatus: String? = null,
        var messageType: String? = null,
        var pinTime: String? = null,
        val groupMuteUntil :String?=null,
        var userMuteUntil:String?=null
) {
    fun isGroup(): Boolean {
        return conversationId.startsWith("group")
    }

    fun name(): String {
        if (isGroup()) return groupName ?: conversationId

        if (displayName != null)
            return displayName!!
        else if (fullName != null)
            return fullName!!
        else return ownerId!!
    }


    fun participantName(): String {
        return participantDisplayName ?: participantFullName ?: participantId!!
    }

    fun senderName(): String {
        return senderDisplayName ?: senderFullName ?: senderId!!
    }


    fun isMute(): Boolean {
        if (!isGroup() && userMuteUntil != null) {
            return Instant.now().isBefore(Instant.parse(userMuteUntil))
        }
        if (isGroup() && groupMuteUntil != null) {
            return Instant.now().isBefore(Instant.parse(groupMuteUntil))
        }
        return false
    }

}

fun isGroup(conversationId: String): Boolean {
    return conversationId.startsWith("group")
}

fun ConversationItem.isCallMessage() =
        messageType == MessageCategory.WEBRTC_AUDIO_CANCEL.name ||
                messageType == MessageCategory.WEBRTC_AUDIO_DECLINE.name ||
                messageType == MessageCategory.WEBRTC_AUDIO_END.name ||
                messageType == MessageCategory.WEBRTC_AUDIO_BUSY.name ||
                messageType == MessageCategory.WEBRTC_AUDIO_FAILED.name ||
                messageType == MessageCategory.WEBRTC_VIDEO_CANCEL.name ||
                messageType == MessageCategory.WEBRTC_VIDEO_DECLINE.name ||
                messageType == MessageCategory.WEBRTC_VIDEO_END.name ||
                messageType == MessageCategory.WEBRTC_VIDEO_BUSY.name ||
                messageType == MessageCategory.WEBRTC_VIDEO_FAILED.name