package com.pigeonmessenger.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.pigeonmessenger.crypto.vo.PreKey
import com.pigeonmessenger.database.room.entities.BaseDao


@Dao
interface PreKeyDao : BaseDao<PreKey> {

    @Transaction
    @Query("SELECT * FROM prekeys WHERE prekey_id = :preKeyId")
    fun getPreKey(preKeyId: Int): PreKey?

    @Query("DELETE FROM prekeys WHERE prekey_id = :preKeyId")
    fun delete(preKeyId: Int)
}