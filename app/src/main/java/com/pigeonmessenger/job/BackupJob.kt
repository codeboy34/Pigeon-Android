package com.pigeonmessenger.job

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.WEEK_IN_MILLIS
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.utils.Constant.BackUp.BACKUP_LAST_TIME
import com.pigeonmessenger.utils.Constant.BackUp.BACKUP_PERIOD
import com.pigeonmessenger.utils.backup.BackupLiveData
import com.pigeonmessenger.utils.backup.BackupNotification
import com.pigeonmessenger.utils.backup.Result
import com.pigeonmessenger.utils.backup.backup
import java.io.File

class BackupJob(private val force: Boolean = false) : BaseJob(Params(if (force) {
    PRIORITY_UI_HIGH
} else {
    PRIORITY_BACKGROUND
}).addTags(GROUP).requireNetwork().persist(),"") {
    override fun cancel() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "shouldReRunOnThrowable $p0: ");
        return RetryConstraint.CANCEL
    }

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "BackupJob"
        val backupLiveData = BackupLiveData()
        private const val TAG ="BackupJob"
    }

    override fun onRun() {
        val context = App.get()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (force) {
            backup(context)
        } else {
            val option = context.defaultSharedPreferences.getInt(BACKUP_PERIOD, 0)
            if (option in 1..3) {
                val currentTime = System.currentTimeMillis()
                val lastTime = context.defaultSharedPreferences.getLong(BACKUP_LAST_TIME, currentTime)
                val timeDiff = currentTime - lastTime
                if (timeDiff >= when (option) {
                        1 -> DAY_IN_MILLIS
                        2 -> WEEK_IN_MILLIS
                        3 -> DAY_IN_MILLIS * 30
                        else -> Long.MAX_VALUE
                    }) {
                    backup(context)
                }
            }
        }
    }

    private fun cleanMedia() {
        val mediaCachePath = App.get().getCacheMediaPath()
        val mediaPath = App.get().getMediaPath().absolutePath
        if (!mediaCachePath.exists()) {
            return
        }
        for (mediaCacheChild in mediaCachePath.listFiles()) {
            if (mediaCacheChild.isDirectory) {
                val local = File("$mediaPath${File.separator}${mediaCacheChild.name}${File.separator}")
                mediaCacheChild.moveChileFileToDir(local) { newFile, oldFile ->
                    messageDao.updateMediaUrl(newFile.toUri().toString(), oldFile.toUri().toString())
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun backup(context: Context) {
        try {
            backupLiveData.start()
            BackupNotification.show()
            cleanMedia()
            backup(context) { result ->
                backupLiveData.setResult(false, result)
                BackupNotification.cancel()
                if (result == Result.SUCCESS) {
                    context.defaultSharedPreferences.putLong(BACKUP_LAST_TIME, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            backupLiveData.setResult(false, null)
            BackupNotification.cancel()
            throw e
        }
    }
}