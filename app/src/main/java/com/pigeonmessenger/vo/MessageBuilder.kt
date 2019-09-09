package com.pigeonmessenger.vo

import kotlin.math.abs

class MessageBuilder(
    val id: String,
    val conversationId:String,
    val senderId: String,
    val category: String,
    val status: String,
    val createdAt: String) {

    private var message: String?=null
    private var mediaUrl: String? = null
    private var mediaMimeType: String? = null
    private var mediaSize: Long? = null
    private var mediaDuration: String? = null
    private var mediaWidth: Int? = null
    private var mediaHeight: Int? = null
    private var thumbImage: String? = null
    private var mediaStatus: String? = null
    private var mediaKey : String?=null
    private var name : String? =null
    private var waveform:ByteArray?=null
    private var quoteMessageId: String? = null
    private var action: String?=null
    private var participantId: String?=null
    private var quoteContent: String? = null
    private var hyperlink:String?=null

    fun setMediaUrl(mediaUrl: String?): MessageBuilder {
        this.mediaUrl = mediaUrl
        return this
    }

    fun setMediaMimeType(mediaMimeType: String): MessageBuilder {
        this.mediaMimeType = mediaMimeType
        return this
    }

    fun setMediaSize(mediaSize: Long): MessageBuilder {
        this.mediaSize = mediaSize
        return this
    }

    fun setMediaDuration(mediaDuration: String): MessageBuilder {
        this.mediaDuration = mediaDuration
        return this
    }

    fun setMediaWidth(mediaWidth: Int?): MessageBuilder {
        this.mediaWidth = mediaWidth?.let { abs(it) }
        return this
    }

    fun setMediaHeight(mediaHeight: Int?): MessageBuilder {
        this.mediaHeight = mediaHeight?.let { abs(it) }
        return this
    }

    fun setThumbImage(thumbImage: String?): MessageBuilder {
        this.thumbImage = thumbImage
        return this
    }


    fun setMediaStatus(mediaStatus: String): MessageBuilder {
        this.mediaStatus = mediaStatus
        return this
    }

    fun setMessage(message:String?):MessageBuilder{
        this.message=message
        return this
    }

    fun setMediaKey(mediaKey: String):MessageBuilder{
     this.mediaKey=mediaKey
        return this
    }

    fun setName(name: String):MessageBuilder{
        this.name = name
        return this
    }

    fun setWaveform(waveform: ByteArray):MessageBuilder{
        this.waveform= waveform
        return this
    }

    fun quoteMessageId(quoteMessageId : String?):  MessageBuilder{
        this.quoteMessageId = quoteMessageId
        return this
    }

    fun quoteContent(quoteMessage:String?):MessageBuilder{
        this.quoteContent = quoteMessage
        return this
    }

    fun setAction(action:String?) : MessageBuilder{
        this.action = action
        return this
    }

    fun setParticipantId(participantId:String?):MessageBuilder{
        this.participantId= participantId
        return this
    }

    fun hyperLink(hyperlink:String):MessageBuilder{
        this.hyperlink = hyperlink
        return this
    }

    fun build(): MessageEntity = MessageEntity(id, conversationId, senderId,message,createdAt,category, status
    ,mediaUrl,mediaMimeType,mediaSize,mediaDuration,mediaWidth,mediaHeight,thumbImage,mediaStatus,waveform,mediaKey,
            name,quoteMessageId,quoteContent,action,participantId,hyperlink)

}
