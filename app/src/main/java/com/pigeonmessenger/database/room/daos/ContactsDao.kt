package com.pigeonmessenger.database.room.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pigeonmessenger.Session
import com.pigeonmessenger.database.room.entities.BaseDao
import com.pigeonmessenger.database.room.entities.User

@Dao
interface ContactsDao : BaseDao<User> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(contacts: List<User>)

    @Update
    fun update(contacts: List<User>)

    @Query("select * from users where relationship= 'REGISTERED_CONTACT'")
    fun getLiveUsers(): LiveData<List<User>>

    @Query("select * from users where relationship='REGISTERED_CONTACT'")
    fun getUsersList(): List<User>

    @Query("select * from users")
    fun getUsers(): List<User>

    @Delete
    fun delete(list: List<User>)

    @Query("delete from users  where user_id  not in (:list)")
    fun deleteExtraContacts(list: String)

    @Query("delete from users where user_id=:phonenumber")
    fun deleteContact(phonenumber: String)

    @Query("update users set full_name=:full_name,bio=:bio, relationship='REGISTERED_CONTACT' where user_id=:phone_number ")
    fun updateContactAndRelationship(phone_number: String, full_name: String, bio: String?)

    // don't use , Only used for syncUser function
    @Query("update users set full_name=:fullName,bio=:bio,thumbnail=:thumbnail, relationship='STRANGE' where user_id=:userId")
    fun syncUser(userId: String, fullName: String, bio: String?, thumbnail: String?)

    @Query("update users set relationship=:relationship where user_id=:userId")
    fun updateRelationship(userId: String, relationship: String)

    @Query("update users set full_name=:full_name,bio=:bio  where user_id=:phone_number ")
    fun updateContact(phone_number: String, full_name: String, bio: String?)

    @Query("update users set thumbnail=:thumbnail , last_updated=:updateTimestamp where user_id=:userId")
    fun updateThumbnail(userId: String, thumbnail: String?,updateTimestamp: String = System.currentTimeMillis().toString())

    @Query("select * from users where user_id=:userId and relationship='REGISTERED_CONTACT' or relationship='STRANGE'")
    fun findUser(userId: String): User?

    @Query("select * from users where user_id=:userId")
    fun findLiveUser(userId: String):LiveData<User>

    @Query("select * from users where user_id=:userId")
    fun findContact(userId: String): User?

    @Query("select * from users where user_id=:contactId")
    fun getContact(contactId: String): LiveData<User>


    @Query("SELECT * FROM users WHERE user_id != :id AND relationship = 'REGISTERED_CONTACT' AND ((display_name LIKE :username OR (full_name like :username and display_name is null))OR user_id like :username) ")
    fun fuzzySearchUser(username: String, id: String = Session.getUserId()): List<User>

    @Query("update users set display_name=:name where user_id=:userId")
    fun updateDisplayName(userId: String, name: String)

    @Query("select * from users where user_id=:userId")
    fun findSelf(userId: String): LiveData<User>

    @Query("UPDATE users set relationship='BLOCKING' where user_id = :userId")
    fun block(userId: String)


    @Query("UPDATE users SET mute_until = :muteUntil WHERE user_id = :id")
    fun updateDuration(id: String, muteUntil: String?)

    @Query("select * from users where relationship='BLOCKING' ")
    fun blockList(): LiveData<List<User>>

    @Query("update users set last_updated=:timestamp where user_id =:userId")
    fun updateLastUpdate(userId: String, timestamp: String = System.currentTimeMillis().toString())

}