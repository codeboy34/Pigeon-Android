package com.pigeonmessenger.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.pigeonmessenger.crypto.vo.Session
import com.pigeonmessenger.database.room.entities.BaseDao

@Dao
interface SessionDao : BaseDao<Session> {

    @Transaction
    @Query("SELECT * FROM sessions WHERE address = :address AND device = :device")
    fun getSession(address: String, device: Int): Session?

    @Transaction
    @Query("SELECT device from sessions where address = :address AND device != 1")
    fun getSubDevice(address: String): List<Int>?

    @Transaction
    @Query("SELECT * FROM sessions where address = :address")
    fun getSessions(address: String): List<Session>?
}