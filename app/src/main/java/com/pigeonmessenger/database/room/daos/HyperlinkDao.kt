package com.pigeonmessenger.database.room.daos

import androidx.room.Dao
import androidx.room.Query
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.vo.Hyperlink

@Dao
interface HyperlinkDao : BaseDao<Hyperlink> {
    @Query("SELECT * FROM hyperlinks WHERE hyperlink = :hyperlink")
    fun findHyperlinkByLink(hyperlink: String): Hyperlink?
}