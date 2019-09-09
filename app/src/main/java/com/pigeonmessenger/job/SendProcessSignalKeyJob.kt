package com.pigeonmessenger.job


import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.google.gson.Gson
import com.pigeonmessenger.Session
import com.pigeonmessenger.api.createPreKeyBundle
import com.pigeonmessenger.crypto.Base64
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.vo.*
import java.util.UUID

class SendProcessSignalKeyJob(
    val conversationId: String,
    val senderId:String,
    val action: ProcessSignalKeyAction,
    val participantId: String? = null,
    priority: Int = PRIORITY_SEND_MESSAGE
) : BaseJob(Params(priority).groupBy("send_message_group").requireWebSocketConnected().persist(),
    UUID.randomUUID().toString()) {


    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.RETRY
    }

    companion object {
        private const val serialVersionUID = 1L
        private val TAG="SendProcessSignalKeyJob"
    }

    override fun onRun() {
        if (action == ProcessSignalKeyAction.RESEND_KEY) {
            Log.d(TAG, "RESEND_KEY: ");
            val result = redirectSendSenderKey(conversationId,senderId)
            if (!result) {
                sendNoKeyMessage(conversationId, senderId)
            }
        } else if (action == ProcessSignalKeyAction.REMOVE_PARTICIPANT) {
            val accountId = Session.getUserId()
            signalProtocol.clearSenderKey(conversationId, accountId)
        } else if (action == ProcessSignalKeyAction.ADD_PARTICIPANT) {
           // sendSenderKey(data.conversationId, participantId!!)
        }
    }

    private fun redirectSendSenderKey(conversationId: String, recipientId: String): Boolean {
        Log.d(TAG, "redirectSendSenderKey: ");
            val keys = signalKeyService.consumeSignalKeys(recipientId).execute().body()
            if (keys!!.count()>0) {
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle)
            } else {
                sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId, SentSenderKeyStatus.UNKNOWN.ordinal))
                Log.e(TAG, "No any signal key from server" + SentSenderKeyStatus.UNKNOWN.ordinal)
                return false
            }
        Log.d(TAG, "encryptSenderKey: ");
        val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, recipientId)
        if (err) {
            Log.d(TAG, "ERROR so returning : "); return false}
        val param = createSignalKeyParam(cipherText!!)
        val bm = BlazeMessage(UUID.randomUUID().toString(), Session.getUserId(), conversationId,
                recipientId, BlazeMessageCategory.CHAT_MESSAGE.name, param, createdAt = nowInUtc())
        Log.d(TAG, "Sending sender key...: ");
        deliver(bm)
        Log.d(TAG, "Sent: ");
        sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId,
                SentSenderKeyStatus.SENT.ordinal, senderKeyId))
        return true
    }

    private fun deliver(blazeMessage: BlazeMessage) {
        socketManager.sendMessage(blazeMessage)
    }

    private fun sendNoKeyMessage(conversationId: String, recipientId: String) {
        val plainText = Gson().toJson(TransferPlainData(PlainDataAction.NO_KEY.name))
        val encoded = Base64.encodeBytes(plainText.toByteArray())
        val params =  createPlainJsonParam(encoded)
        val  bm =BlazeMessage(UUID.randomUUID().toString(),Session.getUserId(),conversationId,recipientId, BlazeMessageCategory.CHAT_MESSAGE.name,params,createdAt = nowInUtc())
        deliver(bm)
    }


    override fun cancel() {
    }
}

enum class ProcessSignalKeyAction { ADD_PARTICIPANT, REMOVE_PARTICIPANT, RESEND_KEY }