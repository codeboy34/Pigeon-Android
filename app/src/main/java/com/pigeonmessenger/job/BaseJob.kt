package com.pigeonmessenger.job

import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.pigeonmessenger.api.ContactsService
import com.pigeonmessenger.api.ConversationService
import com.pigeonmessenger.api.SignalKeyService
import com.pigeonmessenger.crypto.SignalProtocol
import com.pigeonmessenger.database.room.daos.*
import com.pigeonmessenger.di.AppComponent
import com.pigeonmessenger.manager.SocketManager
import com.pigeonmessenger.repo.ContactsRepo
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.AttachmentUtil
import javax.inject.Inject

abstract class BaseJob(params: Params,var jobId:String) : Job(params){

    fun inject(appComponent: AppComponent){
        appComponent.inject(this)
    }

    @Inject
    @Transient
    lateinit var jobManager: PigeonJobManager

    @Inject
    @Transient
    lateinit var socketManager:SocketManager

    @Inject
    @Transient
    lateinit var conversationRepo : ConversationRepo

    @Inject
    @Transient
    lateinit var messageDao : MessageDao

    @Inject
    @Transient
    lateinit var contactDao:ContactsDao

    @Inject
    @Transient
    lateinit var attachmentUtil : AttachmentUtil

    @Inject
    @Transient
    lateinit var attachmentSessionDao: AttachmentSessionDao

    @Inject
    @Transient
    lateinit var conversationApi: ConversationService

    @Inject
    @Transient
    lateinit var contactsService: ContactsService

    @Inject
    @Transient
    lateinit var contactsRepo: ContactsRepo

    @Inject
    @Transient
    lateinit var hyperlinkDao: HyperlinkDao

    @Inject
    @Transient
    lateinit var signalProtocol: SignalProtocol

    @Inject
    @Transient
    lateinit var conversationDao:ConversationDao

    @Inject
    @Transient
    lateinit var participantDao: ParticipantDao

    @Inject
    @Transient
    lateinit var signalKeyService : SignalKeyService

    @Inject
    @Transient
    lateinit var sentSenderKeyDao: SentSenderKeyDao

    companion object {
        private const val serialVersionUID = 1L

        const val PRIORITY_UI_HIGH = 20
        const val PRIORITY_SEND_MESSAGE = 18
        const val PRIORITY_SEND_ATTACHMENT_MESSAGE = 17
        const val PRIORITY_RECEIVE_MESSAGE = 15
        const val PRIORITY_BACKGROUND = 10
        const val PRIORITY_DELIVERED_ACK_MESSAGE = 7
        const val PRIORITY_ACK_MESSAGE = 5
        const val PRIORITY_TYPING_MESSAGE = 2

    }

    protected fun removeJob() {
        jobManager.removeJob(jobId)
    }

    override fun onAdded() {

    }

    override fun onCancel(p0: Int, p1: Throwable?) {

    }

    override fun onRun() {

    }

    internal abstract fun cancel()



}