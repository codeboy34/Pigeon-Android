package com.pigeonmessenger.database.room.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.vo.Participant


@Dao
interface ParticipantDao : BaseDao<Participant> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.user_id, u.full_name, u.relationship FROM participants p, users u WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id")
    fun getParticipants(conversationId: String): List<User>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.user_id, u.display_name, u.full_name, u.thumbnail, u.relationship FROM participants p, users u WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id ORDER BY p.created_at DESC")
    fun getGroupParticipantsLiveData(conversationId: String): LiveData<List<User>>

    @Query("UPDATE participants SET role = :role where conversation_id = :conversationId AND user_id = :userId")
    fun updateParticipantRole(conversationId: String, userId: String, role: String)

    @Transaction
    @Query("SELECT * FROM participants WHERE conversation_id = :conversationId")
    fun getRealParticipants(conversationId: String): List<Participant>

    @Query("DELETE FROM participants WHERE conversation_id = :conversationId AND user_id = :userId")
    fun deleteById(conversationId: String, userId: String)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.user_id, u.full_name, u.relationship FROM participants p, users u WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id ORDER BY p.created_at LIMIT 4")
    fun getParticipantsAvatar(conversationId: String): List<User>


    @Transaction
    @Query("SELECT p.* FROM participants p LEFT JOIN users u ON p.user_id = u.user_id WHERE p.conversation_id = :conversationId " +
            "AND p.user_id NOT IN (SELECT user_id FROM sent_sender_keys WHERE conversation_id= :conversationId) AND p.user_id != :accountId")
    fun getNotSentKeyParticipants(conversationId: String, accountId: String): List<Participant>?


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.user_id, u.full_name, u.relationship FROM participants p, users u WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id ORDER BY p.created_at DESC LIMIT :limit")
    fun getLimitParticipants(conversationId: String, limit: Int): List<User>

    @Query("SELECT * FROM participants WHERE conversation_id = :conversationId AND user_id = :userId")
    fun findParticipantByIds(conversationId: String, userId: String): Participant?

    @Query("SELECT count(*) FROM participants WHERE conversation_id = :conversationId")
    fun getParticipantsCount(conversationId: String): Int
}