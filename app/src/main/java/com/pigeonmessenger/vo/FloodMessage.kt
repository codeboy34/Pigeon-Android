package com.pigeonmessenger.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "flood_messages")
data class FloodMessage(
    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name="conversationId")
    val conversationId :String,
    @ColumnInfo(name="sender_id")
    val senderId:String,

    @SerializedName("data")
    @ColumnInfo(name = "data")
    val data: String,

    @ColumnInfo(name ="blaze_category")
    val category: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String
) : Parcelable
