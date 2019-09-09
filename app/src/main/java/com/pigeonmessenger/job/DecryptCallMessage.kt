package com.pigeonmessenger.job

import android.util.Log
import androidx.collection.ArrayMap
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.crypto.Base64
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.di.Injector
import com.pigeonmessenger.extension.createAtToLong
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.vo.*
import com.pigeonmessenger.webrtc.CallService
import kotlinx.coroutines.*
import org.webrtc.IceCandidate
import java.util.*
import java.util.concurrent.Executors

class DecryptCallMessage(private val callState: CallState) : Injector() {

    companion object {
        val TAG = "DecryptCallMessage"
        const val LIST_PENDING_CALL_DELAY = 2000L

        var listPendingOfferHandled = false
    }

    // private val gson = Gson()
    private val listPendingDispatcher by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    private val listPendingJobMap = ArrayMap<String, Pair<Job, MessageEntity>>()

    private val listPendingCandidateMap = ArrayMap<String, ArrayList<IceCandidate>>()

    fun onRun(floodMessage: FloodMessage) {
        val params = gson.fromJson<BlazeMessageParam>(floodMessage.data, BlazeMessageParam::class.java)

        val message = createCallMessage(params.message_id, floodMessage.conversationId, floodMessage.senderId, params.category, params.data,
                floodMessage.createdAt, MessageStatus.DELIVERED, params.quote_message_id, null)

        processWebRTC(message)
       //processCall(message)
    }

    private fun processWebRTC(message: MessageEntity) {

        if (message.type == MessageCategory.WEBRTC_AUDIO_OFFER.name ||
                message.type == MessageCategory.WEBRTC_VIDEO_OFFER.name) {

            val isExpired = try {
            //    Log.d(TAG,"currentMilis ${System.currentTimeMillis()} ${message.createdAt.createAtToLong()}" )
                val offset = nowInUtc().createAtToLong() - message.createdAt.createAtToLong()
            //    Log.d(TAG,"Offset $offset")
                offset > CallService.DEFAULT_TIMEOUT_MINUTES * 58 * 1000
            } catch (e: NumberFormatException) {
           //     Log.e(TAG,"NumberFormatException" ,e)
                true
            }

            if (!isExpired && !listPendingOfferHandled) {
                listPendingJobMap[message.id] = Pair(GlobalScope.launch(listPendingDispatcher) {
                    delay(LIST_PENDING_CALL_DELAY)
                    listPendingOfferHandled = true
                    listPendingJobMap.forEach { entry ->
                        val pair = entry.value
                        val job = pair.first
                        val curData = pair.second
                        if (entry.key != message.id && !job.isCancelled) {
                            job.cancel()
                            val m = createCallMessage(UUID.randomUUID().toString(), curData.senderId, Session.getUserId(),
                                    MessageCategory.WEBRTC_BUSY.name, null, nowInUtc(), MessageStatus.SENDING, curData.id)
                            jobManager.addJobInBackground(SendMessageJob(m))

                            val savedMessage = createCallMessage(curData.id, m.conversationId, curData.senderId, m.type, m.message,
                                    m.createdAt, MessageStatus.DELIVERED, m.quoteMessageId)

                            messageDao.insert(savedMessage)
                            listPendingCandidateMap.remove(curData.id, listPendingCandidateMap[curData.id])
                        }
                    }
                    processCall(message)
                    listPendingJobMap.clear()
                }, message)
            }
            else if (isExpired) {
                val msg = createCallMessage(message.id, message.conversationId, message.senderId, message.type,
                        null, message.createdAt, MessageStatus.DELIVERED)
                Log.d(TAG,"Expired  ${msg}")
                messageDao.insert(msg)
            }else processCall(message)
        } else {
            Log.d(TAG,"NotExpired ${message}")
            processCall(message)
        }
    }

