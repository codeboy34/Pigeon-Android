package com.pigeonmessenger.database.room.daos

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.pigeonmessenger.Session
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.database.room.entities.SearchMessageItem
import com.pigeonmessenger.vo.*

@Dao
interface MessageDao : BaseDao<MessageEntity> {

    /*
       @Query("select m.id, m., h.site_name AS siteName,h.site_description as siteDescription," +
            " u.display_name as senderDisplayName, u.full_name as senderFullName," +
            " c.name as groupName,c.owner_id as creatorId," +
            " pu.full_name as participantFullName, pu.display_name as participantDisplayName" +
            " from messages m" +
            " LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink" +
            " LEFT JOIN users u ON m.sender_id=u.user_id" +
            " LEFT JOIN conversations c ON m.conversation_id=c.conversation_id" +
            " LEFT JOIN users pu ON m.participant_id=pu.user_id" +
            " where m.conversation_id=:conversationId order by m.created_at DESC ")
     */

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("select m.id, m.conversation_id as conversationId, m.sender_id as senderId, m.message, m.created_at as createdAt, m.type, " +
            "m.status, m.media_url as mediaUrl, m.media_mime_type as mediaMimeType, m.media_size as mediaSize, m.media_duration as mediaDuration," +
            " m.media_width as mediaWidth, m.media_height as mediaHeight, m.thumb_image as thumbImage, m.media_status as mediaStatus, " +
            " m.media_waveform as mediaWaveform, m.name, m.quote_message_id as quoteMessageId, m.quote_content as quoteContent, m.action," +
            " m.participant_id as participantId, m.hyperlink as hyperlink,   " +
            " h.site_name AS siteName,h.site_description as siteDescription," +
            " u.display_name as senderDisplayName, u.full_name as senderFullName," +
            " c.name as groupName,c.owner_id as creatorId," +
            " pu.full_name as participantFullName, pu.display_name as participantDisplayName" +
            " from messages m" +
            " LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink" +
            " LEFT JOIN users u ON m.sender_id=u.user_id" +
            " LEFT JOIN conversations c ON m.conversation_id=c.conversation_id" +
            " LEFT JOIN users pu ON m.participant_id=pu.user_id" +
            " where m.conversation_id=:conversationId order by m.created_at DESC ")
    fun getMessage(conversationId: String): DataSource.Factory<Int, MessageItem>


    //@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    //@Query("SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId , u.avatar_url AS userAvatarUrl, u.full_name AS userFullName, m.type AS type, u1.avatar_url AS botAvatarUrl,  u1.user_id AS botUserId, m.message AS content, m.created_at AS createdAt, m.name AS mediaName, c.icon_url AS conversationAvatarUrl, c.name AS conversationName, c.category AS conversationCategory FROM messages m INNER JOIN users u ON m.sender_id = u.user_id LEFT JOIN conversations c ON c.conversation_id = m.conversation_id LEFT JOIN users u1 ON c.owner_id = u1.user_id WHERE ((m.type = 'SIGNAL_TEXT' OR m.type = 'PLAIN_TEXT') AND m.status != 'FAILED' AND m.message LIKE :query) OR ((m.type = 'SIGNAL_DATA' OR m.type = 'PLAIN_DATA') AND m.status != 'FAILED' AND m.name LIKE :query) ORDER BY m.created_at DESC LIMIT 200")
//  fun fuzzySearchMessage(query: String): List<SearchMessageItem>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEntity(vararg obj: MessageEntity)

    @Query("update messages set status=:status where id= :msgId ")
    fun updateAck(msgId: String, status: String)

    @Query("UPDATE messages SET media_status = :status WHERE id = :id")
    fun updateMediaStatus(status: String, id: String)

    @Query("UPDATE messages SET media_url=:media where id = :id")
    fun updateMediaUrl(media: String, id: String)

    @Query("select * from messages where id=:messageId")
    fun findMessageById(messageId: String): MessageEntity?

    @Query("select * from messages where `action` ='CREATE' and conversation_id=:conversationId")
    fun findGroupCreateMessage(conversationId: String) : MessageEntity?

    @Query("select * from messages where  type = 'SIGNAL_IMAGE' OR type = 'PLAIN_IMAGE' OR type = 'SIGNAL_VIDEO' OR type = 'PLAIN_VIDEO' ")
    fun getMediaMessages(): List<MessageEntity>

