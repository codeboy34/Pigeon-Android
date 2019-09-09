package com.pigeonmessenger.extension

import androidx.room.Transaction
import com.pigeonmessenger.database.room.daos.ConversationDao
import com.pigeonmessenger.database.room.daos.FloodMessageDao
import com.pigeonmessenger.database.room.daos.MessageDao
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.vo.FloodMessage
import com.pigeonmessenger.vo.MessageEntity
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async


fun FloodMessageDao.findFloodMessageDeferred(): Deferred<List<FloodMessage>?> = kotlinx.coroutines.GlobalScope.async {
    findFloodMessagesSync()
}


@Transaction
fun MessageDao.insertMessage(messageEntity: MessageEntity){
    insertEntity(messageEntity)
   //// updateLastMessageId(messageEntity.conversationId,messageEntity.id)
}

@Transaction
fun MessageDao.batchMarkReadAndTake(conversationId: String, createdAt: String) {
    batchMarkRead(conversationId,  createdAt)
    takeUnseen(conversationId)
}

@Transaction
fun ConversationDao.insertConversation(conversation: Conversation, action: (() -> Unit)? = null, haveAction: ((Conversation) -> Unit)? = null) {

    val c = findConversationById(conversation.conversationId)
    if (c == null) {
        insert(conversation)
        action?.let { it() }
    } else {
        haveAction?.let { it(c) }
    }
}
