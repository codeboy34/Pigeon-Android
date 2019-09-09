package com.pigeonmessenger.repo

import com.pigeonmessenger.database.room.daos.ConversationDao
import com.pigeonmessenger.database.room.daos.MessageDao
import com.pigeonmessenger.database.room.daos.ParticipantDao
import com.pigeonmessenger.database.room.dbs.MessageRoomDatabase
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.database.room.entities.SearchMessageItem
import com.pigeonmessenger.extension.batchMarkReadAndTake
import com.pigeonmessenger.extension.insertConversation
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import com.pigeonmessenger.vo.ConversationItemMinimal
import com.pigeonmessenger.vo.MessageEntity
import com.pigeonmessenger.vo.Participant
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConversationRepo(private var messageDao: MessageDao,
                       private var conversationDao: ConversationDao,
                       private var participantDao: ParticipantDao,
                       private var appDatabase: MessageRoomDatabase) {


    fun getMessages(conversationId: String) = messageDao.getMessage(conversationId)


    fun updateMediaStatusStatus(status: String, id: String) {
        messageDao.updateMediaStatus(status, id)
    }


    fun findMessageById(messageId: String): MessageEntity? {
        return messageDao.findMessageById(messageId)
    }

    fun getMediaMessages(str: String): List<MessageEntity> {
        return messageDao.getMediaMessages()
    }

    fun indexUnreadCount(recipientId: String) = conversationDao.indexUnreadCount(recipientId)

    fun updateLastMessageId(recipientId: String, messageId: String) {
        conversationDao.updateLastMessageId(recipientId, messageId)
    }

    fun createConversation(conversation: Conversation) {
        conversationDao.insert(conversation)
    }

    fun conversationList() = conversationDao.conversationList()

    fun findConversation(recipientId: String) = conversationDao.findConversationById(recipientId)

    fun getConversation(conversationId: String) = conversationDao.getConversationById(conversationId)

    fun getUnreadMessage(recipientId: String) = messageDao.getUnreadMessages(recipientId)

    fun batchMarkReadAndTake(conversationId: String, created_at: String) {
        messageDao.batchMarkReadAndTake(conversationId, created_at)
    }

    fun deleteConversation(recipientId: String) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            conversationDao.deleteConversationById(recipientId)
        }
    }


    fun updateConversationPinTimeById(conversationId: String, pinTime: String?) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            conversationDao.updateConversationPinTimeById(conversationId, pinTime)
        }
    }

    fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    fun fuzzySearchMessage(query: String): List<SearchMessageItem> = messageDao.fuzzySearchMessage(query)

    fun fuzzySearchGroup(query: String): List<ConversationItemMinimal> = conversationDao.fuzzySearchGroup(query)


    fun insertConversation(conversation: Conversation, participants: List<Participant>) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            appDatabase.runInTransaction {
                conversationDao.insertConversation(conversation)
                participantDao.insertList(participants)
            }
        }
    }

    fun getConversationById(id: String) = conversationDao.getConversationById(id)

    fun findUnreadMessagesSync(conversationId: String) = messageDao.findUnreadMessagesSync(conversationId)


    fun updateConversationStatus(conversationId: String, status: Int) {
        conversationDao.updateConversationStatusById(conversationId, status)
    }

    fun getConversationStorageUsage() = conversationDao.getConversationStorageUsage()


    fun getGroupParticipantsLiveData(conversationId: String) =
            participantDao.getGroupParticipantsLiveData(conversationId)


    fun getRealParticipants(conversationId: String) = participantDao.getRealParticipants(conversationId)


    fun clearConversation(conversationId: String) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            messageDao.clearConversation(conversationId)
        }
    }

    fun getConversationIdIfExistsSync(recipientId: String) = conversationDao.getConversationIdIfExistsSync(recipientId)

    fun updateGroupIcon(conversationId: String, thumb: String?) {
        conversationDao.updateGroupIcon(conversationId, thumb)
    }

    fun updateGroupName(conversationId: String, name: String) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            conversationDao.updateGroupName(conversationId, name)
        }
    }

}
