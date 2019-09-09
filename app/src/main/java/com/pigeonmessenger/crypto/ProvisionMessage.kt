package com.pigeonmessenger.crypto

import com.google.gson.annotations.SerializedName
import com.pigeonmessenger.utils.GsonHelper

class ProvisionMessage(
        @SerializedName("identity_key_public")
        val identityKeyPublic: ByteArray,
        @SerializedName("identity_key_private")
        val identityKeyPrivate: ByteArray,
        @SerializedName("user_id")
        val userId: String,
        @SerializedName("provisioning_code")
        val provisioningCode: String,
        @SerializedName("profile_key")
        val profileKey: ByteArray) {

    fun toByteArray(): ByteArray {
        return GsonHelper.customGson.toJson(this).toByteArray()
    }
}

class ProvisionEnvelope(
        @SerializedName("public_key")
        val publicKey: ByteArray,
        @SerializedName("body")
        val body: ByteArray) {

    fun toByteArray(): ByteArray {
        return GsonHelper.customGson.toJson(this).toByteArray()
    }
}