package com.pigeonmessenger.job

import android.net.Uri
import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.events.ProgressEvent
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.vo.MediaStatus
import com.pigeonmessenger.vo.MessageEntity
import com.pigeonmessenger.widget.gallery.MimeType
import java.io.File
import java.util.concurrent.CountDownLatch

class AttachmentDownloadJob(var message: MessageEntity) : BaseJob(Params(PRIORITY_SEND_ATTACHMENT_MESSAGE)
        .addTags(GROUP).groupBy("attachment_download").requireNetwork().persist(), message.id) {

    companion object {
        private val TAG = AttachmentDownloadJob::class.java.simpleName
        const val GROUP = "AttachmentDownloadJob"
        private const val serialVersionUID = 1L
    }

    @Transient
    private var fileDownloadTask: FileDownloadTask? = null

    override fun onAdded() {
        messageDao.updateMediaStatus(MediaStatus.PENDING.name, message.id)
        RxBus.publish(ProgressEvent(message.id, 0f))
    }

    override fun cancel() {
        fileDownloadTask?.pause()
    }

    override fun onRun() {
        val downloadFile = getFilePath()?:return
        jobManager.saveJob(this)
        download(downloadFile)
        when{
            fileDownloadTask!!.isPaused->{
                messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
            }
            fileDownloadTask!!.isCanceled->{
                messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
            }
            fileDownloadTask!!.isComplete->{
                Log.d(TAG, "isComplete: ");
                val f = Uri.fromFile(downloadFile).toString()
                messageDao.updateMediaUrl(f,message.id)
                messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            }
            fileDownloadTask!!.isSuccessful->{
                Log.d(TAG, "isSuccessful: ");
                val f = Uri.fromFile(downloadFile).toString()
                messageDao.updateMediaUrl(f,message.id)
                messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            }
        }
        removeJob()
    }


    private fun getFilePath(): File? {
        return if (message.type.endsWith("_IMAGE")) {
            if (message.mediaMimeType?.isImageSupport() == true) {
                when {
                    message.mediaMimeType.equals(MimeType.PNG.toString(), true) -> {
                        App.get().getImagePath().createImageTemp("REC", ".png")
                    }
                    message.mediaMimeType.equals(MimeType.GIF.toString(), true) -> {
                        App.get().getImagePath().createGifTemp()
                    }
                    message.mediaMimeType.equals(MimeType.WEBP.toString(), true) -> {
                        App.get().getImagePath().createWebpTemp()
                    }
                    else -> {
                        App.get().getImagePath().createImageTemp("REC", ".jpg")
                    }
                }
            } else null
        } else if (message.type.endsWith("_DATA")) {
            val extensionName = message.name?.getExtensionName()
            App.get().getDocumentPath().createDocumentTemp(extensionName)
        } else if (message.type.endsWith("_VIDEO")) {
            val extensionName = message.name?.getExtensionName().let {
                it ?: "mp4"
            }
            App.get().getVideoPath().createVideoTemp(extensionName)


        } else if (message.type.endsWith("_AUDIO")) {

            App.get().getAudioPath().createAudioTemp("ogg")
            // messageDao.updateMediaMessageUrl(Uri.fromFile(imageFile).toString(), message.id)
            //  messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            //  messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
            //   messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
        } else null
    }

    private fun download(downloadFile:File) {

        val countDownLatch = CountDownLatch(1)

        fileDownloadTask = FirebaseStorage.getInstance().reference.child("attachment").
                child(message.mediaKey!!).getFile(downloadFile)

        fileDownloadTask!!
                .addOnSuccessListener {
                    Log.d(TAG, ":addOnSuccessListener ");
                    countDownLatch.countDown()
                }.addOnPausedListener {
                    countDownLatch.countDown()
                }.addOnFailureListener {
                    countDownLatch.countDown()
                }.addOnCanceledListener {
                    countDownLatch.countDown()
                }.addOnProgressListener {
                    RxBus.publish(ProgressEvent(message.id, it.totalByteCount.toFloat() / it.totalByteCount.toFloat()))
                }.addOnCompleteListener {
                    countDownLatch.countDown()
                    Log.d(TAG, ":addOnCompleteListener ");
                }

        countDownLatch.await()

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
        return RetryConstraint.CANCEL
    }


}