package com.pigeonmessenger.vo

import androidx.room.Entity
import com.pigeonmessenger.extension.nowInUtc

@Entity
class MessageItem(
        var id: String,
        var conversationId: String,
        var senderId: String,
        var message: String? = null,
        var createdAt: String,
        var type: String,
        var status: String? = null,
        val mediaUrl: String?,
        val mediaMimeType: String?,
        val mediaSize: Long?,
        val mediaDuration: String?,
        val mediaWidth: Int?,
        val mediaHeight: Int?,
        val thumbImage: String?,
        val mediaStatus: String? = null,
        val mediaWaveform: ByteArray? = null,
        val name: String? = null,
        val quoteMessageId: String? = null,
        val quoteContent: String? = null,
        val action: String? = null,
        val participantId: String? = null,
        val hyperlink: String? = null,
        val siteName: String? = null,
        val siteDescription: String? = null,
        val senderDisplayName: String? = null,
        val senderFullName: String? = null,
        val groupName: String? = null,
        val creatorId: String? = null,
        val participantFullName: String? = null,
        val participantDisplayName: String? = null)


fun MessageItem.participantName(): String? {
    return this.participantDisplayName ?: this.participantDisplayName ?: "Unknown"
}

fun MessageItem.senderName(): String? {
    return this.senderDisplayName ?: this.senderFullName ?: "Unknown"
}


fun MessageItem.isMedia(): Boolean = this.type == MessageCategory.SIGNAL_IMAGE.name ||
        this.type == MessageCategory.PLAIN_IMAGE.name ||
        this.type == MessageCategory.SIGNAL_DATA.name ||
        this.type == MessageCategory.PLAIN_DATA.name ||
        this.type == MessageCategory.SIGNAL_VIDEO.name ||
        this.type == MessageCategory.PLAIN_VIDEO.name

fun MessageItem.canNotForward() = this.type == MessageCategory.APP_CARD.name ||
        this.type == MessageCategory.APP_BUTTON_GROUP.name ||
        this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
        (this.mediaStatus != MediaStatus.DONE.name && this.isMedia())
//||
// isCallMessage()

fun create(type:String,createdAt: String?) = MessageItem("","","",null,createdAt ?: nowInUtc(),type,null,null,null,null,
        null,null,null,null)

fun MessageItem.canNotReply() =
        this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
                (this.mediaStatus != MediaStatus.DONE.name && this.isMedia())

fun MessageItem.isCallMessage() =
        type == MessageCategory.WEBRTC_AUDIO_CANCEL.name ||
                type == MessageCategory.WEBRTC_AUDIO_DECLINE.name ||
                type == MessageCategory.WEBRTC_AUDIO_END.name ||
                type == MessageCategory.WEBRTC_AUDIO_BUSY.name ||
                type == MessageCategory.WEBRTC_AUDIO_FAILED.name ||
                type == MessageCategory.WEBRTC_VIDEO_CANCEL.name ||
                type == MessageCategory.WEBRTC_VIDEO_DECLINE.name ||
                type == MessageCategory.WEBRTC_VIDEO_END.name ||
                type == MessageCategory.WEBRTC_VIDEO_BUSY.name ||
                type == MessageCategory.WEBRTC_VIDEO_FAILED.name