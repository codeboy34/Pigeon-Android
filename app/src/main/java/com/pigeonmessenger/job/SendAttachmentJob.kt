package com.pigeonmessenger.job

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.google.firebase.storage.*
import com.google.gson.Gson
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.database.room.entities.AttachmentSessionEntity
import com.pigeonmessenger.events.ProgressEvent
import com.pigeonmessenger.extension.insertMessage
import com.pigeonmessenger.vo.MediaStatus
import com.pigeonmessenger.vo.MessageEntity
import com.pigeonmessenger.vo.TransferAttachmentData
import com.pigeonmessenger.vo.createMessage
import org.jetbrains.anko.doAsync
import java.util.concurrent.CountDownLatch


class SendAttachmentJob(val message: MessageEntity) : BaseJob(Params(PRIORITY_SEND_ATTACHMENT_MESSAGE)
        .addTags(message.id).groupBy("send_media_job").requireNetwork().persist(), message.id) {

    var isCancel: Boolean = false

    override fun cancel() {
        if (isCancel) {
            return
        }

        isCancel = true

        fileStorageReference?.let {
            if (it.isInProgress) {
                Log.d(TAG, "downloading.. ");
                it.pause()
            }
        }
    }

    val TAG = "SendAttachmentJob"


    @Transient
    private var fileStorageReference: StorageTask<UploadTask.TaskSnapshot>? = null


    override fun onRun() {

        jobManager.saveJob(this)
        val isSuccess = processAttachment()
        if (isSuccess) {

            val mediaDuration = if (message.mediaDuration != null) message.mediaDuration.toLong() else null
            val transferAttachmentData = TransferAttachmentData(null, null, message.mediaKey!!, message.id,
                    null, message.mediaMimeType!!, message.mediaSize!!, message.name, message.mediaWidth,
                    message.mediaHeight, message.thumbImage, mediaDuration, message.mediaWaveform)

            val json = Gson().toJson(transferAttachmentData)

            val encodedMessage = Base64.encodeToString(json.toByteArray(), Base64.DEFAULT)

            val sendMessage = createMessage(message.id, message.conversationId, message.senderId, encodedMessage, message.createdAt, message.type)
            jobManager.addJobInBackground(SendMessageJob(sendMessage, true))
        }
        removeJob()
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "Retry ", p0);
        return RetryConstraint.CANCEL
    }

    var progress = 1
    fun processAttachment(): Boolean {
        val latch = CountDownLatch(1)
        var sessionUri: String? = null
        var result = false

        val attachmentSessionEntity = attachmentSessionDao.getAttachment(message.id)
        //var attachmentSessionEntity : AttachmentSessionEntity? =  AttachmentSessionDb.getInstance(applicationContext).getAttachment(message.id)

        if (attachmentSessionEntity != null) {
            Log.d(TAG, "attSession :${attachmentSessionEntity.sessionUri} ");
            sessionUri = attachmentSessionEntity.sessionUri
        } else {
            Log.d(TAG, "attachmentSession null ");
        }


        val uri = if (sessionUri != null) Uri.parse(sessionUri) else null

        fileStorageReference = FirebaseStorage.getInstance().reference.child("attachment")
                .child(message.mediaKey!!).putFile(Uri.parse(message.mediaUrl),
                        StorageMetadata.Builder().build()
                        , uri)

        fileStorageReference!!
                .addOnSuccessListener {
                    Log.d(TAG, "onSuccess: ");
                    result = true
                    removeAttachmentSession()
                    updateMediaStatus(MediaStatus.DONE)
                    removeJob()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.d(TAG, "onCancelled ");
                    removeAttachmentSession()
                    updateMediaStatus(MediaStatus.CANCELED)
                    removeJob()
                    latch.countDown()
                }.addOnProgressListener {
                    if (sessionUri == null) {
                        val tempSessionUri: Uri? = it.uploadSessionUri
                        if (tempSessionUri != null) {
                            sessionUri = tempSessionUri.toString()
                            doAsync {
                                attachmentSessionDao.insert(AttachmentSessionEntity(message.id, tempSessionUri.toString()))
                            }
                        }
                    }
                    RxBus.publish(ProgressEvent(message.id, it.bytesTransferred.toFloat() / it.totalByteCount.toFloat()))
                }.addOnPausedListener {
                    Log.d(TAG, "onPauseListener: ");
                    updateMediaStatus(MediaStatus.CANCELED)
                    removeJob()
                    latch.countDown()
                }.addOnFailureListener {
                    Log.d(TAG, "onFailer ", it);
                    removeAttachmentSession()
                    updateMediaStatus(MediaStatus.CANCELED)
                    removeJob()
                    latch.countDown()
                }

        latch.await()
        return result
    }

    fun updateMediaStatus(status: MediaStatus) {
        doAsync {
            messageDao.updateMediaStatus(status.name, message.id)
        }
    }

    private fun removeAttachmentSession() {
        doAsync {
            attachmentSessionDao.deleteAttachmentSession(message.id)
        }
    }

    override fun onAdded() {
        messageDao.insertMessage(message)
        messageDao.updateMediaStatus(MediaStatus.PENDING.name, message.id)
    }

    override fun onCancel(p0: Int, p1: Throwable?) {

    }

    enum class MediaResult {
        SUCCESS, CANCEL, PAUSE,
    }
}