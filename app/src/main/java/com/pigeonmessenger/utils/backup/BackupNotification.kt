package com.pigeonmessenger.utils.backup

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.activities.SetupAccountActivity
import org.jetbrains.anko.notificationManager

class BackupNotification {
    companion object {
        private const val BACKUP_ID = 313389
        private const val CHANNEL_NODE = "channel_node"
        val FROM_NOTIFICATION="notification"

        private fun getBackupNotification(context: Context, backup: Boolean = true): Notification {
            val callIntent = Intent(context, SetupAccountActivity::class.java)
            callIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            callIntent.putExtra(FROM_NOTIFICATION, true)
            val pendingCallIntent = PendingIntent.getActivity(context, 0, callIntent, FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context, CHANNEL_NODE)
                .setSmallIcon(R.drawable.ic_action_chat)
                .setContentIntent(pendingCallIntent)
                .setOngoing(true)
                .setContentTitle(context.getString(if (backup) R.string.backup_notification_title else R.string.restore_notification_title))
                .setContentText(context.getString(if (backup) R.string.backup_notification_content else R.string.restore_notification_content))

            return builder.build()
        }

        private val notificationManager: NotificationManager by lazy {
            App.get().notificationManager
        }

        fun show(backup: Boolean = true) {
            notificationManager.notify(BACKUP_ID, getBackupNotification( App.get(), backup))
        }

        fun cancel() {
            notificationManager.cancel(BACKUP_ID)
        }
    }
}