package com.pigeonmessenger.vo

import com.google.gson.annotations.SerializedName

data class TransferPlainData(
    @SerializedName("action")
    val action: String,
    @SerializedName("messages")
    val messages: List<String>? = null,
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("message_id")
    val messageId: String? = null
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 7L
    }
}

data class ResendData(
    val userId: String,
    val messageId: String
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 4L
    }
}

enum class PlainDataAction { RESEND_KEY, NO_KEY, RESEND_MESSAGES }
