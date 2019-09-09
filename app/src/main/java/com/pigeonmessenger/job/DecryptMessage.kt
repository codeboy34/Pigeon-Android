package com.pigeonmessenger.job

import android.util.Base64
import android.util.Log
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.crypto.SignalProtocol
import com.pigeonmessenger.crypto.SignalProtocol.Companion.DEFAULT_DEVICE_ID
import com.pigeonmessenger.crypto.vo.RatchetSenderKey
import com.pigeonmessenger.crypto.vo.RatchetStatus
import com.pigeonmessenger.database.room.entities.ConversationStatus
import com.pigeonmessenger.di.Injector
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.fragment.settings.*
import com.pigeonmessenger.job.BaseJob.Companion.PRIORITY_SEND_ATTACHMENT_MESSAGE
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.GsonHelper.customGson
import com.pigeonmessenger.vo.*
import org.whispersystems.libsignal.DecryptionCallback
import org.whispersystems.libsignal.NoSessionException
import org.whispersystems.libsignal.SignalProtocolAddress
import java.util.*
import javax.inject.Inject

class DecryptMessage : Injector() {

    companion object {
        val TAG = DecryptMessage::class.java.simpleName
        const val GROUP = "DecryptMessage"
    }

    private var refreshKeyMap = arrayMapOf<String, Long?>()

    @Inject
    lateinit var conversationRepo: ConversationRepo

    private var handled = false

    fun onRun(data: FloodMessage) {
        Log.d(TAG, "onRun: ");
        processMessage(data)
    }

    private fun isExistMessage(messageId: String): Boolean {
        return false
    }

    private fun processMessage(floodMessage: FloodMessage) {
        syncConversation(floodMessage)
        processSystemConversationMessage(floodMessage)
        processPlainMessage(floodMessage)
        processSignalMessage(floodMessage)
    }

    private fun processPlainMessage(floodMessage: FloodMessage) {
        Log.d(TAG, "processPlainMessage(): ");
        if (!floodMessage.category.startsWith("PLAIN_")) {
            return
        }

        Log.d(TAG, "processing Plain Message ...: ");
        val params = gson.fromJson<BlazeMessageParam>(floodMessage.data, BlazeMessageParam::class.java)
        if (params.category == MessageCategory.PLAIN_JSON.name) {
            val json = com.pigeonmessenger.crypto.Base64.decode(params.data)
            val plainData = gson.fromJson(String(json), TransferPlainData::class.java)
            if (plainData.action == PlainDataAction.RESEND_KEY.name) {
                Log.d(TAG, "ACTION Resend_Key: ");
                if (signalProtocol.containsSession(floodMessage.senderId)) {
                    Log.d(TAG, ":Sending KEY ");
                    jobManager.addJobInBackground(SendProcessSignalKeyJob(floodMessage.conversationId, floodMessage.senderId, ProcessSignalKeyAction.RESEND_KEY))
                }
            } else if (plainData.action == PlainDataAction.RESEND_MESSAGES.name) {
                for (mId in plainData.messages!!) {
                    val resendMessage = resendMessageDao.findResendMessage(floodMessage.senderId, mId)
                    if (resendMessage != null) {
                        continue
                    }
                    val needResendMessage = messageDao.findMessageById(mId)
                    if (needResendMessage != null) {
                        needResendMessage.id = UUID.randomUUID().toString()
                        jobManager.addJobInBackground(SendMessageJob(needResendMessage,
                                true, ResendData(floodMessage.senderId, mId), messagePriority = PRIORITY_SEND_ATTACHMENT_MESSAGE))
                        resendMessageDao.insert(ResendMessage(mId, floodMessage.senderId, 1, nowInUtc()))
                    } else {
                        resendMessageDao.insert(ResendMessage(mId, floodMessage.senderId, 0, nowInUtc()))
                    }
                }
            } else if (plainData.action == PlainDataAction.NO_KEY.name) {
                ratchetSenderKeyDao.delete(floodMessage.conversationId, SignalProtocolAddress(floodMessage.senderId,
                        DEFAULT_DEVICE_ID).toString())
            }
            // updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            //  messageHistoryDao.insert(MessageHistory(data.messageId))
        }
        handled = true
    }

