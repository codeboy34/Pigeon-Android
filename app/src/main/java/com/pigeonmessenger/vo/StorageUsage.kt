package com.pigeonmessenger.vo

import androidx.room.Entity

@Entity
data class StorageUsage(
    val conversationId: String,
    val type: String,
    val mediaSize: Long,
    val count: Long
)