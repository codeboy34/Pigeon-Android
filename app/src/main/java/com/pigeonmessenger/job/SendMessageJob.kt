package com.pigeonmessenger.job

import android.annotation.SuppressLint
import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.Session
import com.pigeonmessenger.api.createPreKeyBundle
import com.pigeonmessenger.database.room.entities.isGroup
import com.pigeonmessenger.extension.findLastUrl
import com.pigeonmessenger.extension.insertMessage
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.vo.*
import java.util.*

data class SendMessageJob(private val message: MessageEntity
                          , private var alreadyAdded: Boolean = false,
                          private val resendData: ResendData? = null,
                          private val messagePriority: Int = PRIORITY_SEND_MESSAGE) : BaseJob(
        Params(messagePriority).addTags(message.id).groupBy("send_message_group")
                .requireWebSocketConnected().persist(), message.id) {


    override fun cancel() {

    }

    private var TAG = "SendMessageJob"


    override fun onRun() {
        if (message.isPlain() || message.isCall()) {
            val conversation = conversationDao.findConversationById(message.conversationId)!! //TODO
            val messageParams = createChatMessageBlazeParamas(message.id, message.type, message.message, message.quoteMessageId)
            val blazeMessage = createCallBlazeMessage(message.conversationId,conversation.ownerId, messageParams)
            socketManager.sendMessage(blazeMessage)
        } else {
            sendSignalMessage()
        }

        if (!message.isCall()) messageDao.updateAck(message.id, MessageStatus.SENT.name)

    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
        Log.d(TAG, "onThrowable ${throwable}: ");
        return RetryConstraint.RETRY
    }

    override fun onAdded() {
        if (message.isCall()) {
            return
        }

        if (!alreadyAdded) {
            messageDao.insertMessage(message)
            parseHyperlink()
        }
    }


    private fun parseHyperlink() {
        if (message.type.endsWith("_TEXT")) {
            message.type.findLastUrl()?.let {
                jobManager.addJobInBackground(ParseHyperlinkJob(it, message.id))
            }
        }
    }


    private fun sendSignalMessage() {
        if (resendData != null) {
            if (checkSignalSession(resendData.userId)) {
                deliver(encryptedBlazeMessage(resendData.userId))
            }
            return
        }
        val conversation = conversationDao.findConversationById(message.conversationId)?:return
        if (signalProtocol.isExistSenderKey(message.conversationId, message.senderId)) {
            Log.d(TAG, "senderKeyExists ");
            val recipientId = if (conversation.isGroup()) {
                checkSentSenderKey(message.conversationId)
                null
            } else conversation.ownerId
            deliver(encryptedBlazeMessage(recipientId))
        } else {
            Log.d(TAG, "senderKey not exists : ");
            if (conversation.isGroup()) {
                sendGroupSenderKey(message.conversationId)
                deliver(encryptedBlazeMessage())
            } else {
                sendSenderKey(message.conversationId, conversation.ownerId!!)
                deliver(encryptedBlazeMessage(conversation.ownerId))
            }
        }
    }

    private fun deliver(blazeMessage: BlazeMessage) {
        socketManager.sendMessage(blazeMessage)
    }

    private fun sendGroupSenderKey(conversationId: String) {
        val participants = participantDao.getNotSentKeyParticipants(conversationId, Session.getUserId())
                ?: return
        sendBatchSenderKey(conversationId, participants)
    }

    private fun checkSentSenderKey(conversationId: String) {
        val participants = participantDao.getNotSentKeyParticipants(conversationId, Session.getUserId())
                ?: return
        if (participants.size == 1) {
            sendSenderKey(conversationId, participants[0].userId)
        } else if (participants.size > 1) {
            sendBatchSenderKey(conversationId, participants)
        }
    }

    private fun sendSenderKey(conversationId: String, recipientId: String): Boolean {
        Log.d(TAG, "sendSenderKey...: ");
        if (!signalProtocol.containsSession(recipientId)) {
            Log.d(TAG, "session not contains : ");
            Log.d(TAG, "consuming signal keys: ");
            val keys = signalKeyService.consumeSignalKeys(recipientId).execute().body()
            if (keys!!.count() > 0) {
                Log.d(TAG, "${keys[0]}: ");
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle)
            } else {
                sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId, SentSenderKeyStatus.UNKNOWN.ordinal))
                Log.e(TAG, "No any signal key from server" + SentSenderKeyStatus.UNKNOWN.ordinal)
                return false
            }
        }
        val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, recipientId)
        if (err) return false
        val param = createSignalKeyParam(cipherText!!)
        val bm = BlazeMessage(UUID.randomUUID().toString(), message.senderId, conversationId,
                recipientId, BlazeMessageCategory.CHAT_MESSAGE.name, param, createdAt = nowInUtc())
        deliver(bm)
        sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId,
                SentSenderKeyStatus.SENT.ordinal, senderKeyId))
        return true
    }

    private fun sendBatchSenderKey(conversationId: String, participants: List<Participant>) {
        Log.d(TAG, "sendBatchSenderKey: ");

        val requestSignalKeyUsers = arrayListOf<String>()
        val signalKeyMessages = ArrayList<BlazeSignalKeyMessage>()
        for (p in participants) {
            if (!signalProtocol.containsSession(p.userId)) {
                requestSignalKeyUsers.add(p.userId)
            } else {
                val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, p.userId)
                if (err) {
                    requestSignalKeyUsers.add(p.userId)
                } else {
                    signalKeyMessages.add(createBlazeSignalKeyMessage(p.userId, cipherText!!, senderKeyId))
                }
            }
        }

        if (requestSignalKeyUsers.isNotEmpty()) {
            Log.d(TAG, "requetSignalKeys ...: ");
            val signalKeys = signalKeyService.consumeSignalKeys(requestSignalKeyUsers).execute().body()
            Log.d(TAG, "Request Done: ");
            val keys = arrayListOf<String>()
            if (signalKeys!!.isNotEmpty()) {
                for (key in signalKeys) {
                    val preKeyBundle = createPreKeyBundle(key)
                    signalProtocol.processSession(key.userId!!, preKeyBundle)
                    val (cipherText, senderKeyId, _) = signalProtocol.encryptSenderKey(conversationId, key.userId)
                    signalKeyMessages.add(createBlazeSignalKeyMessage(key.userId, cipherText!!, senderKeyId))
                    keys.add(key.userId)
                }
            } else {
                Log.e(TAG, "No any group signal key from server")
            }

            val noKeyList = requestSignalKeyUsers.filter { !keys.contains(it) }
            if (noKeyList.isNotEmpty()) {
                val sentSenderKeys = noKeyList.map {
                    SentSenderKey(conversationId, it, SentSenderKeyStatus.UNKNOWN.ordinal)
                }
                sentSenderKeyDao.insertList(sentSenderKeys)
            }
        }
        if (signalKeyMessages.isEmpty()) {
            return
        }
        val bm = createSignalKeyMessage(conversationId, message.senderId, createSignalKeyMessageParam(signalKeyMessages))
        deliver(bm)
        val sentSenderKeys = signalKeyMessages.map {
            SentSenderKey(conversationId, it.recipient_id, SentSenderKeyStatus.SENT.ordinal, it.senderKeyId)
        }
        sentSenderKeyDao.insertList(sentSenderKeys)
    }


    @SuppressLint("LogNotTimber")
    private fun checkSignalSession(recipientId: String): Boolean {
        if (!signalProtocol.containsSession(recipientId)) {
            Log.d(TAG, "session not contains : ");
            Log.d(TAG, "consuming signal keys: ");
            val keys = signalKeyService.consumeSignalKeys(recipientId).execute().body()
            if (keys!!.count() > 0) {
                Log.d(TAG, "${keys[0]}: ");
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle)
            }
        }
        return true
    }

    private fun encryptedBlazeMessage(recipientId: String? = null): BlazeMessage {
        val encrypted = encryptNormalMessage()
        val messageParams = createChatMessageBlazeParamas(message.id, message.type, encrypted, message.quoteMessageId)
        return createChatBlazeMessage(message.conversationId, recipientId, messageParams, message.createdAt)

    }

    private fun encryptNormalMessage(): String {
        Log.d(TAG, "encryptNormalMessage: ")
        return if (resendData != null) {
            signalProtocol.encryptSessionMessage(message, resendData.userId, resendData.messageId)
        } else {
            signalProtocol.encryptGroupMessage(message)
        }
    }
}