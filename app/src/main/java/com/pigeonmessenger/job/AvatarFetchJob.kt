package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.extension.avatarFile
import com.pigeonmessenger.extension.save
import com.pigeonmessenger.extension.toBitmap
import java.io.File


class AvatarFetchJob(var userId: String) : BaseJob(Params(PRIORITY_BACKGROUND)
        .addTags().groupBy("download_avatar").requireNetwork(), "") {

    private val TAG = "AvatarFetchJob"

    private val avatarFile: File by lazy {
        applicationContext.avatarFile(userId)
    }

    override fun cancel() {}

    override fun onRun() {
        Log.d(TAG, "onRunn() $userId ");
        val response = contactsService.fetchAvatar(userid = userId).execute()
        if (response.isSuccessful) {
            val account = response.body()
            Log.d(TAG, "account $account ")
            if (account!!.avatar != null) {
                if (!avatarFile.exists()) avatarFile.createNewFile()
                val bitmap = account.avatar!!.toBitmap()
                bitmap?.save(avatarFile)
                contactDao.updateThumbnail(userId, account.thumbnail)
            } else {
                if (avatarFile.exists()) {
                    avatarFile.delete()
                }
                contactDao.updateThumbnail(userId, null)
            }

            contactDao.updateLastUpdate(userId)
        } else {
            Log.d(TAG, "Error ${response.code()}")
        }
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "onThrowable $p0");
        return RetryConstraint.CANCEL
    }


}