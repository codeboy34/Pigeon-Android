package com.pigeonmessenger.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "messages")
data class MessageEntity(
        @SerializedName("id")
        @PrimaryKey
        @ColumnInfo(name = "id")
        var id: String,

        @SerializedName("conversation_id")
        @ColumnInfo(name = "conversation_id")
        var conversationId: String,

        @SerializedName("sender_id")
        @ColumnInfo(name = "sender_id")
        var senderId: String,

        @SerializedName("message")
        @ColumnInfo(name = "message")
        var message: String? = null,

        @SerializedName("created_at")
        @ColumnInfo(name = "created_at")
        var createdAt: String,

        @SerializedName("type")
        @ColumnInfo(name = "type")
        var type: String,

        @SerializedName("status")
        @ColumnInfo(name = "status")
        var status: String? = null,

        @SerializedName("media_url")
        @ColumnInfo(name = "media_url")
        val mediaUrl: String?,

        @SerializedName("media_mime_type")
        @ColumnInfo(name = "media_mime_type")
        val mediaMimeType: String?,

        @SerializedName("media_size")
        @ColumnInfo(name = "media_size")
        val mediaSize: Long?,

        @SerializedName("media_duration")
        @ColumnInfo(name = "media_duration")
        val mediaDuration: String?,

        @SerializedName("media_width")
        @ColumnInfo(name = "media_width")
        val mediaWidth: Int?,

        @SerializedName("media_height")
        @ColumnInfo(name = "media_height")
        val mediaHeight: Int?,

        @SerializedName("thumb_image")
        @ColumnInfo(name = "thumb_image")
        val thumbImage: String?,

        @ColumnInfo(name = "media_status")
        val mediaStatus: String? = null,

        @ColumnInfo(name = "media_waveform", typeAffinity = ColumnInfo.BLOB)
        val mediaWaveform: ByteArray? = null,

        @SerializedName("media_key")
        @ColumnInfo(name = "media_key")
        val mediaKey: String? = null,

        @SerializedName("name")
        @ColumnInfo(name = "name")
        val name: String? = null,

        @SerializedName("quote_message_id")
        @ColumnInfo(name = "quote_message_id")
        val quoteMessageId: String? = null,

        @SerializedName("quote_content")
        @ColumnInfo(name = "quote_content")
        val quoteContent: String? = null,


        @SerializedName("action")
        @ColumnInfo(name = "action")
        val action: String? = null,

        @SerializedName("participant_id")
        @ColumnInfo(name = "participant_id")
        val participantId: String? = null,

        @SerializedName("hyperlink")
        @ColumnInfo(name = "hyperlink")
        val hyperlink: String? = null
) : Serializable


enum class MessageCategory {
    SIGNAL_KEY,
    SIGNAL_TEXT,
    SIGNAL_IMAGE,
    SIGNAL_VIDEO,
    SIGNAL_STICKER,
    SIGNAL_DATA,
    SIGNAL_CONTACT,
    SIGNAL_AUDIO,
    PLAIN_TEXT,
    PLAIN_IMAGE,
    PLAIN_VIDEO,
    PLAIN_DATA,
    PLAIN_STICKER,
    PLAIN_CONTACT,
    PLAIN_AUDIO,
    PLAIN_JSON,
    STRANGER,
    SECRET,
    TYPING,
    SYSTEM_CONVERSATION,
    APP_BUTTON_GROUP,
    APP_CARD,
    WEBRTC_AUDIO_OFFER,
    WEBRTC_AUDIO_ANSWER,
    WEBRTC_VIDEO_OFFER,
    WEBRTC_ICE_CANDIDATE,
    WEBRTC_CANCEL,
    WEBRTC_DECLINE,
    WEBRTC_END,
    WEBRTC_BUSY,
    WEBRTC_FAILED,

    WEBRTC_VIDEO_CANCEL,
    WEBRTC_VIDEO_DECLINE,
    WEBRTC_VIDEO_END,
    WEBRTC_VIDEO_BUSY,
    WEBRTC_VIDEO_FAILED,

    WEBRTC_AUDIO_CANCEL,
    WEBRTC_AUDIO_DECLINE,
    WEBRTC_AUDIO_END,
    WEBRTC_AUDIO_BUSY,
    WEBRTC_AUDIO_FAILED,

}

enum class MediaStatus { PENDING, DONE, CANCELED, EXPIRED }

enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

fun createMessage(id: String,
                  conversationId: String,
                  senderId: String,
                  message: String?,
                  createdAt: String,
                  category: String = MessageCategory.PLAIN_TEXT.name,
                  action: String? = null,
                  participantId: String? = null,
                  status: MessageStatus = MessageStatus.SENDING): MessageEntity {

    return MessageBuilder(id, conversationId, senderId, category,
            status.name, createdAt)
            .setMessage(message)
            .setAction(action)
            .setParticipantId(participantId)
            .build()

}


