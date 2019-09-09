package com.pigeonmessenger.api.response

import com.google.gson.annotations.SerializedName

data class ContactResponse(
        @SerializedName("user_id")
    var phone_number : String,
    var full_name :String,
    var thumbnail:String?=null,
    var avatar:String?=null,
    var bio : String?=null
    )