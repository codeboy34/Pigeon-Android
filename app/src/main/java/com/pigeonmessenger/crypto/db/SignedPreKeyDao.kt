package com.pigeonmessenger.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.pigeonmessenger.crypto.vo.SignedPreKey
import com.pigeonmessenger.database.room.entities.BaseDao


@Dao
interface SignedPreKeyDao : BaseDao<SignedPreKey> {

    @Transaction
    @Query("SELECT * FROM signed_prekeys WHERE prekey_id = :signedPreKeyId")
    fun getSignedPreKey(signedPreKeyId: Int): SignedPreKey?

    @Transaction
    @Query("SELECT * FROM signed_prekeys")
    fun getSignedPreKeyList(): List<SignedPreKey>

    @Query("DELETE FROM signed_prekeys WHERE prekey_id = :signedPreKeyId")
    fun delete(signedPreKeyId: Int)
}