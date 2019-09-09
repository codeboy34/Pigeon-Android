package com.pigeonmessenger.job

import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.vo.createEventMessage

class SendEventJob  : BaseJob(
        Params(PRIORITY_SEND_MESSAGE).addTags("Message_tag").groupBy("message_job")
                .requireWebSocketConnected(), "") {


    override fun onRun() {
        val eventblazeMessage = createEventMessage()
        socketManager.sendMessage(eventblazeMessage)
    }

    override fun cancel() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.CANCEL
    }
}