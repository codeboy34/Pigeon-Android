package com.pigeonmessenger.services

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import com.pigeonmessenger.Session
import com.pigeonmessenger.database.room.daos.MessageDao
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.job.NotificationJob.Companion.KEY_REPLY
import com.pigeonmessenger.job.NotificationJob.Companion.RECIPIENT_ID
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.job.SendMessageJob
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.vo.createMessage
import dagger.android.AndroidInjection
import java.util.UUID
import javax.inject.Inject

class SendService : IntentService("SendService") {

    @Inject
    lateinit var jobManager: PigeonJobManager
    @Inject
    lateinit var messageDao: MessageDao

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onHandleIntent(intent: Intent) {
        val bundle = RemoteInput.getResultsFromIntent(intent)
        if (bundle != null) {
            val content = bundle.getCharSequence(KEY_REPLY) ?: return
            val recipientId = intent.getStringExtra(RECIPIENT_ID)

            val category = MessageCategory.PLAIN_TEXT.name

            //val category = if (intent.getBooleanExtra(IS_PLAIN, false)) {
             //   MessageCategory.PLAIN_TEXT.name
           // } else {
           //     MessageCategory.SIGNAL_TEXT.name
            //}
            val manager = getSystemService<NotificationManager>()
            manager?.cancel(recipientId.hashCode())
            val message = createMessage(UUID.randomUUID().toString(),Session.getUserId(), recipientId,
                     content.toString().trim(), nowInUtc())
            jobManager.addJobInBackground(SendMessageJob(message))
       /*     messageDao.findUnreadMessagesSync(conversationId)?.let { list ->
                if (list.isNotEmpty()) {
                    messageDao.batchMarkReadAndTake(conversationId, Session.getAccountId()!!, list.last().created_at)
                    list.map { BlazeAckMessage(it.id, MessageStatus.READ.name) }.let { messages ->
                        val chunkList = messages.chunked(100)
                        for (item in chunkList) {
                            jobManager.addJobInBackground(SendAckMessageJob(createAckListParamBlazeMessage(item)))
                        }
                    }
                }
            }*/
        }
    }
}
