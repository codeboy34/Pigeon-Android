package com.pigeonmessenger.job

import android.annotation.SuppressLint
import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.User

class RefreshUsersJob(private val userIds: List<String>) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork().persist(),"")   {

    companion object {
        private const val GROUP = "RefreshUserJob"
        private val TAG ="RefreshUsersJob"

    }
    override fun cancel() {

    }

    @SuppressLint("LogNotTimber")
    override fun onRun() {
        Log.d(TAG, "Refreshing Users for ${userIds}: ")
        val response = contactsService.fetchUsers(userIds).execute()
        if (response.isSuccessful){
            Log.d(TAG, "body : ${response.body()}")
            response.body()?.map {
                User(userId = it.phone_number,full_name = it.full_name,bio = it.bio,thumbnail = it.thumbnail,relationship = Relationship.STRANGE.name)
            }.let {
                contactDao.insert(it!!)
            }
        }else{
            Log.d(TAG, "Error error code ${ response.code() }: ");
        }
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.RETRY
    }
}