package com.pigeonmessenger.database.room.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.viewmodals.ConversationItem
import com.pigeonmessenger.vo.ConversationItemMinimal
import com.pigeonmessenger.vo.ConversationStorageUsage
import com.pigeonmessenger.vo.StorageUsage

@Dao
interface ConversationDao : BaseDao<Conversation> {

    @Query("select * from conversations where conversation_id =:conversationId")
    fun getConversationById(conversationId: String): LiveData<Conversation>

    @Transaction
    @Query("SELECT c.* FROM conversations c WHERE c.conversation_id = :conversationId")
    fun findConversationById(conversationId: String): Conversation?

    /* @Query("select c.conversationId as conversationId,c.unseen_message_count as unseenCount " +
            ",co.display_name as displayName , co.full_name as fullName ,co.relationship as relationship ," +
            "co.thumbnail as avatarThumbnail , co.avatar_url as avatarUrl"+
            ",m.message as message ,m.senderId as senderId, m.createdAt as createdAt ,m.msg_status as messageStatus ,m.type as messageType "+
            "from conversations c " +
            "left join users co on c.conversationId=co.user_id " +
            "inner join messages m on c.last_message_id = m.id order by m.createdAt desc")*/

    //@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)

    @Query("select c.conversation_id as conversationId, c.unseen_message_count as unseenCount, " +
            "c.pin_time as pinTime,c.icon_update,c.name as groupName, c.status as groupStatus, c.category as category, " +
            "c.owner_id as ownerId, c.mute_until as groupMuteUntil, c.icon_thumbnail as groupThumbnail, " +
            "co.display_name as displayName, co.full_name as fullName, co.relationship as relationship, " +
            "co.thumbnail as avatarThumbnail, co.mute_until as userMuteUntil, co.last_updated, " +
            "m.message as message,m.action, m.sender_id as senderId, m.participant_id as participantId," +
            "pu.display_name as participantDisplayName, pu.full_name as participantFullName, " +
            "su.display_name as senderDisplayName, su.full_name as senderFullName, " +
            "m.created_at as createdAt, m.status as messageStatus, m.type as messageType " +
            "from conversations c " +
            "left join users co on c.owner_id=co.user_id " +
            "left join messages m on c.last_message_id = m.id " +
            "left join users pu on m.participant_id=pu.user_id " +
            "left join users su on m.sender_id=su.user_id " +
            "where c.status !=1  order by c.pin_time DESC, m.created_at desc ")
    fun conversationList(): LiveData<List<ConversationItem>>

    @Query("UPDATE conversations SET draft = :text WHERE conversation_id = :conversationId")
    fun saveDraft(conversationId: String, text: String)


    @Query("UPDATE conversations SET status = :status WHERE conversation_id = :conversationId")
    fun updateConversationStatusById(conversationId: String, status: Int)


    @Query("UPDATE conversations SET owner_id = :ownerId, category = :category, name = :name, announcement = :announcement,mute_until = :muteUntil,icon_thumbnail=:thumbnail, created_at = :createdAt, status = :status WHERE conversation_id = :conversationId")
    fun updateConversation(
            conversationId: String,
            ownerId: String,
            category: String,
            name: String,
            thumbnail: String?,
            announcement: String?,
            muteUntil: String?,
            createdAt: String,
            status: Int
    )

    @Query("UPDATE conversations SET announcement = :announcement WHERE conversation_id = :conversationId")
    fun updateConversationAnnouncement(conversationId: String, announcement: String)

    @Query("DELETE from conversations where conversation_id = :conversationId")
    fun deleteConversationById(conversationId: String)

    @Query("UPDATE conversations SET mute_until = :muteUntil WHERE conversation_id = :conversationId")
    fun updateGroupDuration(conversationId: String, muteUntil: String?)

    @Query("UPDATE conversations SET icon_url = :iconUrl WHERE conversation_id = :conversationId")
    fun updateGroupIconUrl(conversationId: String, iconUrl: String)

    @Query("UPDATE conversations SET  icon_thumbnail=:thumbnail,icon_update=:timestamp where conversation_id= :conversationId ")
    fun updateGroupIcon(conversationId: String,  thumbnail: String? ,timestamp :String =System.currentTimeMillis().toString() )

    @Query("select unseen_message_count from conversations where conversation_id=:recipientId")
    fun indexUnreadCount(recipientId: String): Int

    @Query("UPDATE conversations SET last_message_id=:lastMessageId where conversation_id=:recipientId")
    fun updateLastMessageId(recipientId: String, lastMessageId: String)

    @Query("UPDATE conversations set pin_time = :pinTime where conversation_id=:conversationId")
    fun updateConversationPinTimeById(conversationId: String, pinTime: String?)

    @Query("UPDATE conversations set name = :name where conversation_id=:conversationId")
    fun updateGroupName(conversationId: String, name: String)

    @Query("DELETE from conversations")
    fun deleteAll()

    @Query("SELECT  c.conversation_id as conversationId, c.owner_id as ownerId, c.category, c.icon_url as groupIconUrl, c.name as groupName ,u.full_name as fullName , u.display_name as displayName, m.mediaSize FROM conversations c INNER JOIN (SELECT conversation_id, sum(media_size) as mediaSize FROM messages WHERE IFNULL(media_size,'') != '' GROUP BY conversation_id) m ON m.conversation_id = c.conversation_id INNER JOIN users u ON u.user_id = c.owner_id ORDER BY m.mediaSize DESC")
    fun getConversationStorageUsage(): LiveData<List<ConversationStorageUsage>?>


    @Query("SELECT type, sum(media_size) as mediaSize ,conversation_id as conversationId, count(id) as count FROM messages WHERE conversation_id = :conversationId AND IFNULL(media_size,'') != '' GROUP BY type")
    fun getStorageUsage(conversationId: String): List<StorageUsage>


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.conversation_id AS conversationId,c.icon_thumbnail as groupIconThumbnail, c.category AS category, c.name AS groupName FROM conversations c  WHERE c.category = 'GROUP'  AND c.name LIKE :query ORDER BY c.created_at DESC")
    fun fuzzySearchGroup(query: String): List<ConversationItemMinimal>

    @Query("SELECT DISTINCT c.conversation_id FROM conversations c WHERE c.owner_id = :recipientId and c.category = 'SUCCESS' ") //TODO match with mixin bcs its change accidentk
    fun getConversationIdIfExistsSync(recipientId: String): String?


    //fun mute()

}