package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.vo.BlazeMessage

class AckSendJob(var blazeMessage: BlazeMessage) : BaseJob(
Params(PRIORITY_SEND_MESSAGE).addTags("Message_tag").groupBy("message_job")
.requireWebSocketConnected().persist(), blazeMessage.id) {

    companion object {
        const val TAG= "AckSendJob"
    }
    override fun cancel() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "shouldReRunOnThrowable $p0: ");
       return RetryConstraint.RETRY
    }

    override fun onRun() {
        if (blazeMessage.recipientId.isNullOrEmpty())
            return

        socketManager.sendMessage(blazeMessage)
    }



}