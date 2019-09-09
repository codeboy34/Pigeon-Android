package com.pigeonmessenger.database.room.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.vo.FloodMessage

@Dao
interface FloodMessageDao : BaseDao<FloodMessage> {

    @Query("SELECT * FROM flood_messages ORDER BY created_at ASC limit 10")
    fun findFloodMessagesSync(): List<FloodMessage>?

    @Query("select count(1) from flood_messages")
    fun getFloodMessageCount(): LiveData<Int>
}