package com.pigeonmessenger.job

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.activities.ChatRoom
import com.pigeonmessenger.activities.SplashActivity
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.isGroup
import com.pigeonmessenger.extension.avatarFile
import com.pigeonmessenger.extension.mainThread
import com.pigeonmessenger.extension.notNullElse
import com.pigeonmessenger.extension.supportsNougat
import com.pigeonmessenger.services.SendService
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.vo.MessageEntity
import org.jetbrains.anko.notificationManager

class NotificationJob(val message: MessageEntity) :
        BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().groupBy("notification_group"), "") {
    override fun cancel() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.RETRY
    }

    companion object {
        private const val serialVersionUID = 1L
        const val CHANNEL_GROUP = "channel_group"
        const val CONVERSATION_ID = "conversation_id"
        const val CHANNEL_MESSAGE = "channel_message"
        const val KEY_REPLY = "key_reply"
        const val RECIPIENT_ID = "recipient_id"
        const val SENDER_ID = "sender_id"
        const val IS_PLAIN = "is_plain"
    }

    override fun onRun() {
        notifyMessage(message)
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private val notificationManager: NotificationManager by lazy {
        App.get().notificationManager
    }

    @SuppressLint("NewApi")
    private fun notifyMessage(message: MessageEntity) {
        val context = App.get()
        val user = syncUser(message.senderId) ?: return
        if (user.relationship == Relationship.BLOCKING.name) {
            return
        }
        val conversation: Conversation? = conversationRepo.findConversation(message.conversationId)

        val mainIntent = SplashActivity.getSingleIntent(context)
        val conversationIntent = ChatRoom.putIntent(context, message.conversationId, message.senderId)
        notificationBuilder = NotificationCompat.Builder(context, CHANNEL_MESSAGE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_MESSAGE, App.get().getString(R.string.notification_message),
                    NotificationManager.IMPORTANCE_HIGH)

            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }

        notificationBuilder.setContentIntent(
                PendingIntent.getActivities(context, message.id.hashCode(),
                        arrayOf(mainIntent, conversationIntent), PendingIntent.FLAG_UPDATE_CURRENT))
        supportsNougat {
            val remoteInput = RemoteInput.Builder(KEY_REPLY)
                    .setLabel(context.getString(R.string.notification_reply))
                    .build()
            val sendIntent = Intent(context, SendService::class.java)
            sendIntent.putExtra(CONVERSATION_ID, message.conversationId)
            sendIntent.putExtra(SENDER_ID, message.senderId)
            //  sendIntent.putExtra(IS_PLAIN, user.isBot() || message.isRepresentativeMessage(conversation))
            val pendingIntent = PendingIntent.getService(
                    context, message.conversationId.hashCode(), sendIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val action = NotificationCompat.Action.Builder(R.mipmap.ic_launcher,
                    context.getString(R.string.notification_reply), pendingIntent)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .build()
            notificationBuilder.addAction(action)
        }

        val titleName = if (conversation!!.isGroup()) {
            "${user.getName()} @${conversation.name}"
        } else user.getName()

        when (message.type) {
            MessageCategory.SIGNAL_TEXT.name, MessageCategory.PLAIN_TEXT.name -> {
                notificationBuilder.setContentTitle(titleName)
                notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_text_message))
                notificationBuilder.setContentText(message.message)
            }
            MessageCategory.SIGNAL_IMAGE.name, MessageCategory.PLAIN_IMAGE.name -> {
                notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_image_message))
                notificationBuilder.setContentTitle(titleName)
                notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_image_message))

            }
            MessageCategory.SIGNAL_VIDEO.name, MessageCategory.PLAIN_VIDEO.name -> {
                notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_video_message))
                notificationBuilder.setContentTitle(titleName)
                notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_video_message))

            }
            MessageCategory.SIGNAL_DATA.name, MessageCategory.PLAIN_DATA.name -> {
                notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_data_message))
                notificationBuilder.setContentTitle(titleName)
                notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_data_message))
            }
            MessageCategory.SIGNAL_AUDIO.name, MessageCategory.PLAIN_AUDIO.name -> {
                notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_audio_message))
                notificationBuilder.setContentTitle(titleName)
                notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_audio_message))
            }
            MessageCategory.SIGNAL_CONTACT.name, MessageCategory.PLAIN_CONTACT.name -> {
                notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_contact_message))
                notificationBuilder.setContentTitle(titleName)
                notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_contact_message))
            }

            MessageCategory.WEBRTC_AUDIO_OFFER.name -> {
                notificationBuilder.setContentTitle(titleName)
                notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_audio_call_message))
            }
            else -> {
                // No support
                return
            }
        }
        notificationBuilder.setSmallIcon(R.drawable.ic_action_chat)
        notificationBuilder.color = ContextCompat.getColor(context, R.color.gray_light)
        notificationBuilder.setWhen(System.currentTimeMillis())

        notificationBuilder.setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.pigeon))
        notificationBuilder.setAutoCancel(true)
        notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
        notNullElse(user, {
            context.mainThread {
                val height = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
                val width = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

                Glide.with(context)
                        .asBitmap()
                        .load(context.avatarFile(user.userId))
                        .apply(RequestOptions().fitCenter().circleCrop())
                        .listener(object : RequestListener<Bitmap> {
                            override fun onResourceReady(
                                    resource: Bitmap?,
                                    model: Any?,
                                    target: Target<Bitmap>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                            ): Boolean {
                                notificationBuilder.setLargeIcon(resource)
                                notificationManager.notify(message.senderId.hashCode(), notificationBuilder.build())
                                return false
                            }

                            override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Bitmap>?,
                                    isFirstResource: Boolean
                            ):
                                    Boolean {
                                notificationBuilder.setLargeIcon(
                                        BitmapFactory.decodeResource(context.resources, R.drawable.avatar_contact))
                                notificationManager.notify(message.senderId.hashCode(), notificationBuilder.build())
                                return false
                            }
                        }).submit(width, height)
            }
        }, {
            notificationManager.notify(message.senderId.hashCode(), notificationBuilder.build())
        })
    }

    private fun syncConversation(recipient: String) {
        val conversation = conversationRepo.findConversation(recipient)
        if (conversation == null) {
            // val conversationEntity = Conversation(recipient)
            // conversationRepo.createConversation(conversationEntity)
        }
        //   conversationRepo.updateMediaStatusStatus()
    }

    private fun syncUser(userId: String): User? {
        val u: User? = contactDao.findUser(userId)
        if (u == null) {
            val response = contactsService.fetchProfile(userId).execute()
            if (response.isSuccessful) {
                response.body()?.let { data ->
                    val contactEntity = User(userId = data.phone_number, full_name = data.full_name,
                            relationship = Relationship.STRANGE.name)
                    contactsRepo.upsert(contactEntity)
                }
            }
        }
        return u
    }

}