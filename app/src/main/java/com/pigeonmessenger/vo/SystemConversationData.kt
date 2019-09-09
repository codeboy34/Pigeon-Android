package com.pigeonmessenger.vo

import com.google.gson.annotations.SerializedName

data class SystemConversationData(
    @SerializedName("action")
    val action: String,
    @SerializedName("participant_id")
    val participantId: String?,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("role")
    val role: String?,
    val type:String?
)

enum class SystemConversationAction { JOIN, EXIT, ADD, REMOVE, CREATE, UPDATE_ICON,UPDATE_NAME, ROLE }
