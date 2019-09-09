package com.pigeonmessenger.di

import android.util.Log
import com.google.gson.Gson
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.ContactsService
import com.pigeonmessenger.api.ConversationService
import com.pigeonmessenger.api.SignalKeyService
import com.pigeonmessenger.crypto.SignalProtocol
import com.pigeonmessenger.crypto.db.RatchetSenderKeyDao
import com.pigeonmessenger.database.room.daos.*
import com.pigeonmessenger.database.room.entities.*
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.job.RefreshConversationJob
import com.pigeonmessenger.job.RefreshUsersJob
import com.pigeonmessenger.repo.ContactsRepo
import com.pigeonmessenger.viewmodals.isGroup
import com.pigeonmessenger.vo.FloodMessage
import java.io.IOException
import javax.inject.Inject

open class Injector : Injectable {

    val gson = Gson()

    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var jobManager: PigeonJobManager

    @Inject
    lateinit var contactsDao: ContactsDao

    @Inject
    lateinit var contactsService: ContactsService

    @Inject
    lateinit var contactsRepo: ContactsRepo

    @Inject
    lateinit var conversationDao: ConversationDao

    @Inject
    lateinit var conversationService: ConversationService

    @Inject
    lateinit var participantDao: ParticipantDao

    @Inject
    lateinit var signalProtocol: SignalProtocol

    @Inject
    lateinit var ratchetSenderKeyDao: RatchetSenderKeyDao

    @Inject
    lateinit var signalKeyService: SignalKeyService

    @Inject
    lateinit var resendMessageDao: ResendMessageDao


    protected fun syncConversation(data: FloodMessage) {
        if (data.category == "SIGNAL_KEY" || data.category.startsWith("PLAIN_"))
            return


        var conversation = conversationDao.findConversationById(data.conversationId)
        if (conversation == null) {
            if (!isGroup(data.conversationId)) {
                conversation = createConversation(data.conversationId, ConversationCategory.CONTACT.name, data.senderId,
                        ConversationStatus.SUCCESS.ordinal)
                conversationDao.insert(conversation)
                syncUser(data.senderId)
            } else {
                conversation = createConversation(data.conversationId, category = ConversationCategory.GROUP.name, ownerId = null,
                        status = ConversationStatus.START.ordinal)
                conversationDao.insert(conversation)
                refreshConversation(data.conversationId)
            }
        }

        if (conversation.status == ConversationStatus.START.ordinal &&
                conversation.category == ConversationCategory.GROUP.name)
            jobManager.addJobInBackground(RefreshConversationJob(data.conversationId))

    }


    private fun refreshConversation(conversationId: String) {
        Log.d(TAG, "refreshConversation()--------------------------------: ");
        try {
            val response = conversationService.getConversation(conversationId).execute()

            Log.d(TAG, ":${response?.body()} ");

            if (response != null && response.isSuccessful) {
                Log.d(TAG, "response success:  ");
                response.body()?.let { conversationData ->
                    Log.d(TAG, "${conversationData}  ");

                    val status = if (conversationData.participants.find { Session.getUserId() == it.userId } != null) {
                        ConversationStatus.SUCCESS.ordinal
                    } else {
                        ConversationStatus.QUIT.ordinal
                    }
                    val ownerId: String = conversationData.creatorId
                    Log.d(TAG, "status $status: ");
                    conversationDao.updateConversation(conversationData.conversationId, ownerId, conversationData.category, conversationData.name,
                            conversationData.thumbnail,conversationData.announcement, conversationData.muteUntil, conversationData.createdAt, status)
                }
            } else {
                Log.d(TAG, ":Reponse null fuckkkkk ");
            }
        } catch (e: IOException) {
            Log.d(TAG, ":${e} ");
        }
    }

    private fun syncUser(userId: String) {
        val user = contactsDao.findUser(userId)
        if (user == null) {
            try {
                val response = contactsService.fetchProfile(userId).execute()
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        val contact = contactsDao.findContact(userId)
                        if (contact == null) {
                            contactsDao.insert(User(null, userId, data.full_name, data.bio, data.thumbnail,
                                    null, relationship = Relationship.STRANGE.name))
                        } else {
                            contactsDao.syncUser(userId, data.full_name, data.bio, data.thumbnail)
                        }
                    }
                }
            } catch (e: Exception) {
                jobManager.addJobInBackground(RefreshUsersJob(arrayListOf(userId)))
            }
        }
    }

    companion object {
        val TAG = "Injector"
    }

    init {
        App.get().appComponent.inject(this)
    }
}