package com.pigeonmessenger.database.room.daos

import androidx.room.Dao
import androidx.room.Query
import com.pigeonmessenger.database.room.entities.TestEntity

@Dao
interface TestDao{
    @Query("select * from test")
    fun getAll():List<TestEntity>
}