    private fun processCall(message: MessageEntity) {

        val ctx = App.get()

        if (message.type == MessageCategory.WEBRTC_AUDIO_OFFER.name ||
                message.type == MessageCategory.WEBRTC_VIDEO_OFFER.name) {
            syncUser(message.senderId)?.let { user ->
                if (message.type == MessageCategory.WEBRTC_VIDEO_OFFER.name) callState.callType = CallState.CallType.VIDEO
                else callState.callType = CallState.CallType.AUDIO

                val pendingCandidateList = listPendingCandidateMap[message.id]
                if (pendingCandidateList == null || pendingCandidateList.isEmpty()) {
                    CallService.incoming(ctx, user, message.conversationId, message)
                } else {
                    CallService.incoming(ctx, user, message.conversationId, message, gson.toJson(pendingCandidateList.toArray()))
                    pendingCandidateList.clear()
                    listPendingCandidateMap.remove(message.id, pendingCandidateList)
                }
            }
        } else if (listPendingJobMap.containsKey(message.quoteMessageId)) {
            listPendingJobMap[message.quoteMessageId]?.let { pair ->
                if (message.type == MessageCategory.WEBRTC_ICE_CANDIDATE.name) {
                    val json = String(Base64.decode(message.message))
                    val ices = gson.fromJson(json, Array<IceCandidate>::class.java)
                    var list = listPendingCandidateMap[message.quoteMessageId]
                    if (list == null) {
                        list = arrayListOf()
                    }
                    list.addAll(ices)
                    listPendingCandidateMap[message.quoteMessageId] = list
                    return@let
                }

                pair.first.let {
                    if (!it.isCancelled) {
                        it.cancel()
                    }
                }
                listPendingJobMap.remove(message.quoteMessageId)

                messageDao.insert(message)
            }
        } else {
            when (message.type) {
                MessageCategory.WEBRTC_AUDIO_ANSWER.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE ||
                            message.quoteMessageId != callState.callInfo.messageId) {
                        return
                    }
                    CallService.answer(ctx, message)
                }
                MessageCategory.WEBRTC_ICE_CANDIDATE.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE ||
                            message.quoteMessageId != callState.callInfo.messageId) {
                        return
                    }
                    CallService.candidate(ctx, message)
                }
                MessageCategory.WEBRTC_CANCEL.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        //  notifyServer(data)
                        return
                    }
                    saveCallMessage(message)
                    if (message.quoteMessageId != callState.callInfo.messageId) {
                        return
                    }
                    CallService.cancel(ctx)
                }
                MessageCategory.WEBRTC_DECLINE.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        //notifyServer(data)
                        return
                    }

                    val uId = getUserId()
                     saveCallMessage(message, senderId = uId)
                    if (message.quoteMessageId != callState.callInfo.messageId) {
                        return
                    }
                    CallService.decline(ctx)
                }
                MessageCategory.WEBRTC_BUSY.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE ||
                            message.quoteMessageId != callState.callInfo.messageId ||
                            callState.user == null) {
                        return
                    }

                        saveCallMessage(message, senderId = Session.getUserId())
                    CallService.busy(ctx)
                }
                MessageCategory.WEBRTC_END.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        //  notifyServer(data)
                        return
                    }

                    val duration = System.currentTimeMillis() - callState.connectedTime!!
                    val uId = getUserId()
                    saveCallMessage(message, duration = duration.toString(), senderId = uId, status = MessageStatus.READ)
                    CallService.remoteEnd(ctx)
                }
                MessageCategory.WEBRTC_FAILED.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        //  notifyServer(data)
                        return
                    }

                    val uId = getUserId()
                     saveCallMessage(message, senderId =  uId)
                    CallService.remoteFailed(ctx)
                }
            }
            //  notifyServer(data)
        }
    }

    private fun getUserId(): String {
        return if (callState.isInitiator) {
            Session.getUserId()
        } else {
            callState.user!!.userId
        }
    }


    private fun saveCallMessage(
            data: MessageEntity,
            category: String? = null,
            duration: String? = null,
            senderId: String = data.senderId,
            status: MessageStatus = MessageStatus.DELIVERED
    ) {
        if (data.senderId == Session.getUserId()!! ||
                data.quoteMessageId == null) {
            return
        }
        val realCategory = category ?: data.type
        val message = createCallMessage(data.quoteMessageId, data.conversationId, senderId, realCategory,
                null, data.createdAt, status, mediaDuration = duration)
        messageDao.insert(message)
    }

    private fun syncUser(userId: String): User? {
        val u: User? = contactsDao.findUser(userId)
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