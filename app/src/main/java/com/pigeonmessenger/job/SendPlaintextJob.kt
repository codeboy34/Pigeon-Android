package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.vo.BlazeMessage

class SendPlaintextJob(
        private val blazeMessage: BlazeMessage,
        val userId: String? = null,
        priority: Int = PRIORITY_SEND_MESSAGE
) : BaseJob(Params(priority).addTags(blazeMessage.id).groupBy("send_message_group")
        .requireWebSocketConnected().persist(), blazeMessage.id) {

    companion object {
        const val TAG ="SendPlaintextJob"
    }
    override fun cancel() {

    }


    override fun onRun() {
        Log.d(TAG, ":sending PlainMessage ");
        socketManager.sendMessage(blazeMessage)
    }
    override fun onAdded() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.RETRY
    }
}