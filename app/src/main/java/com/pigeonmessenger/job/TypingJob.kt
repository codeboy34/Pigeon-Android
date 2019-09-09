package com.pigeonmessenger.job

import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.Session
import com.pigeonmessenger.vo.createTypingBlazeMessage

class TypingJob (private var conversationId:String ,private var recipientId:String?):  BaseJob(
Params(PRIORITY_TYPING_MESSAGE).addTags("typing").groupBy("typing_job")
        .requireWebSocketConnected(), "")  {

    override fun cancel() {
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.CANCEL
    }

    override fun onRun() {
        val bm =createTypingBlazeMessage(conversationId,Session.getUserId(),recipientId)
        socketManager.sendMessage(bm)
    }
}