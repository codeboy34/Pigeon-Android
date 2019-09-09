package com.pigeonmessenger.vo

import com.pigeonmessenger.database.room.entities.SearchMessageItem
import com.pigeonmessenger.database.room.entities.User

data class SearchDataPackage(
        val allContactList: List<User>?,
        val contactList: List<User>?,
        val messageList: List<SearchMessageItem>?,
        val groupList: List<ConversationItemMinimal>?
        )