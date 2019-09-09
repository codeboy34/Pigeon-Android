package com.pigeonmessenger.job

import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint

class BlockJob(private var userId: String) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(BlockJob.GROUP).groupBy("relationship").requireNetwork(), "") {
    override fun cancel() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.CANCEL
    }

    override fun onRun() {
        super.onRun()
        contactsService.block(userId).subscribe {
            if (it.isSuccessful) {
                contactDao.block(userId)
            }else{

            }
        }
    }

    companion object {
        val GROUP = "BlockJob"
    }
}