package com.pigeonmessenger.repo

import com.pigeonmessenger.Session
import com.pigeonmessenger.database.room.daos.ContactsDao
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ContactsRepo(var contactsDao: ContactsDao) {

    private val TAG= "ContactsRepo"

    fun getRegisteredContacts() = contactsDao.getUsersList()

    fun getContacts()=contactsDao.getLiveUsers()


    fun upsert(user: User) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            val u=contactsDao.findContact(user.userId)
            if (u==null){
                contactsDao.insert(user)
            }else{
                contactsDao.updateContactAndRelationship(user.userId,user.userId,user.bio)
            }
        }
    }


    fun getContact(contactId:String)= contactsDao.getContact(contactId)

    fun fuzzySearchUser(query: String): List<User> = contactsDao.fuzzySearchUser(query)

    fun updateRelationship(userId: String,relationship: String){
        contactsDao.updateRelationship(userId,relationship)
    }

    public fun findSelf()= contactsDao.findSelf(Session.getUserId() ?: "")


}