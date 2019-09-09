package com.pigeonmessenger.viewmodals

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.vo.ConversationStorageUsage
import javax.inject.Inject

class SettingStorageViewModel @Inject
internal constructor(

) : ViewModel() {

    init {
        App.get().appComponent.inject(this)
    }
    @Inject
    lateinit var conversationRepository: ConversationRepo

    fun getConversationStorageUsage(): LiveData<List<ConversationStorageUsage>?> = conversationRepository.getConversationStorageUsage()

    fun clear(conversationId: String, category: String) {

    }
}