package com.pigeonmessenger.events

import com.google.gson.annotations.SerializedName

data class SeenStatusEvent(
        @SerializedName("user_id")
        val userId: String,
        @SerializedName("online")
        val isOnline: Boolean,
        @SerializedName("last_seen")
        val lastSeenTimestamp: String?
)
