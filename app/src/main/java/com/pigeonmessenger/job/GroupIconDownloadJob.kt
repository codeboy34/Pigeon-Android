package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.extension.*

class GroupIconDownloadJob(private val conversationId: String) : BaseJob(Params(PRIORITY_BACKGROUND)
        .addTags().groupBy("fetch_profiles").requireNetwork(), "") {

    companion object {
        private const val TAG = "GroupIconDownJob"
    }

    override fun cancel() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.e(TAG, "shouldReRunOnThrowable", p0)
        return RetryConstraint.CANCEL
    }

    override fun onRun() {
        val response = conversationApi.iconDownload(conversationId).execute()
        if (response.isSuccessful && response.body() != null) {
            response.body()?.let {
                Log.d(TAG,"GROUP ICON RESPONSE $it")
                if (it.icon != null) {
                    it.icon.toBitmap()?.save(applicationContext.avatarFile(conversationId , true))
                } else {
                    applicationContext.avatarFile(conversationId).delete()
                }
                conversationDao.updateGroupIcon(conversationId, it.thumbnail)
            }
        }
    }
}