fun createCallMessage(
        messageId: String,
        conversationId: String,
        senderId: String,
        category: String,
        content: String?,
        createdAt: String,
        status: MessageStatus,
        quoteMessageId: String? = null,
        mediaDuration: String? = null
): MessageEntity {
    val builder = MessageBuilder(messageId, conversationId, senderId, category, status.name, createdAt)
            .setMessage(content)
            .quoteMessageId(quoteMessageId)
    if (mediaDuration != null) {
        builder.setMediaDuration(mediaDuration)
    }
    return builder.build()
}

fun createMediaMessage(
        messageId: String,
        conversationId: String,
        senderId: String,
        category: String,
        createdAt: String,
        content: String?,
        mediaUrl: String?,
        mediaMimeType: String,
        mediaSize: Long,
        mediaWidth: Int?,
        mediaHeight: Int?,
        thumbImage: String?,
        mediaStatus: MediaStatus,
        status: MessageStatus,
        mediaKey: String
) = MessageBuilder(messageId, conversationId, senderId, category, status.name, createdAt)
        .setMediaUrl(mediaUrl)
        .setMediaMimeType(mediaMimeType)
        .setMediaSize(mediaSize)
        .setMediaWidth(mediaWidth)
        .setMediaHeight(mediaHeight)
        .setThumbImage(thumbImage)
        .setMediaStatus(mediaStatus.name)
        .setMediaKey(mediaKey)
        .build()


fun createVideoMessage(
        messageId: String,
        conversationId: String,
        senderId: String,
        category: String,
        content: String?,
        mediaKey: String,
        mediaUrl: String?,
        duration: Long?,
        mediaWidth: Int? = null,
        mediaHeight: Int? = null,
        thumbImage: String? = null,
        mediaMimeType: String,
        mediaSize: Long,
        createdAt: String,
        mediaStatus: MediaStatus,
        status: MessageStatus
) = MessageBuilder(messageId, conversationId, senderId, category, status.name, createdAt)
        .setMediaKey(mediaKey)
        .setMediaUrl(mediaUrl)
        .setMediaDuration(duration.toString())
        .setMediaWidth(mediaWidth)
        .setMediaHeight(mediaHeight)
        .setThumbImage(thumbImage)
        .setMediaMimeType(mediaMimeType)
        .setMediaSize(mediaSize)
        .setMediaStatus(mediaStatus.name)
        .build()


fun createAudioMessage(
        messageId: String,
        conversationId: String,
        senderId: String,
        category: String,
        mediaSize: Long,
        mediaUrl: String?,
        mediaKey: String,
        mediaDuration: String,
        createdAt: String,
        mediaWaveform: ByteArray?,
        mediaStatus: MediaStatus,
        status: MessageStatus
) = MessageBuilder(messageId, conversationId, senderId, category, status.name, createdAt)
        .setMediaUrl(mediaUrl)
        .setWaveform(mediaWaveform!!)
        .setMediaSize(mediaSize)
        .setMediaKey(mediaKey)
        .setMediaDuration(mediaDuration)
        .setMediaMimeType("audio/ogg")
        .setMediaStatus(mediaStatus.name)
        .build()


fun createAttachmentMessage(
        messageId: String,
        conversationId: String,
        senderId: String,
        category: String,
        content: String?,
        name: String?,
        mediaKey: String,
        mediaUrl: String?,
        mediaMimeType: String,
        mediaSize: Long,
        createdAt: String,
        mediaStatus: MediaStatus,
        status: MessageStatus
) = MessageBuilder(messageId, conversationId, senderId, category, status.name, createdAt)
        .setName(name!!)
        .setMediaUrl(mediaUrl)
        .setMediaMimeType(mediaMimeType)
        .setMediaSize(mediaSize)
        .setMediaKey(mediaKey)
        .setMediaStatus(mediaStatus.name)
        .build()


fun createReplyMessage(
        messageId: String,
        conversationId: String,
        senderId: String,
        category: String,
        content: String,
        createdAt: String,
        status: MessageStatus,
        quoteMessageId: String?,
        quoteContent: String? = null,
        action: String? = null,
        participantId: String? = null,
        snapshotId: String? = null
) = MessageBuilder(messageId, conversationId, senderId, category, status.name, createdAt)
        .setMessage(content)
        .quoteMessageId(quoteMessageId)
        .quoteContent(quoteContent)
        .build()

fun MessageEntity.isMedia(): Boolean = this.type == MessageCategory.SIGNAL_IMAGE.name ||
        this.type == MessageCategory.PLAIN_IMAGE.name ||
        this.type == MessageCategory.SIGNAL_DATA.name ||
        this.type == MessageCategory.PLAIN_DATA.name ||
        this.type == MessageCategory.SIGNAL_VIDEO.name ||
        this.type == MessageCategory.PLAIN_VIDEO.name

fun MessageEntity.canNotForward() = this.type == MessageCategory.APP_CARD.name ||
        this.type == MessageCategory.APP_BUTTON_GROUP.name ||
        this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
        (this.mediaStatus != MediaStatus.DONE.name && this.isMedia())

fun MessageEntity.isCall() = this.type.startsWith("WEBRTC")


fun MessageEntity.canNotReply() =
        this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
                (this.mediaStatus != MediaStatus.DONE.name && this.isMedia())


fun MessageEntity.isPlain(): Boolean {
    return type.startsWith("PLAIN_")
}