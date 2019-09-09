package com.pigeonmessenger.vo

import com.pigeonmessenger.api.request.SignalKeyRequest
import java.io.Serializable
import java.util.*

data class BlazeMessageParam(
        val message_id: String,
        val category: String,
        val data: String?,
        val keys: SignalKeyRequest? = null,
        val messages: List<Any>? = null,
        val quote_message_id: String? = null

) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6L
    }
}

fun createChatMessageBlazeParamas(
        message_id: String,
        category: String,
        data: String?,
        quote_message_id: String?) =
        BlazeMessageParam(
                message_id, category, data, quote_message_id = quote_message_id)

fun createSignalKeyParam(chiperText: String) = BlazeMessageParam(UUID.randomUUID().toString(), data = chiperText, category = MessageCategory.SIGNAL_KEY.name)

fun createSignalKeyMessageParam(messages: ArrayList<BlazeSignalKeyMessage>) =
        BlazeMessageParam(UUID.randomUUID().toString(), MessageCategory.SIGNAL_KEY.name, null, null, messages)

fun createPlainJsonParam(encoded: String) = BlazeMessageParam(UUID.randomUUID().toString(), MessageCategory.PLAIN_JSON.name, encoded)

fun BlazeMessageParam.isChatMessage()=
    this.category == MessageCategory.SIGNAL_TEXT.name ||
            this.category == MessageCategory.SIGNAL_IMAGE.name ||
            this.category == MessageCategory.PLAIN_IMAGE.name ||
            this.category == MessageCategory.SIGNAL_DATA.name ||
            this.category == MessageCategory.PLAIN_DATA.name ||
            this.category == MessageCategory.SIGNAL_VIDEO.name ||
            this.category == MessageCategory.PLAIN_VIDEO.name ||
            this.category == MessageCategory.SIGNAL_CONTACT.name