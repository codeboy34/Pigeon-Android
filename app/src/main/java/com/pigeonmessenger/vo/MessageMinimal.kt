package com.pigeonmessenger.vo

import androidx.room.ColumnInfo

class MessageMinimal(var id:String,
                     @ColumnInfo(name="created_at")
                     var createdAt:String)