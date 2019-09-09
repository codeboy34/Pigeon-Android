package com.pigeonmessenger.api.response

import com.google.gson.annotations.SerializedName
import com.pigeonmessenger.api.request.ParticipantRequest

data class ConversationResponse(
        @SerializedName("conversation_id")
        val conversationId: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("category")
        val category: String,
        @SerializedName("creator_id")
        val creatorId: String,
        @SerializedName("thumbnail")
        val thumbnail:String?=null,
        @SerializedName("announcement")
        val announcement: String,
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("participants")
        val participants: List<ParticipantRequest>,
        @SerializedName("mute_until")
        val muteUntil: String
)