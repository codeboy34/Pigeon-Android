package com.pigeonmessenger.viewmodals

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.database.room.entities.SearchMessageItem
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.di.Injectable
import com.pigeonmessenger.repo.ContactsRepo
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.vo.ConversationItemMinimal
import com.pigeonmessenger.vo.SearchDataPackage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SearchViewModel : ViewModel(), Injectable {

    companion object {
        private const val TAG ="SearchViewModel"
    }
    @Inject
    lateinit var conversationRepo: ConversationRepo

    @Inject
    lateinit var contactsRepo: ContactsRepo

    init {
        App.get().appComponent.inject(this)
    }

    private fun fuzzySearchMessage(context: CoroutineContext, query: String): Deferred<List<SearchMessageItem>> =
            GlobalScope.async(context) {
                conversationRepo.fuzzySearchMessage("%${query.trim()}%")
            }

    private fun fuzzySearchGroup(context: CoroutineContext, query: String): Deferred<List<ConversationItemMinimal>> =
            GlobalScope.async(context) {
                conversationRepo.fuzzySearchGroup("%${query.trim()}%")
            }

    private fun fuzzySearchUser(context: CoroutineContext, query: String): Deferred<List<User>> =
            GlobalScope.async(context) {
                contactsRepo.fuzzySearchUser("%${query.trim()}%")
            }

    private fun contactList() = contactsRepo.getRegisteredContacts()

    fun fuzzySearch(keyword: String?) = GlobalScope.async {
        if (keyword.isNullOrBlank()) {
            Log.d(TAG, "NullOrBlank: $keyword");
            SearchDataPackage(contactList(), null, null, null)
        } else {
            Log.d(TAG, ": NotNull $keyword");

            val userList = fuzzySearchUser(coroutineContext, keyword).await()
            val messageList = fuzzySearchMessage(coroutineContext, keyword).await()
            Log.d(TAG,"${messageList} ");
            val groupList = fuzzySearchGroup(coroutineContext, keyword).await()
            Log.d(TAG, "GROUPLIST ${groupList}: ");
            SearchDataPackage(null, userList, messageList, groupList)
        }
    }

}