package com.pigeonmessenger.vo

import android.annotation.SuppressLint
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
data class QuoteMessageItem(
    @PrimaryKey
    val messageId: String,
    val conversationId : String,
    val senderId: String,
    val userFullName: String?,
    val type: String,
    val message: String?,
    val createdAt: String,
    val status: String,
    val mediaStatus: String?,
    val userAvatarUrl: String?,
    val mediaName: String?,
    val mediaMimeType: String?,
    val mediaSize: Long?,
    val mediaWidth: Int?,
    val mediaHeight: Int?,
    val thumbImage: String?,
    val mediaUrl: String?,
    val mediaDuration: String?
) : Parcelable {
    constructor(messageItem: MessageItem) : this(messageItem.id, messageItem.conversationId,
        messageItem.senderId,null, messageItem.type,
        messageItem.message, messageItem.createdAt, messageItem.status!!, messageItem.mediaStatus,
        null, messageItem.name, messageItem.mediaMimeType, messageItem.mediaSize,
        messageItem.mediaWidth, messageItem.mediaHeight, messageItem.thumbImage, messageItem.mediaUrl,
        messageItem.mediaDuration)
}
