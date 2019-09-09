package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.google.gson.Gson
import com.pigeonmessenger.Session
import com.pigeonmessenger.vo.PlainDataAction
import com.pigeonmessenger.vo.TransferPlainData
import com.pigeonmessenger.vo.createParamBlazeMessage
import com.pigeonmessenger.vo.createPlainJsonParam

class TestJob: BaseJob(Params(PRIORITY_SEND_MESSAGE).addTags("").groupBy("send_message_group")
        .requireWebSocketConnected().persist(), "")
{
    companion object {
        const val TAG ="TestJob"
    }
    override fun cancel() {

    }

    override fun onRun() {
        super.onRun()
        requestResendKey("","8949501678","fff")
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, " shouldReRunOnThrowable $p0: ");
        return RetryConstraint.CANCEL
    }

    private fun requestResendKey(conversationId: String, userId: String, messageId: String) {

        Log.d(TAG, "requestResendKey...: ")

        val plainText = Gson().toJson(TransferPlainData(
                action = PlainDataAction.RESEND_KEY.name,
                messageId = messageId
        ))

        val encoded = com.pigeonmessenger.crypto.Base64.encodeBytes(plainText.toByteArray())
        val bm = createParamBlazeMessage(conversationId, Session.getUserId(), userId, createPlainJsonParam(encoded))
        jobManager.addJobInBackground(SendPlaintextJob(bm, userId))

        //val address = SignalProtocolAddress(userId, SignalProtocol.DEFAULT_DEVICE_ID)
        //val ratchet = RatchetSenderKey(conversationId, address.toString(), RatchetStatus.REQUESTING.name, bm.messageParam?.message_id, nowInUtc())
       /// ratchetSenderKeyDao.insert(ratchet)
    }

}