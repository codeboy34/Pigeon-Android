package com.pigeonmessenger.job

import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.extension.avatarFile

class RefreshUserJob(var userId: String) : BaseJob(Params(PRIORITY_BACKGROUND)
        .addTags().groupBy("fetch_profiles").requireNetwork(), "") {


    override fun cancel() {

    }

    override fun onRun() {
        val user = contactDao.findUser(userId) ?: return

        val profileResponse = contactsService.fetchProfile(userId).execute()
        if (profileResponse.isSuccessful) {
            val profile = profileResponse.body()
            profile?.let {
                if (user.full_name != profile.full_name || user.bio != profile.bio) {
                    contactDao.updateContact(userId, profile.full_name, profile.bio)
                }
                if (profile.thumbnail != user.thumbnail) {
                    contactDao.updateThumbnail(userId, profile.thumbnail)
                    applicationContext.avatarFile(userId).delete()
                }
            }
        }
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.CANCEL
    }
}