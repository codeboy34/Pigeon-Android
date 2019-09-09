package com.pigeonmessenger.api.request

import com.google.gson.annotations.SerializedName

data class AccountUpdateRequest(
    @SerializedName("full_name")
    val fullName: String? = null,
    val bio:String?=null,
    @SerializedName("avatar")
    val avatar: String? = null,
    @SerializedName("thumbnail")
    val thumbnailBase64: String? = null)