    @Query("SELECT id FROM messages WHERE conversation_id = :conversationId AND sender_id = :userId AND status = 'FAILED' ORDER BY created_at DESC LIMIT 1000")
    fun findFailedMessages(conversationId: String, userId: String): List<String>?


    @Query("select id , created_at from messages where conversation_id = :conversationId and sender_id != :accountId  and status!='READ' and status != 'FAILED' ")
    fun getUnreadMessages(conversationId: String, accountId: String = Session.getUserId()): List<MessageMinimal>

    @Query("update messages set status = 'READ' where conversation_id = :conversationId and created_at <= :createdAt and sender_id != :sessionId ")
    fun batchMarkRead(conversationId: String, createdAt: String, sessionId: String = Session.getUserId())

    @Query("update conversations set unseen_message_count=(SELECT count(1) from messages where conversation_id=:conversationId and sender_id !=:accountId and status !='READ' ) where conversation_id=:conversationId")
    fun takeUnseen(conversationId: String, accountId: String = Session.getUserId())

    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversation_id=:conversationId")
    fun clearConversation(conversationId: String)

    @Query("UPDATE messages SET hyperlink = :hyperlink WHERE id = :id")
    fun updateHyperlink(hyperlink: String, id: String)

    @Query("update conversations set last_message_id = :lastMessageId where conversation_id=:conversationId")
    fun updateLastMessageId(conversationId: String, lastMessageId: String)

    @Query("update messages set message = :plainText , status =:status where id=:messageId")
    fun updateMessageContentAndStatus(plainText: String, status: String, messageId: String)


    @Query("UPDATE messages SET message = :content, media_mime_type = :mediaMimeType, media_size = :mediaSize, media_width = :mediaWidth, media_height = :mediaHeight ,thumb_image = :thumbImage, media_duration = :mediaDuration,media_status = :mediaStatus, status = :status, name = :name, media_waveform = :mediaWaveform WHERE id = :messageId")
    fun updateAttachmentMessage(
            messageId: String,
            content: String,
            mediaMimeType: String,
            mediaSize: Long,
            mediaWidth: Int?,
            mediaHeight: Int?,
            thumbImage: String?,
            name: String?,
            mediaWaveform: ByteArray?,
            mediaDuration: String?,
            mediaStatus: String,
            status: String
    )

    @Query("SELECT count(id) FROM messages WHERE conversation_id = :conversationId AND quote_message_id = :messageId AND quote_content IS NULL")
    fun countMessageByQuoteId(conversationId: String, messageId: String): Int


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, u.full_name AS fullName, u.display_name as displayName, m.type ,m.message, m.created_at AS createdAt, m.name AS mediaName,c.icon_thumbnail AS groupIconThumbnail, c.name AS conversationName, c.category AS conversationCategory FROM messages m LEFT JOIN conversations c ON c.conversation_id = m.conversation_id LEFT JOIN users u ON c.owner_id = u.user_id WHERE ((m.type = 'SIGNAL_TEXT' OR m.type = 'PLAIN_TEXT') AND m.status != 'FAILED' AND m.message LIKE :query) OR ((m.type = 'SIGNAL_DATA' OR m.type = 'PLAIN_DATA') AND m.status != 'FAILED' AND m.name LIKE :query) ORDER BY m.created_at DESC LIMIT 200")
    fun fuzzySearchMessage(query: String): List<SearchMessageItem>


    @Query("SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS senderId , u.full_name AS userFullName, m.type AS type, m.message AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform, m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId, m.quote_content as quoteContent FROM messages m INNER JOIN users u ON m.sender_id = u.user_id WHERE m.conversation_id = :conversationId AND m.id = :quoteMessageId AND m.status != 'FAILED'")
    fun findMessageItemById(conversationId: String, quoteMessageId: String): QuoteMessageItem?

    @Query("DELETE from messages where type != 'SYSTEM_CONVERSATION' AND  `action` != 'CREATE' and `action`='ADD' ")
    fun clearAllConversation()

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT id, created_at FROM messages WHERE conversation_id = :conversationId AND sender_id != :userId AND status = 'DELIVERED' ORDER BY created_at ASC")
    fun findUnreadMessagesSync(conversationId: String, userId: String = Session.getUserId()): List<MessageMinimal>?

    @Query("select id as messageId, media_url as mediaUrl , thumb_image as mediaThumbnail, name, media_duration mediaDuration,created_at as createdAt  from messages where type=:type and conversation_id = :conversationId and media_status = 'DONE' order by created_at DESC")
    fun getSharedMedia(conversationId: String,type:String) :LiveData<List<MediaMinimal>>

}
