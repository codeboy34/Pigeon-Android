package com.pigeonmessenger.vo

import com.google.gson.annotations.SerializedName
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.User

data class Account(
        @SerializedName("user_id")
        var userId: String? = null,
        var full_name: String?=null,
        var created_at: String? = null,
        var avatar: String? = null,
        var thumbnail: String? = null,
        var bio: String? = null
)

fun Account.toUser(): User = User(null, userId!!, full_name, bio, thumbnail, "$userId.jpg", relationship = Relationship.ME.name)