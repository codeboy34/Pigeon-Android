package com.pigeonmessenger.viewmodals

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.collection.ArraySet
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.gson.Gson
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.database.room.entities.*
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.job.*
import com.pigeonmessenger.manager.SocketManager
import com.pigeonmessenger.repo.ContactsRepo
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.Attachment
import com.pigeonmessenger.utils.Constant.PAGE_SIZE
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import com.pigeonmessenger.utils.image.Compressor
import com.pigeonmessenger.utils.video.MediaController
import com.pigeonmessenger.vo.*
import com.pigeonmessenger.widget.gallery.MimeType
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.inject.Inject

class MessageViewModal(app: Application) : AndroidViewModel(app) {

    init {
        App.get().appComponent.inject(this)
    }


    private val notificationManager: NotificationManager by lazy {
        App.get().getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }


    private var TAG = "MessageViewModal"

    @Inject
    lateinit var conversationRepo: ConversationRepo

    @Inject
    lateinit var jobManager: PigeonJobManager

    @Inject
    lateinit var contactsRepo: ContactsRepo

    @Inject
    lateinit var socketManager: SocketManager


    // fun getMessages():LiveData<List<MessageEntity>> = conversationRepo.getMessages()

    fun getMessages(id: String, initialLoadKey: Int = 0): LiveData<PagedList<MessageItem>> {
        return LivePagedListBuilder(conversationRepo.getMessages(id), PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE * 2)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build())
                .setInitialLoadKey(initialLoadKey)
                .build()
    }

    fun sendMessage(msg: MessageEntity) {
        //   jobManager.addJobInBackground(TestJob())
        jobManager.addJobInBackground(SendMessageJob(msg))
    }

    fun sendImageMessage(conversationId: String, senderId: String,
                         uri: Uri): Flowable<Int>? {

        val category = MessageCategory.SIGNAL_IMAGE.name
        val mimeType = getMimeType(uri)
        if (mimeType?.isImageSupport() != true) {
            App.get().toast(R.string.error_format)
            return null
        }
        if (mimeType == MimeType.GIF.toString()) {
            return Flowable.just(uri).map {
                val gifFile = App.get().getImagePath().createGifTemp()
                gifFile.copyFromInputStream(FileInputStream(uri.getFilePath(App.get())))
                val size = getImageSize(gifFile)
                val thumbnail = gifFile.blurThumbnail(size)?.bitmap2String(mimeType)

                val message = createMediaMessage(UUID.randomUUID().toString(),
                        conversationId, senderId, category, nowInUtc(), null, Uri.fromFile(gifFile).toString(),
                        mimeType, gifFile.length(), size.width, size.height, thumbnail,
                        MediaStatus.PENDING, MessageStatus.SENDING, randomMediaKey())
                jobManager.addJobInBackground(SendAttachmentJob(message))
                return@map -0
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }

        val temp = App.get().getImagePath().createImageTemp(type = ".jpg")

        return Compressor()
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .compressToFileAsFlowable(File(uri.getFilePath(App.get())), temp.absolutePath)
                .map { imageFile ->
                    val imageUrl = Uri.fromFile(temp).toString()
                    val length = imageFile.length()
                    if (length <= 0) {
                        return@map -1
                    } else {
                    }

                    val size = getImageSize(imageFile)
                    val thumbnail = imageFile.blurThumbnail(size)?.bitmap2String(mimeType)
                    val message = createMediaMessage(UUID.randomUUID().toString(),
                            conversationId, senderId, category, nowInUtc(), null, imageUrl,
                            MimeType.JPEG.toString(), length, size.width, size.height, thumbnail,
                            MediaStatus.PENDING, MessageStatus.SENDING, randomMediaKey())


                    jobManager.addJobInBackground(SendAttachmentJob(message))
                    return@map -0
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun indexUnreadCount(recipientid: String) = conversationRepo.indexUnreadCount(recipientid)

    fun sendTypingMessage(conversationId: String, recipientId: String?) {
        jobManager.addJobInBackground(TypingJob(conversationId, recipientId))
    }

    fun sendVideoMessage(conversationId: String, senderId: String, uri: Uri) =
            Flowable.just(uri).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).map {
                val video = App.get().getVideoModel(it)!!
                val category = MessageCategory.SIGNAL_VIDEO.name
                val mimeType = getMimeType(uri)
                if (mimeType != "video/mp4") {
                    video.needChange = true
                }
                if (!video.fileName.endsWith(".mp4")) {
                    video.fileName = "${video.fileName.getFileNameNoEx()}.mp4"
                }
                val videoFile = App.get().getVideoPath().createVideoTemp("mp4")

                MediaController().convertVideo(video.originalPath, video.bitrate, video.resultWidth, video.resultHeight, video
                        .originalWidth, video
                        .originalHeight, videoFile, video.needChange)

                val message = createVideoMessage(UUID.randomUUID().toString(), conversationId, senderId,
                        category, null, randomMediaKey(),
                        Uri.fromFile(videoFile).toString(),
                        video.duration, video
                        .resultWidth,
                        video.resultHeight, video.thumbnail,
                        "video/mp4",
                        videoFile.length(), nowInUtc(), MediaStatus.PENDING, MessageStatus.SENDING)
                jobManager.addJobInBackground(SendAttachmentJob(message))
            }.observeOn(AndroidSchedulers.mainThread())!!


    fun sendAudioMessage(conversationId: String, senderId: String, file: File, duration: Long, waveForm: ByteArray) {
        val category = MessageCategory.SIGNAL_AUDIO.name
        val message = createAudioMessage(UUID.randomUUID().toString(), conversationId, senderId, category,
                file.length(), Uri.fromFile(file).toString(), randomMediaKey(), duration.toString(), nowInUtc(), waveForm,
                MediaStatus.PENDING, MessageStatus.SENDING)
        jobManager.addJobInBackground(SendAttachmentJob(message))
    }

    fun cancel(id: String) {
        doAsync {
            notNullElse(jobManager.findJobById(id), {
                it.cancel()
            }, {
                conversationRepo.updateMediaStatusStatus(MediaStatus.CANCELED.name, id)
            })
        }
    }

    fun retryDownload(messageId: String) {
        doAsync {
            conversationRepo.findMessageById(messageId)?.let {
                jobManager.addJobInBackground(AttachmentDownloadJob(it))
            }
        }
    }

    fun retryUpload(messageId: String) {
        doAsync {
            conversationRepo.findMessageById(messageId)?.let {
                jobManager.addJobInBackground(SendAttachmentJob(it))
            }
        }
    }

    fun sendAttachmentMessage(conversationId: String, senderId: String, attachment: Attachment) {
        val category = MessageCategory.SIGNAL_DATA.name
        val message = createAttachmentMessage(UUID.randomUUID().toString(), conversationId, senderId, category,
                null, attachment.filename, randomMediaKey(), attachment.uri.toString(),
                attachment.mimeType, attachment.fileSize, nowInUtc(), MediaStatus.PENDING, MessageStatus.SENDING)
        jobManager.addJobInBackground(SendAttachmentJob(message))
    }

    fun sendReplyMessage(conversationId: String, senderId: String, content: String, replyMessage: MessageItem) {
        val category = MessageCategory.SIGNAL_TEXT.name
        val message = createReplyMessage(UUID.randomUUID().toString(), conversationId,
                senderId, category, content.trim(), nowInUtc(), MessageStatus.SENDING,
                replyMessage.id, Gson().toJson(QuoteMessageItem(replyMessage)))
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    private fun getConversationIdIfExistsSync(recipientId: String) = conversationRepo.getConversationIdIfExistsSync(recipientId)
    private fun findUnreadMessagesSync(conversationId: String) = conversationRepo.findUnreadMessagesSync(conversationId)

    fun sendForwardMessages(selectItem: List<Any>, messages: List<ForwardMessage>?) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            var conversationId: String? = null
            for (item in selectItem) {
                if (item is User) {
                    conversationId = getConversationIdIfExistsSync(item.userId)
                    if (conversationId == null) {
                        conversationId = generateConversationId(Session.getUserId()!!, item.userId)
                        initConversation(conversationId, item.userId)
                    }
                    sendForwardMessages(conversationId, messages)
                } else if (item is ConversationItem) {
                    conversationId = item.conversationId
                    sendForwardMessages(item.conversationId, messages)
                }

                App.get().mainThread {
                    App.get().toast(R.string.forward_success)
                }
                findUnreadMessagesSync(conversationId!!)?.let {
                    if (it.isNotEmpty()) {
                        conversationRepo.batchMarkReadAndTake(conversationId, it.last().createdAt)
                        it.map { AckMessage(it.id, MessageStatus.READ.name) }.let {
                            val recipientId = conversationRepo.findConversation(conversationId)!!.ownerId
                            it.chunked(100).forEach {
                                val ackBlaze = createAcksMessage(UUID.randomUUID().toString(), Session.getUserId(), conversationId, recipientId, it, nowInUtc())
                                jobManager.addJobInBackground(AckSendJob(ackBlaze))
                            }
                        }
                    }
                }
            }
        }
    }


    private fun sendForwardMessages(conversationId: String, messages: List<ForwardMessage>?) {
        messages?.let {
            val senderId = Session.getUserId()
            for (item in it) {
                if (item.id != null) {
                    sendFordMessage(conversationId, senderId, item.id).subscribe({}, {
                        Timber.e("")
                    })
                } else {
                    when (item.type) {
                        ForwardCategory.CONTACT.name -> {
                            // sendContactMessage(item.sharedUserId!!, senderId, item.sharedUserId, isPlainMessage)
                        }
                        ForwardCategory.IMAGE.name -> {
                            sendImageMessage(conversationId, senderId, Uri.parse(item.mediaUrl))
                                    ?.subscribe({
                                    }, {
                                        Timber.e(it)
                                    })
                        }
                        ForwardCategory.DATA.name -> {
                            App.get().getAttachment(Uri.parse(item.mediaUrl))?.let {
                                sendAttachmentMessage(conversationId, senderId, it)
                            }
                        }
                        ForwardCategory.VIDEO.name -> {
                            sendVideoMessage(conversationId, senderId,
                                    Uri.parse(item.mediaUrl))
                                    .subscribe({
                                    }, {
                                        Timber.e(it)
                                    })
                        }
                        ForwardCategory.TEXT.name -> {
                            item.content?.let {
                                sendMessage(createMessage(UUID.randomUUID().toString(), conversationId, senderId,
                                        it, nowInUtc(), MessageCategory.SIGNAL_TEXT.name))
                            }
                        }
                    }
                }
            }
        }
    }


    fun sendFordMessage(conversationId: String, senderId: String, id: String) =
            Flowable.just(id).observeOn(Schedulers.io()).map {
                conversationRepo.findMessageById(id)?.let { message ->
                    when {
                        message.type.endsWith("_IMAGE") -> {
                            val category = MessageCategory.SIGNAL_IMAGE.name
                            if (message.mediaUrl?.fileExists() != true) {
                                return@let 0
                            }
                            jobManager.addJobInBackground(SendAttachmentJob(createMediaMessage(UUID.randomUUID().toString(),
                                    conversationId, senderId, category, nowInUtc(), null, message.mediaUrl, message.mediaMimeType!!, message.mediaSize!!,
                                    message.mediaWidth, message.mediaHeight, message.thumbImage,
                                    MediaStatus.PENDING, MessageStatus.SENDING, randomMediaKey())))
                        }
                        message.type.endsWith("_VIDEO") -> {
                            val category = MessageCategory.SIGNAL_VIDEO.name
                            if (message.mediaUrl?.fileExists() != true) {
                                return@let 0
                            }
                            jobManager.addJobInBackground(SendAttachmentJob(createVideoMessage(UUID.randomUUID().toString(),
                                    conversationId, senderId, category, null, randomMediaKey(), message.mediaUrl,
                                    message.mediaDuration?.toLong(), message.mediaWidth, message.mediaHeight, message.thumbImage,
                                    message.mediaMimeType!!, message.mediaSize!!, nowInUtc(),
                                    MediaStatus.PENDING, MessageStatus.SENDING
                            )))
                        }
                        message.type.endsWith("_DATA") -> {
                            val category = MessageCategory.SIGNAL_DATA.name
                            val uri = if (message.senderId == Session.getUserId()) {
                                if (message.mediaUrl?.fileExists() != true) {
                                    return@let 0
                                }
                                message.mediaUrl
                            } else {
                                val file = File(message.mediaUrl).apply {
                                    if (!this.exists()) {
                                        return@let 0
                                    }
                                }
                                file.toUri().toString()
                            }

                            jobManager.addJobInBackground(SendAttachmentJob(createAttachmentMessage(UUID.randomUUID().toString(), conversationId, senderId,
                                    category, null, message.name, randomMediaKey(),
                                    uri, message.mediaMimeType!!, message.mediaSize!!, nowInUtc(), MediaStatus.PENDING, MessageStatus.SENDING)))
                        }
                        message.type.endsWith("_AUDIO") -> {
                            val category = MessageCategory.SIGNAL_AUDIO.name
                            if (message.mediaUrl?.fileExists() != true) {
                                return@let 0
                            }
                            jobManager.addJobInBackground(SendAttachmentJob(createAudioMessage(UUID.randomUUID().toString(), conversationId, senderId,
                                    category, message.mediaSize!!, message.mediaUrl, randomMediaKey(), message.mediaDuration!!, nowInUtc(), message.mediaWaveform!!,
                                    MediaStatus.PENDING, MessageStatus.SENDING)))
                        }
                    }
                    return@let 1
                }
            }.observeOn(AndroidSchedulers.mainThread())!!


    fun initConversation(conversationId: String, ownerId: String) {
        var conversation = conversationRepo.findConversation(ownerId)
        if (conversation == null) {
            conversation = createConversation(conversationId, ConversationCategory.CONTACT.name, ownerId,
                    ConversationStatus.SUCCESS.ordinal)
            conversationRepo.createConversation(conversation)
        }
    }

    fun conversationList() = conversationRepo.conversationList()
    fun getFriends() = contactsRepo.getContacts()

    fun findContactEntity(contactId: String) = contactsRepo.getContact(contactId)


    fun markMessageRead(conversationId: String) {

        GlobalScope.launch(SINGLE_DB_THREAD) {
            conversationRepo.getUnreadMessage(conversationId).also { list ->
                if (list.isNotEmpty()) {
                    notificationManager.cancel(conversationId.hashCode())
                    conversationRepo.batchMarkReadAndTake(conversationId, list.last().createdAt)
                    if (isGroup(conversationId)) return@also
                    list.map { AckMessage(it.id, MessageStatus.READ.name) }.let {
                        val recipientId = conversationRepo.findConversation(conversationId)!!.ownerId
                        val ackBlaze = createAcksMessage(UUID.randomUUID().toString(), Session.getUserId(), conversationId, recipientId, it, nowInUtc())
                        jobManager.addJobInBackground(AckSendJob(ackBlaze))
                    }
                }
            }
        }
    }

    fun deleteConversation(recipientId: String) {
        conversationRepo.deleteConversation(recipientId)
    }

    fun updateConversationPinTimeById(conversationId: String, pinTime: String?) {
        conversationRepo.updateConversationPinTimeById(conversationId, pinTime)
    }

    fun deleteMessages(set: ArraySet<MessageItem>) {
        val data = ArraySet(set)
        GlobalScope.launch(SINGLE_DB_THREAD) {
            data.forEach { item ->
                conversationRepo.deleteMessage(item.id)
                jobManager.cancelJobById(item.id)
                notificationManager.cancel(item.id.hashCode())
            }
        }
    }

    fun findConversation(conversationId: String) = conversationRepo.getConversation(conversationId)

    fun getConversationById(conversationId: String) = conversationRepo.findConversation(conversationId)


    fun registerOnlineStatus(recipientId: String) {
        socketManager.registerOnlineStatus(recipientId)
    }

    fun leaveOnlineStatus(recipientId: String) {
        socketManager.leaveOnlieStatus(recipientId)
    }

}