package com.pigeonmessenger.database.room.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.Instant

@Parcelize
@Entity(tableName = "Users")
data class User(

        @ColumnInfo(name = "display_name")
        var displayName: String? = null,

        @PrimaryKey
        @ColumnInfo(name = "user_id")
        var userId: String,

        @ColumnInfo(name = "full_name")
        var full_name: String? = null,

        @ColumnInfo(name = "bio")
        var bio: String? = null,

        @ColumnInfo(name = "thumbnail")
        var thumbnail: String? = null,

        @ColumnInfo(name = "last_updated")
        var lastUpdated: String? = null,

        @ColumnInfo(name = "relationship")
        var relationship: String? = null,

        @ColumnInfo(name = "mute_until")
        var muteUntil: String? = null

) : Parcelable {

    fun getName(): String {
        if (displayName != null)
            return displayName!!
        else if (full_name != null)
            return full_name!!
        else return  userId
    }

    override fun hashCode(): Int {
        return userId.toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (userId != other.userId) return false

        return true
    }


    fun isMute(): Boolean {
        if (muteUntil != null) {
            return Instant.now().isBefore(Instant.parse(muteUntil))
        }
        return false
    }
}


enum class Relationship {
    UNREGISTERED_CONTACT, REGISTERED_CONTACT, STRANGE, BLOCKING, ME
}