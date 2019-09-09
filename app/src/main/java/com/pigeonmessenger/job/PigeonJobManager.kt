package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import com.pigeonmessenger.extension.notNullElse
import java.util.concurrent.ConcurrentHashMap

class PigeonJobManager(config:Configuration) : JobManager(config){

    val TAG = "PigeonJobManager"

    private val map: ConcurrentHashMap<String, BaseJob> by lazy {
        ConcurrentHashMap<String, BaseJob>()
    }

    fun saveJob(job: Job) {
        if (job is BaseJob) {
            map[job.jobId] = job
        }
    }

    fun removeJob(id: String) {
        Log.d(TAG, "Removing Job ")
        val job : BaseJob?=map.remove(id)
        notNullElse(job,{
            Log.d(TAG, "job found ${it.id} ");
        },{
            Log.d(TAG, "job is null ");
        })
    }

    fun cancelJobById(id: String) {
        findJobById(id)?.cancel()
    }

    fun findJobById(id: String): BaseJob? {
        Log.d(TAG, "findJobById($id): ");
        return map[id]
    }

    fun cancelAllJob() {
        for (job in map.values) {
            job.cancel()
        }
        map.clear()
    }
}