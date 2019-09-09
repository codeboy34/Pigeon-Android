package com.pigeonmessenger.api.request

import com.google.gson.annotations.SerializedName

class SettingRequest(
        val profile: Int? = null,
        @SerializedName("last_seen")
        var lastSeen: Int? = null
)