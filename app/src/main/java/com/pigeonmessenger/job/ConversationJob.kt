package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.api.request.ConversationRequest
import com.pigeonmessenger.api.request.ParticipantAction
import com.pigeonmessenger.api.request.ParticipantRequest
import com.pigeonmessenger.api.response.ConversationResponse
import com.pigeonmessenger.database.room.entities.ConversationStatus
import com.pigeonmessenger.events.ConversationEvent
import com.pigeonmessenger.extension.getGroupAvatarPath
import com.pigeonmessenger.extension.save
import com.pigeonmessenger.extension.toBitmap
import com.pigeonmessenger.utils.ErrorHandler
import retrofit2.Response
import timber.log.Timber
import java.util.*

class ConversationJob(
        private val request: ConversationRequest? = null,
        private val conversationId: String? = null,
        private val participantRequests: List<ParticipantRequest>? = null,
        private val type: Int,
        private val recipientId: String? = null
) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy(GROUP), UUID.randomUUID().toString()) {

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG,"shouldReRunOnThrowable"+p0 )
        return RetryConstraint.CANCEL
    }

    companion object {
        const val GROUP = "ConversationJob"
        private const val serialVersionUID = 1L
        private val TAG = "ConversationJob"
        const val TYPE_CREATE = 0
        const val TYPE_ADD = 1
        const val TYPE_REMOVE = 2
        const val TYPE_UPDATE = 3
        const val TYPE_MAKE_ADMIN = 4
        const val TYPE_EXIT = 5
        const val TYPE_DELETE = 6
        const val TYPE_MUTE = 7
    }

    override fun onRun() {
        createGroup()
    }

    private fun createGroup() {
        try {
            val response = when (type) {
                TYPE_CREATE ->conversationApi.create(request!!).execute()
                TYPE_ADD ->
                    conversationApi.participants(conversationId!!, ParticipantAction.ADD.name, participantRequests!!).execute()
                TYPE_REMOVE ->
                    conversationApi.participants(conversationId!!, ParticipantAction.REMOVE.name, participantRequests!!)
                            .execute()
                TYPE_UPDATE ->
                    conversationApi.update(conversationId!!, request!!).execute()
                TYPE_MAKE_ADMIN ->
                    conversationApi.participants(conversationId!!, ParticipantAction.ROLE.name, participantRequests!!)
                            .execute()
                TYPE_EXIT ->
                    conversationApi.exit(conversationId!!).execute()
                TYPE_MUTE ->
                    conversationApi.mute(request!!.conversationId, request).execute()
                else -> null
            };
            handleResult(response)
        } catch (e: Exception) {
            Log.d(TAG, ":$e ");
            if (type != TYPE_CREATE || type != TYPE_MUTE) {
                RxBus.publish(ConversationEvent(type, false))
                ErrorHandler.handleError(e)
            }
            Timber.e(e)
        }
    }

    private fun handleResult(r: Response<ConversationResponse>?) {
        if (r != null && r.isSuccessful) {
            if (type == TYPE_CREATE) {
                Log.d(TAG, "updating group success ");
                conversationRepo.updateConversationStatus(request!!.conversationId, ConversationStatus.SUCCESS.ordinal)
                //  conversationDao.insertConversation(conversation)
                Log.d(TAG, "updated ");
                // val participants = mutableListOf<Participant>()
                ///  cr.participants.mapTo(participants) { Participant(cr.conversationId, it.userId, it.role, cr.createdAt) }
                //    participantDao.insertList(participants)
                //  jobManager.addJobInBackground(GenerateAvatarJob(cr.conversationId))
            } else if (type == TYPE_MUTE) {
                // if (cr.category == ConversationCategory.UNREGISTERED_CONTACT.name) {
                //  conversationId?.let { userDao.updateDuration(it, cr.muteUntil) }
                // } else {
                // conversationId?.let { conversationDao.updateGroupDuration(it, cr.muteUntil) }
                //  }
            } else if (type == TYPE_UPDATE) {
                request!!.iconBase64?.let {
                    val iconUrl = applicationContext.getGroupAvatarPath(request.conversationId)
                    it.toBitmap()?.save(iconUrl)
                    conversationRepo.updateGroupIcon(conversationId!!, request.thumbnail)
                }
                RxBus.publish(ConversationEvent(type, true))
            } else {
                RxBus.publish(ConversationEvent(type, true))
            }
        } else {
            if (type != TYPE_CREATE || type != TYPE_MUTE) {
                RxBus.publish(ConversationEvent(type, false))
            } else if (type == TYPE_CREATE) {
                request?.let {
                    conversationRepo.updateConversationStatus(request.conversationId,
                            ConversationStatus.FAILURE.ordinal)
                }
            }
            if (r?.isSuccessful == false) {
                // ErrorHandler.handleMixinError(r.errorCode)
            }
        }
    }

    override fun cancel() {
    }
}