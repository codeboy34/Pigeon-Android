package com.pigeonmessenger.fragment


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.ChatRoom
import com.pigeonmessenger.adapter.SearchAdapter
import com.pigeonmessenger.database.room.entities.SearchMessageItem
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.utils.onlyLast
import com.pigeonmessenger.viewmodals.SearchViewModel
import com.pigeonmessenger.vo.ConversationItemMinimal
import com.pigeonmessenger.vo.SearchDataPackage
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.jetbrains.anko.custom.onUiThread
import kotlin.coroutines.CoroutineContext


class SearchFragment : Fragment() {

    private lateinit var searchContext: CoroutineContext
    private lateinit var searchChannel: Channel<Deferred<SearchDataPackage>>

    private val searchViewModel by lazy {
        ViewModelProviders.of(this).get(SearchViewModel::class.java)
    }

    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter()
    }

    companion object {
        private const val TAG = "SearchFragment"

        @Volatile
        private var INSTANCE: SearchFragment? = null

        fun getInstance(): SearchFragment =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: SearchFragment().also { INSTANCE = it }
                }
    }

    private var keyword: String? = null
        set(value) {
            if (field != value) {
                field = value
                bindData()
            }
        }

    fun setQueryText(text: String) {
        if (isAdded && text != keyword) {
            keyword = text
        }
    }


    private var searchDisposable: Disposable? = null
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("CheckResult")
    private fun bindData(keyword: String? = this@SearchFragment.keyword) {
        searchDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        fuzzySearch(searchContext, keyword)
    }

    private fun fuzzySearch(context: CoroutineContext, keyword: String?) = runBlocking(context) {
        searchChannel.send(searchViewModel.fuzzySearch(keyword))
    }

    private fun setSearchListener(listener: (SearchDataPackage) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchChannel)) {
            listener(result)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView()---------: ");
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "====onViewCreated() ======");
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated: ");
        searchContext = Job()
        searchChannel = Channel()
        search_rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        search_rv.addItemDecoration(StickyRecyclerHeadersDecoration(searchAdapter))
        search_rv.adapter = searchAdapter


        searchAdapter.onItemClickListener = object : OnSearchClickListener {
            override fun onGroupClick(conversation: ConversationItemMinimal) {
                ChatRoom.show(requireContext(),conversation.conversationId)
            }

            override fun onMessageClick(message: SearchMessageItem) {
                ChatRoom.show(requireContext(),message.conversationId,message.userId,messageId = message.messageId,keyword = keyword)
            }

            override fun onContactClick(user: User) {
                ChatRoom.show(requireContext(),null,user.userId)
            }

        }


        setSearchListener {
            requireContext().onUiThread {
                if (it.allContactList != null) {
                    Log.d(TAG, "Contacts ${it.allContactList}: ");
                    searchAdapter.setData(it.allContactList, null,null)
                } else {
                    searchAdapter.keyword = keyword
                    Log.d(TAG, "Messages ${it.messageList}");
                    searchAdapter.setData(it.contactList, it.messageList,it.groupList)
                }
            }
        }
    }

    override fun onDetach() {
        Log.d(TAG, "-----OnDetach-----: ");
        super.onDetach()
        if (!searchChannel.isClosedForReceive)
        searchChannel.close()
        searchContext.cancelChildren()
        INSTANCE = null
    }

    interface OnSearchClickListener {
        fun onContactClick(user: User)
        fun onMessageClick(message: SearchMessageItem)
        fun onGroupClick(conversation: ConversationItemMinimal)
    }


}
