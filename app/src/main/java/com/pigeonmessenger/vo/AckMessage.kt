package com.pigeonmessenger.vo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class AckMessage (
        @SerializedName("message_id")
        val messageId:String,
        @SerializedName("status")
        val status:String
):Serializable