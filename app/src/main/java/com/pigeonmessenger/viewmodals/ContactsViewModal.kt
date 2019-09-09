package com.pigeonmessenger.viewmodals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.ContactsService
import com.pigeonmessenger.database.room.daos.ContactsDao
import com.pigeonmessenger.database.room.daos.ConversationDao
import com.pigeonmessenger.job.AvatarFetchJob
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ContactsViewModal(app: Application) : AndroidViewModel(app) {

    @Inject
    lateinit var contactsDao: ContactsDao

    @Inject
    lateinit var contactsService: ContactsService

    @Inject
    lateinit var jobManager: PigeonJobManager

    @Inject
    lateinit var conversationRepo: ConversationRepo

    @Inject
    lateinit var conversationDao: ConversationDao

    fun getLiveUsers() = contactsDao.getLiveUsers()

    init {
        App.get().appComponent.inject(this)
    }

    fun getContact(contactId: String) = contactsDao.getContact(contactId)

    fun fetchAvatar(userId: String) {
        jobManager.addJobInBackground(AvatarFetchJob(userId))
    }

    fun clearConversationById(conversationId: String) {
        conversationRepo.clearConversation(conversationId)
    }

    fun block(userId: String) = contactsService.block(userId).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())

    fun unblock(userId: String) = contactsService.block(userId).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())

    fun updateBlockRelationship(userId: String, relationship: String) {
        GlobalScope.launch {
            contactsDao.updateRelationship(userId, relationship)
        }

    }

    fun mute(userId: String, muteUntil: String?) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            contactsDao.updateDuration(userId, muteUntil)
        }
    }

    fun blockList() = contactsDao.blockList()

}