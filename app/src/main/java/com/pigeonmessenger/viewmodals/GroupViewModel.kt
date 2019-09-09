package com.pigeonmessenger.viewmodals

import androidx.lifecycle.ViewModel
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.request.ConversationRequest
import com.pigeonmessenger.api.request.ParticipantRequest
import com.pigeonmessenger.database.room.daos.ContactsDao
import com.pigeonmessenger.database.room.daos.ConversationDao
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.database.room.entities.ConversationCategory
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.job.ConversationJob
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_CREATE
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_EXIT
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_MAKE_ADMIN
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.repo.ContactsRepo
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import com.pigeonmessenger.vo.ConversationBuilder
import com.pigeonmessenger.vo.Participant
import com.pigeonmessenger.vo.ParticipantRole
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class GroupViewModel
internal constructor(

) : ViewModel() {

    @Inject
    lateinit var userRepository: ContactsRepo

    @Inject
    lateinit var conversationRepository: ConversationRepo

    @Inject
    lateinit var contactsDao: ContactsDao

    @Inject
    lateinit var jobManager: PigeonJobManager

    @Inject
    lateinit var conversationDao: ConversationDao

    fun getFriends() = userRepository.getContacts()

    init {
        App.get().appComponent.inject(this)
    }

    fun createGroupConversation(
            conversationId: String,
            groupName: String,
            announcement: String,
            thumb: String?,
            iconBase64: String?,
            iconUrl: String?,
            users: List<User>,
            sender: User
    ): Conversation {
        val createdAt = nowInUtc()
        val conversation = ConversationBuilder(conversationId, createdAt, 0)
                .setCategory(ConversationCategory.GROUP.name)
                .setName(groupName)
                .setThumbnail(thumb)
                .setIconUrl(iconUrl)
                .setAnnouncement(announcement)
                .setOwnerId(sender.userId)
                .setUnseenMessageCount(0)
                .build()
        val mutableList = mutableListOf<Participant>()

        users.mapTo(mutableList) {
            val role = if (it.userId == Session.getUserId()) ParticipantRole.OWNER.name else ""
            Participant(conversationId, it.userId, role, createdAt)
        }

        if (mutableList.find { it.userId == Session.getUserId() } == null) {
            mutableList.add(Participant(conversationId, Session.getUserId(), ParticipantRole.OWNER.name, createdAt))
        }
        conversationRepository.insertConversation(conversation, mutableList)

        val participantRequestList = mutableListOf<ParticipantRequest>()

        mutableList.mapTo(participantRequestList) { ParticipantRequest(it.userId, it.role) }
        val request = ConversationRequest(conversationId, ConversationCategory.GROUP.name,
                groupName, iconBase64, thumb, announcement, participants = participantRequestList, createdAt = nowInUtc())
        jobManager.addJobInBackground(ConversationJob(request, type = TYPE_CREATE))

        return conversation
    }

    fun getConversationStatusById(id: String) = conversationRepository.getConversationById(id)

    /**
     * @param type only support 2 types
     * @see ConversationJob.TYPE_ADD
     * @see ConversationJob.TYPE_REMOVE
     */
    fun modifyGroupMembers(conversationId: String, users: List<User>, type: Int) {
        startGroupJob(conversationId, users, type)
    }


    fun getGroupParticipantsLiveData(conversationId: String) =
            conversationRepository.getGroupParticipantsLiveData(conversationId)

    fun getConversationById(conversationId: String) =
            conversationRepository.getConversationById(conversationId)


    fun findSelf() = userRepository.findSelf()

    //  fun updateGroup(conversationId: String, announcement: String): Observable<PigeonResponse<ConversationResponse>> {
    ///    val request = ConversationRequest(conversationId, name = null,
    //       iconBase64 = null, announcement = announcement)
    //  return conversationRepository.updateAsync(conversationId, request)
    //    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    //  }

    fun makeAdmin(conversationId: String, user: User) {
        startGroupJob(conversationId, listOf(user), TYPE_MAKE_ADMIN, "ADMIN")
    }

    private fun startGroupJob(conversationId: String, users: List<User>, type: Int, role: String = "") {
        val participantRequests = mutableListOf<ParticipantRequest>()
        users.mapTo(participantRequests) {
            ParticipantRequest(it.userId, role, nowInUtc())
        }
        jobManager.addJobInBackground(ConversationJob(conversationId = conversationId,
                participantRequests = participantRequests, type = type))
    }

    fun getRealParticipants(conversationId: String) = conversationRepository.getRealParticipants(conversationId)

    fun exitGroup(conversationId: String) {
        jobManager.addJobInBackground(ConversationJob(conversationId = conversationId, type = TYPE_EXIT))
    }

    fun deleteMessageByConversationId(conversationId: String) {
        conversationRepository.deleteConversation(conversationId)
    }

    fun updateGroup(conversationId: String, conversationRequest: ConversationRequest) {
        jobManager.addJobInBackground(ConversationJob(conversationRequest, conversationId = conversationId, type = ConversationJob.TYPE_UPDATE))
    }

    fun mute(conversationId: String, duration: String?) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            conversationDao.updateGroupDuration(conversationId, duration)
        }
    }

    fun updateAnnouncement(conversationId: String, announcement: String?) {
        announcement?.let {
            //conversationRepository.updateAnnouncement(conversationId, announcement)
        }
    }


    fun clearConversationById(conversationId: String) {
        conversationRepository.clearConversation(conversationId)
    }

    fun findUser(userId: String) = contactsDao.findUser(userId)

    fun updateIconThumb(conversationId: String, thumb: String?) {
        conversationRepository.updateGroupIcon(conversationId, thumb)
    }


}