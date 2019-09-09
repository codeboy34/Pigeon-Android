package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.extension.getGroupAvatarPath
import com.pigeonmessenger.extension.save
import com.pigeonmessenger.extension.toBitmap
import com.pigeonmessenger.vo.MessageEntity
import com.pigeonmessenger.vo.SystemConversationAction

class UpdateConversationJob(private var message: MessageEntity) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(RefreshConversationJob.GROUP).groupBy("refresh_conversation")
        .requireNetwork().persist(), message.conversationId) {

    companion object {
        private val TAG = "UpdateConversationJob"
    }

    override fun cancel() {

    }

    override fun onRun() {
        Log.d(TAG, "onRunn: ");
        when (message.action) {
            SystemConversationAction.UPDATE_NAME.name -> {
                Log.d(TAG, "Update name");
                val response = conversationApi.name(message.conversationId).execute()
                if (response.isSuccessful && response.body() != null) {
                    val iconResponse = response.body()
                    iconResponse!!.name?.let {
                        conversationDao.updateGroupName(message.conversationId, it)
                    }
                }
            }
            SystemConversationAction.UPDATE_ICON.name -> {
                Log.d(TAG, "Update icon");
                val response = conversationApi.iconDownload(message.conversationId).execute()
                if (response.isSuccessful && response.body() != null) {
                    val iconResponse = response.body()
                    iconResponse?.let {
                        if (it.icon != null) {
                            val iconUrl = applicationContext.getGroupAvatarPath(message.conversationId)
                            it.icon.toBitmap()!!.save(iconUrl)
                        } else {
                            applicationContext.getGroupAvatarPath(message.conversationId).delete()
                        }
                        conversationRepo.updateGroupIcon(message.conversationId, iconResponse.thumbnail)
                    }
                }
            }
        }
        messageDao.insert(message)
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.e(TAG,"shouldReRunOnThrowable",p0)
        return RetryConstraint.CANCEL
    }


}