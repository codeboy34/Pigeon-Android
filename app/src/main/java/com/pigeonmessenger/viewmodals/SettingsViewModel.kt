package com.pigeonmessenger.viewmodals

import androidx.lifecycle.ViewModel
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.SettingsService
import com.pigeonmessenger.api.request.SettingRequest
import com.pigeonmessenger.database.room.daos.ConversationDao
import com.pigeonmessenger.database.room.daos.MessageDao
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel : ViewModel() {

    @Inject
    lateinit var settingsService: SettingsService

    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var conversationDao:ConversationDao

    init {
        App.get().appComponent.inject(this)
    }

    fun updateSetting(settingRequest: SettingRequest) =
            settingsService.updateSetting(settingRequest).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())


    fun clearAllChat() {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            messageDao.clearAllConversation()
        }
    }

    fun deleteAllConversation(){
        GlobalScope.launch(SINGLE_DB_THREAD) {
            conversationDao.deleteAll()
        }
    }
}