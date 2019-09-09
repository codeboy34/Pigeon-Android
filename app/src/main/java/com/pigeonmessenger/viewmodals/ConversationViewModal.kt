package com.pigeonmessenger.viewmodals

import androidx.lifecycle.ViewModel
import com.pigeonmessenger.database.room.daos.ConversationDao
import javax.inject.Inject

class ConversationViewModal :ViewModel(){
    @Inject
    lateinit var conversationDao: ConversationDao

    fun conversationList() = conversationDao.conversationList()


}