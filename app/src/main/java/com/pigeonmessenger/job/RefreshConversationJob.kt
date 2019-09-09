package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.Session
import com.pigeonmessenger.database.room.entities.ConversationCategory
import com.pigeonmessenger.database.room.entities.ConversationStatus
import com.pigeonmessenger.extension.insertConversation
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.extension.putBoolean
import com.pigeonmessenger.extension.sharedPreferences
import com.pigeonmessenger.vo.ConversationBuilder
import com.pigeonmessenger.vo.Participant
import com.pigeonmessenger.vo.ParticipantRole

class RefreshConversationJob(val conversationId: String)
    : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy("refresh_conversation")
    .requireNetwork().persist(), conversationId) {

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "shouldReRunOnThrowable: ${p0}");
        return RetryConstraint.RETRY
    }

    override fun cancel() {
    }

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshConversationJob"
        const val PREFERENCES_CONVERSATION = "preferences_conversation"
        const val TAG ="RefreshConversationJob"
    }

    override fun onAdded() {
        jobManager.saveJob(this)
    }

    override fun onRun() {
        val response = conversationApi.getConversation(conversationId).execute()

        if (response.isSuccessful) {
            response.body()?.let { data ->

                Log.d(TAG, "RefreshConversationJob(): ${data}");
                val ownerId: String = data.creatorId

                var c = conversationDao.findConversationById(data.conversationId)
                if (c == null) {
                    Log.d(TAG, " conversation null ");
                    val builder = ConversationBuilder(data.conversationId,
                        data.createdAt, ConversationStatus.SUCCESS.ordinal)
                    c = builder.setOwnerId(ownerId)
                        .setCategory(data.category)
                        .setName(data.name)
                        .setAnnouncement(data.announcement)
                            .build()
                    if (c.announcement.isNullOrBlank()) {
                        //RxBus.publish(GroupEvent(data.conversationId))
                        applicationContext.sharedPreferences(PREFERENCES_CONVERSATION)
                            .putBoolean(data.conversationId, true)
                    }
                    Log.d(TAG, "inserted: ");
                    conversationDao.insertConversation(c)
                } else {
                    Log.d(TAG, "check status: ");
                    val status = if (data.participants.find { Session.getUserId() == it.userId } != null) {
                        ConversationStatus.SUCCESS.ordinal
                    } else {
                        ConversationStatus.QUIT.ordinal
                    }
                    Log.d(TAG, "Status ${status}: ");
                    conversationDao.updateConversation(data.conversationId, ownerId, ConversationCategory.GROUP.name, data.name,
                        data.thumbnail,data.announcement, data.muteUntil, data.createdAt, status)

                }

                Log.d(TAG, "Updated : ");

                val participants = mutableListOf<Participant>()
                val userIdList = mutableListOf<String>()
                for (p in data.participants) {
                    val item = Participant(conversationId, p.userId, p.role, nowInUtc())
                    if (p.role == ParticipantRole.OWNER.name) {
                        participants.add(0, item)
                    } else {
                        participants.add(item)
                    }

                    val u = contactDao.findUser(p.userId)
                    if (u == null) {
                        userIdList.add(p.userId)
                    }
                }
                val local = participantDao.getRealParticipants(data.conversationId)

                val remoteIds = participants.map { it.userId }
                val needRemove = local.filter { !remoteIds.contains(it.userId) }
                Log.d(TAG, "needRemove: $needRemove ");
                if (needRemove.isNotEmpty()) {
                    participantDao.deleteList(needRemove)
                }
                participantDao.insertList(participants)
                Log.d(TAG, " userIdList ${userIdList}: ");
                if (userIdList.isNotEmpty()) {
                    jobManager.addJobInBackground(RefreshUsersJob(userIdList))
                }
            }
        }else{
            Log.d(TAG, "not success ");
        }

        removeJob()
    }

}