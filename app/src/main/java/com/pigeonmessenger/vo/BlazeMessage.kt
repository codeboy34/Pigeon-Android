package com.pigeonmessenger.vo

import com.google.gson.annotations.SerializedName
import com.pigeonmessenger.Session
import com.pigeonmessenger.extension.nowInUtc
import java.io.Serializable
import java.util.*

class BlazeMessage(
        @SerializedName("id")
        val id: String,
        @SerializedName("sender_id")
        val senderId: String,
        @SerializedName("conversation_id")
        val conversationId: String,
        @SerializedName("recipient_id")
        val recipientId: String?=null,
        @SerializedName("message_type")
        val category: String,
        @SerializedName("message_params")
        val messageParam: BlazeMessageParam? = null,
        @SerializedName("ack_params")
        val ack: AckMessage? = null,
        @SerializedName("acks_params")
        val acks: List<AckMessage>? = null,
        @SerializedName("system_data")
        var systemData: SystemConversationData? = null,
        @SerializedName("created_at")
        var createdAt: String
) : Serializable


fun createChatBlazeMessage(conversationId: String,
                           recipientId: String?,
                           blazeMessageParam: BlazeMessageParam,
                           createdAt: String) =
        BlazeMessage(UUID.randomUUID().toString(), Session.getUserId(), conversationId,recipientId,
                BlazeMessageCategory.CHAT_MESSAGE.name, blazeMessageParam, createdAt = createdAt)

fun createAckMessage(id: String, conversationId: String,
                     senderId: String,
                     recipientId: String,
                     ackMessage: AckMessage?,
                     createdAt: String) =
        BlazeMessage(id, senderId, conversationId, recipientId,BlazeMessageCategory.ACK.name, ack = ackMessage, createdAt = createdAt)

fun createAcksMessage(id: String, senderId: String,
                      conversationId: String,
                      recipientId: String?,
                      ackMessage: List<AckMessage>?,
                      createdAt: String) =
        BlazeMessage(id, senderId, conversationId, recipientId,BlazeMessageCategory.ACKS.name, acks = ackMessage, createdAt = createdAt)


fun createEventMessage() = BlazeMessage(UUID.randomUUID().toString(), Session.getUserId(), "", createdAt = nowInUtc(), category = BlazeMessageCategory.EVENT.name)

fun createCallBlazeMessage(conversationId: String, recipientId: String?,params: BlazeMessageParam) = BlazeMessage(UUID.randomUUID().toString(), Session.getUserId(), conversationId,recipientId,
        BlazeMessageCategory.CALL.name, params, createdAt = nowInUtc())

fun createSignalKeyMessage(conversationId: String,senderId: String,param: BlazeMessageParam) =
        BlazeMessage(UUID.randomUUID().toString(),senderId,conversationId,null,
                BlazeMessageCategory.CREATE_SIGNAL_KEY_MESSAGES.name, param,createdAt = nowInUtc())

fun createParamBlazeMessage(conversationId: String,senderId: String, recipientId: String?,blazeMessageParam: BlazeMessageParam)=
        BlazeMessage(UUID.randomUUID().toString(),senderId,conversationId,recipientId, BlazeMessageCategory.CHAT_MESSAGE.name,blazeMessageParam,createdAt = nowInUtc())

fun createTypingBlazeMessage(conversationId: String,senderId: String,recipientId: String?)=
        BlazeMessage(UUID.randomUUID().toString(),senderId,conversationId,recipientId, BlazeMessageCategory.TYPING.name,createdAt = nowInUtc())


enum class BlazeMessageCategory {
    CHAT_MESSAGE, ACK, ACKS, SYSTEM, EVENT, CALL,CREATE_SIGNAL_KEY_MESSAGES,TYPING
}