    private fun processSignalMessage(floodMessage: FloodMessage) {
        if (!floodMessage.category.startsWith("SIGNAL_")) {
            return
        }

        Log.d(TAG, "processing signal message ...: ");
        val data = gson.fromJson<BlazeMessageParam>(floodMessage.data, BlazeMessageParam::class.java)
        Log.d(TAG, ":Decrypting message $data ");
        val (keyType, cipherText, resendMessageId) = SignalProtocol.decodeMessageData(data.data!!)
        try {
            signalProtocol.decrypt(floodMessage.conversationId, floodMessage.senderId, keyType, cipherText,
                    data.category, DecryptionCallback {
                Log.d(TAG, "Decrypt success..: ");
                Log.d(TAG, "data ${String(it)}: ");
                if (data.category != MessageCategory.SIGNAL_KEY.name) {
                    val plaintext = String(it)
                    if (resendMessageId != null) {
                        processRedecryptMessage(floodMessage, data, resendMessageId, plaintext)
                    } else {
                        processDecryptSuccess(floodMessage, plaintext)
                    }
                }
            })

            val address = SignalProtocolAddress(floodMessage.senderId, DEFAULT_DEVICE_ID)
            val status = ratchetSenderKeyDao.getRatchetSenderKey(floodMessage.conversationId, address.toString())?.status
            if (status != null) {
                if (status == RatchetStatus.REQUESTING.name) {
                    requestResendMessage(floodMessage.conversationId, floodMessage.senderId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "decrypt failed ", e)
            if (e !is NoSessionException) {
                /* Bugsnag.beforeNotify {
                     it.addToTab("Decrypt", "conversation", data.conversationId)
                     it.addToTab("Decrypt", "message_id", data.messageId)
                     it.addToTab("Decrypt", "user", data.userId)
                     it.addToTab("Decrypt", "data", data.data)
                     it.addToTab("Decrypt", "category", data.category)
                     it.addToTab("Decrypt", "created_at", data.createdAt)
                     it.addToTab("Decrypt", "resend_message", resendMessageId ?: "")
                     true
                 }
                 Bugsnag.notify(e)*/
            }

            if (resendMessageId != null) {
                return
            }

            if (data.category == MessageCategory.SIGNAL_KEY.name) {
                ratchetSenderKeyDao.delete(floodMessage.conversationId, SignalProtocolAddress(floodMessage.senderId,
                        DEFAULT_DEVICE_ID).toString())
                refreshKeys(floodMessage.conversationId)
            } else {
                insertFailedMessage(floodMessage, data)
                refreshKeys(floodMessage.conversationId)
                val address = SignalProtocolAddress(floodMessage.senderId, DEFAULT_DEVICE_ID)
                val status = ratchetSenderKeyDao.getRatchetSenderKey(floodMessage.conversationId, address.toString())?.status
                if (status == null || (status != RatchetStatus.REQUESTING.name && status != RatchetStatus.REQUESTING_MESSAGE.name)) {
                    requestResendKey(floodMessage.conversationId, floodMessage.senderId, floodMessage.id)
                }
            }
        }
        handled = true
    }

    private fun insertFailedMessage(floodMessage: FloodMessage, data: BlazeMessageParam) {
        if (data.category == MessageCategory.SIGNAL_TEXT.name ||
                data.category == MessageCategory.SIGNAL_IMAGE.name ||
                data.category == MessageCategory.SIGNAL_VIDEO.name ||
                data.category == MessageCategory.SIGNAL_DATA.name ||
                data.category == MessageCategory.SIGNAL_AUDIO.name ||
                data.category == MessageCategory.SIGNAL_STICKER.name ||
                data.category == MessageCategory.SIGNAL_CONTACT.name) {
            messageDao.insert(createMessage(data.message_id, floodMessage.conversationId,
                    floodMessage.senderId, data.data, floodMessage.createdAt, data.category, status = MessageStatus.FAILED))
        }
    }

    private fun requestResendMessage(conversationId: String, userId: String) {
        val messages = messageDao.findFailedMessages(conversationId, userId) ?: return
        val plainText = gson.toJson(TransferPlainData(PlainDataAction.RESEND_MESSAGES.name, messages.reversed()))
        val encoded = com.pigeonmessenger.crypto.Base64.encodeBytes(plainText.toByteArray())
        val bm = createParamBlazeMessage(conversationId, Session.getUserId(), userId, createPlainJsonParam(encoded))
        jobManager.addJobInBackground(SendPlaintextJob(bm))
        ratchetSenderKeyDao.delete(conversationId, SignalProtocolAddress(userId, DEFAULT_DEVICE_ID).toString())
    }

    private fun requestResendKey(conversationId: String, userId: String, messageId: String) {
        Log.d(TAG, "requestResendKey...: ")

        val plainText = gson.toJson(TransferPlainData(
                action = PlainDataAction.RESEND_KEY.name,
                messageId = messageId
        ))
        val encoded = com.pigeonmessenger.crypto.Base64.encodeBytes(plainText.toByteArray())
        val bm = createParamBlazeMessage(conversationId, Session.getUserId(), userId, createPlainJsonParam(encoded))
        jobManager.addJobInBackground(SendPlaintextJob(bm, userId))

        val address = SignalProtocolAddress(userId, DEFAULT_DEVICE_ID)
        val ratchet = RatchetSenderKey(conversationId, address.toString(), RatchetStatus.REQUESTING.name, bm.messageParam?.message_id, nowInUtc())
        ratchetSenderKeyDao.insert(ratchet)
    }

    private fun processDecryptSuccess(floodMessage: FloodMessage, content: String) {

        val context = App.get()
        val plainData = floodMessage.data
        val data = gson.fromJson<BlazeMessageParam>(plainData, BlazeMessageParam::class.java)
        when {
            data.category.endsWith("_TEXT") -> {
                val message = if (data.quote_message_id == null) {
                    createMessage(data.message_id, floodMessage.conversationId, floodMessage.senderId, content, floodMessage.createdAt,
                            MessageCategory.SIGNAL_TEXT.name, status = MessageStatus.DELIVERED).apply {
                        this.message?.findLastUrl()?.let { jobManager.addJobInBackground(ParseHyperlinkJob(it, data.message_id)) }
                    }
                } else {
                    val quoteMsg = messageDao.findMessageItemById(floodMessage.conversationId, data.quote_message_id)
                    if (quoteMsg != null) {
                        createReplyMessage(data.message_id, floodMessage.conversationId, floodMessage.senderId, data.category,
                                content, floodMessage.createdAt, MessageStatus.DELIVERED, data.quote_message_id, gson.toJson(quoteMsg))
                    } else {
                        createReplyMessage(data.message_id, floodMessage.conversationId, floodMessage.senderId, data.category,
                                content, floodMessage.createdAt, MessageStatus.DELIVERED, data.quote_message_id)
                    }
                }
                messageDao.insertMessage(message)
                sendNotificationJob(message)
            }
            data.category.endsWith("_IMAGE") -> {
                val decoded = Base64.decode(content, Base64.DEFAULT)
                val mediaData = gson.fromJson(String(decoded), TransferAttachmentData::class.java)
                if (mediaData.invalidData()) {
                    return
                }
                val mimeType = if (mediaData.mimeType.isNullOrEmpty()) mediaData.mineType else mediaData.mimeType
                val message = createMediaMessage(data.message_id, floodMessage.conversationId, floodMessage.senderId, data.category,
                        floodMessage.createdAt, null, null,
                        mimeType!!, mediaData.size, mediaData.width, mediaData.height, mediaData.thumbnail,
                        MediaStatus.CANCELED, MessageStatus.DELIVERED, mediaData.mediaKey)

                messageDao.insertMessage(message)

                val downOverMob = App.get().defaultSharedPreferences.getBoolean(PREF_DOWN_IMG_MOB, false)
                val downOverWifi = App.get().defaultSharedPreferences.getBoolean(PREF_DOWN_IMG_WIFI, false)
                if (Connectivity.isConnectedMobile(context) && downOverMob)
                    jobManager.addJobInBackground(AttachmentDownloadJob(message))
                else if (Connectivity.isConnectedMobile(context) && downOverWifi)
                    jobManager.addJobInBackground(AttachmentDownloadJob(message))
                sendNotificationJob(message)

            }

            data.category.endsWith("_VIDEO") -> {
                val decoded = Base64.decode(data.data, Base64.DEFAULT)
                val mediaData = gson.fromJson<TransferAttachmentData>(String(decoded), TransferAttachmentData::class.java)
                if (mediaData.invalidData()) {
                    return
                }
                val mimeType = if (mediaData.mimeType.isEmpty()) mediaData.mineType else mediaData.mimeType
                val message = createVideoMessage(data.message_id, floodMessage.senderId, floodMessage.conversationId,
                        data.category, mediaData.attachmentId, mediaData.mediaKey, null, mediaData.duration,
                        mediaData.width, mediaData.height, mediaData.thumbnail, mimeType!!,
                        mediaData.size, floodMessage.createdAt, MediaStatus.CANCELED, MessageStatus.DELIVERED)
                messageDao.insertMessage(message)


                sendNotificationJob(message)
            }

            data.category.endsWith("_DATA") -> {
                val decoded = Base64.decode(content, Base64.DEFAULT)

                Log.d(TAG, "Decoded ${String(decoded)}")

                val mediaData = gson.fromJson<TransferAttachmentData>(String(decoded), TransferAttachmentData::class.java)
                val mimeType = if (mediaData.mimeType.isEmpty()) mediaData.mineType else mediaData.mimeType

                val message = createAttachmentMessage(data.message_id, floodMessage.conversationId, floodMessage.senderId,
                        data.category,
                        null, mediaData.name, mediaData.mediaKey, null,
                        mimeType!!, mediaData.size, floodMessage.createdAt, MediaStatus.CANCELED, MessageStatus.SENDING)

                Log.d(TAG, "SYSTEM DATA MESSAGE $message")
                messageDao.insertMessage(message)

                val downOverMob = App.get().defaultSharedPreferences.getBoolean(PREF_DOWN_FILE_MOB, false)
                val downOverWifi = App.get().defaultSharedPreferences.getBoolean(PREF_DOWN_FILE_WIFI, false)
                if (Connectivity.isConnectedMobile(context) && downOverMob)
                    jobManager.addJobInBackground(AttachmentDownloadJob(message))
                else if (Connectivity.isConnectedMobile(context) && downOverWifi)
                    jobManager.addJobInBackground(AttachmentDownloadJob(message))

                sendNotificationJob(message)
            }

            data.category.endsWith("_AUDIO") -> {
                Log.d(TAG, "1")
                val decoded = Base64.decode(content, Base64.DEFAULT)
                val mediaData = gson.fromJson<TransferAttachmentData>(String(decoded), TransferAttachmentData::class.java)
                Log.d(TAG, "2")
                val message = createAudioMessage(data.message_id, floodMessage.conversationId, floodMessage.senderId,
                        data.category,
                        mediaData.size, null, mediaData.mediaKey, mediaData.duration.toString(), floodMessage.createdAt,
                        mediaData.waveform,
                        MediaStatus.CANCELED, MessageStatus.SENDING)

                Log.d(TAG, "3 $message")
                messageDao.insertMessage(message)
                Log.d(TAG, "4 Message inserted ...")

                val downOverMob = App.get().defaultSharedPreferences.getBoolean(PREF_DOWN_AUDIO_MOB, false)
                val downOverWifi = App.get().defaultSharedPreferences.getBoolean(PREF_DOWN_AUDIO_WIFI, false)
                if (Connectivity.isConnectedMobile(context) && downOverMob)
                    jobManager.addJobInBackground(AttachmentDownloadJob(message))
                else if (Connectivity.isConnectedMobile(context) && downOverWifi)
                    jobManager.addJobInBackground(AttachmentDownloadJob(message))

                sendNotificationJob(message)
            }
        }
    }

    private fun processSystemConversationMessage(data: FloodMessage) {
        if (data.category != MessageCategory.SYSTEM_CONVERSATION.name)
            return

        Log.d(TAG, "processSystemConversationMessage :${data} ");
        val systemMessage: SystemConversationData = gson.fromJson(data.data, SystemConversationData::class.java)
        var userId = data.senderId
        if (systemMessage.userId != null) {
            userId = systemMessage.userId
        }

        val message = createMessage(data.id, data.conversationId, userId, null, data.createdAt,
                MessageCategory.SYSTEM_CONVERSATION.name, action = systemMessage.action, participantId = systemMessage.participantId)

        val accountId = Session.getUserId()
        if (systemMessage.action == SystemConversationAction.ADD.name ||
                systemMessage.action == SystemConversationAction.JOIN.name) {
            participantDao.insert(Participant(data.conversationId, systemMessage.participantId!!, "", data.createdAt))
            if (systemMessage.participantId == accountId) {
                jobManager.addJobInBackground(RefreshConversationJob(data.conversationId))
            } else {
                jobManager.addJobInBackground(RefreshUsersJob(arrayListOf(systemMessage.participantId)))
            }

        } else if (systemMessage.action == SystemConversationAction.REMOVE.name ||
                systemMessage.action == SystemConversationAction.EXIT.name) {

            if (systemMessage.participantId == accountId)
                conversationDao.updateConversationStatusById(data.conversationId, ConversationStatus.QUIT.ordinal)
            participantDao.deleteById(data.conversationId, systemMessage.participantId!!)

        } else if (systemMessage.action == SystemConversationAction.CREATE.name) {
            // messageDao.findMessageById()
            //    messageDao.findGroupCreateMessage(message.conversationId)?:return

        } else if (systemMessage.action == SystemConversationAction.UPDATE_ICON.name ||
                systemMessage.action == SystemConversationAction.UPDATE_NAME.name) {
            jobManager.addJobInBackground(UpdateConversationJob(message))
            return
        } else if (systemMessage.action == SystemConversationAction.ROLE.name) {
            Log.d(TAG, "Adding role : ");
            participantDao.updateParticipantRole(data.conversationId,
                    systemMessage.participantId!!, systemMessage.role!!)
            if (message.participantId != accountId) {
                return
            }
        }
        Log.d(TAG, "inserting action: ");
        messageDao.insertMessage(message)
    }

    private fun processRedecryptMessage(floodMessage: FloodMessage, data: BlazeMessageParam, messageId: String, plainText: String) {
        if (data.category == MessageCategory.SIGNAL_TEXT.name) {
            messageDao.updateMessageContentAndStatus(plainText, MessageStatus.DELIVERED.name, messageId)
        } else if (data.category == MessageCategory.SIGNAL_IMAGE.name ||
                data.category == MessageCategory.SIGNAL_VIDEO.name ||
                data.category == MessageCategory.SIGNAL_AUDIO.name ||
                data.category == MessageCategory.SIGNAL_DATA.name) {
            val decoded = com.pigeonmessenger.crypto.Base64.decode(plainText)
            val mediaData = customGson.fromJson(String(decoded), TransferAttachmentData::class.java)
            val duration = if (mediaData.duration == null) null else mediaData.duration.toString()
            val mimeType = if (mediaData.mimeType.isEmpty()) mediaData.mineType else mediaData.mimeType

            messageDao.updateAttachmentMessage(messageId, mediaData.attachmentId, mimeType!!, mediaData.size,
                    mediaData.width, mediaData.height, mediaData.thumbnail, mediaData.name, mediaData.waveform, duration,
                    MediaStatus.CANCELED.name, MessageStatus.DELIVERED.name)
            if (data.category == MessageCategory.SIGNAL_IMAGE.name || data.category == MessageCategory.SIGNAL_AUDIO.name) {
                val message = messageDao.findMessageById(messageId)!!
                // jobManager.addJobInBackground(AttachmentDownloadJob(message))
            }
        }

        if (messageDao.countMessageByQuoteId(floodMessage.conversationId, messageId) > 0) {
            // messageDao.findMessageItemById(data.conversationId, messageId)?.let {
            //  messageDao.updateQuoteContentByQuoteId(data.conversationId, messageId, gson.toJson(it))
            //  }
        }
    }


    /*
        private fun syncUser(userId: String) {
            val user = userDao.findUser(userId)
            if (user == null) {
                try {
                    val call = userApi.getUserById(userId).execute()
                    val response = call.body()
                    if (response != null && response.isSuccess) {
                        response.data?.let { data ->
                            userDao.insert(data)
                        }
                    }
                } catch (e: IOException) {
                    jobManager.addJobInBackground(RefreshContactsJob(arrayListOf(userId)))
                }
            }
        }
        */

    private fun refreshKeys(conversationId: String) {
        val start = refreshKeyMap[conversationId] ?: 0.toLong()
        val current = System.currentTimeMillis()
        if (start == 0.toLong()) {
            refreshKeyMap[conversationId] = current
        }
        if (current - start < 1000 * 60) {
            return
        }
        refreshKeyMap[conversationId] = current

        val response = signalKeyService.getSignalKeyCount().execute()
        if (response.isSuccessful && response.body() != null) {
            val count = response.body()
            if (count!!.preKeyCount >= RefreshOneTimePreKeysJob.PREKEY_MINI_NUM) {
                return

            }
        }

        Log.w(TAG, "Registering new pre keys...")
    }

    private fun sendNotificationJob(message: MessageEntity) {
        if (App.conversationWith == message.conversationId) {
            return
        }
        jobManager.addJobInBackground(NotificationJob(message))
    }


}