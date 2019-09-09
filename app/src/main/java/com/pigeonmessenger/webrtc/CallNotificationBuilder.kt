package com.pigeonmessenger.webrtc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.CallActivity
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.supportsOreo
import com.pigeonmessenger.vo.CallState

class CallNotificationBuilder {

    companion object {
        private const val CHANNEL_NODE = "channel_node"
        const val WEBRTC_NOTIFICATION = 313388
        const val ACTION_EXIT = "action_exit"

        fun getCallNotification(context: Context, state: CallState, user: User?): Notification {
            val callIntent = Intent(context, CallActivity::class.java)
            callIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            user?.let {
                callIntent.putExtra(CallActivity.ARGS_ANSWER, it)
            }
            val pendingCallIntent = PendingIntent.getActivity(context, 0, callIntent, FLAG_UPDATE_CURRENT)

           supportsOreo {
                createNotificationChannel(context, CHANNEL_NODE,"channel_name")
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_NODE)
                .setSmallIcon(R.drawable.ic_action_chat)
                .setContentIntent(pendingCallIntent)
                .setOngoing(true)
                .setContentTitle(user?.getName())

            when (state.callInfo.callState) {
                CallService.CallState.STATE_DIALING -> {
                    builder.setContentText(context.getString(R.string.call_notification_outgoing))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_CANCEL, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_cancel) {
                        it.putExtra(CallService.EXTRA_TO_IDLE, true)
                    })
                }
                CallService.CallState.STATE_RINGING -> {
                    builder.setContentText(context.getString(R.string.call_notification_incoming_voice))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_ANSWER, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_answer))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_DECLINE, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_decline))
                }
                CallService.CallState.STATE_CONNECTED -> {
                    builder.setContentText(context.getString(R.string.call_notification_connected))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_LOCAL_END, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_hang_up) {
                        it.putExtra(CallService.EXTRA_TO_IDLE, true)
                    })
                }
                else -> {
                    builder.setContentText(context.getString(R.string.call_connecting))
                    val action = if (state.isInitiator) CallService.ACTION_CALL_CANCEL else CallService.ACTION_CALL_DECLINE
                    builder.addAction(getAction(context, action, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_hang_up) {
                        it.putExtra(CallService.EXTRA_TO_IDLE, true)
                    })
                }
            }
            return builder.build()
        }

        private fun getAction(
            context: Context,
            action: String,
            iconResId: Int,
            titleResId: Int,
            putExtra: ((intent: Intent) -> Unit)? = null
        ): NotificationCompat.Action {
            val intent = Intent(context, CallService::class.java)
            intent.action = action
            putExtra?.invoke(intent)
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Action(iconResId, context.getString(titleResId), pendingIntent)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun createNotificationChannel(context:Context, channelId: String, channelName: String): String{
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val notificationManager =  context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(chan)
            return channelId
        }

    }


}
