package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.extension.avatarFile

class RefreshContactsJob : BaseJob(Params(PRIORITY_BACKGROUND)
        .addTags().groupBy("fetch_profiles").requireNetwork(), "") {

    private val TAG = "RefreshContactsJob"
    override fun cancel() {

    }

    override fun onRun() {
        Log.d(TAG, "onRun() ")
        val profileResponse = contactsService.fetchProfiles().execute()
        if (profileResponse.isSuccessful) {
            val profiles = profileResponse.body()
            val registeredContacts = contactsRepo.getRegisteredContacts()
            Log.d(TAG, "onResponse  $profiles");

            profiles?.forEach {
                Log.d(TAG, "refreshing profile ${it}: ");

                val contact = registeredContacts.find { r -> r.userId == it.phone_number } ?: return

                Log.d(TAG, " $contact ")

                if (contact.full_name != it.full_name || contact.bio != it.bio)
                    contactDao.updateContact(it.phone_number, it.full_name, it.bio)

                if (contact.thumbnail != it.thumbnail) {
                    applicationContext.avatarFile(it.phone_number).run {
                        this.delete()
                    }
                    contactDao.updateThumbnail(it.phone_number, it.thumbnail)
                }
            }
        } else {
            Log.d(TAG, "${profileResponse.code()} ");
        }

    }


    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "THROWABLE $p0");
        return RetryConstraint.CANCEL
    }
}