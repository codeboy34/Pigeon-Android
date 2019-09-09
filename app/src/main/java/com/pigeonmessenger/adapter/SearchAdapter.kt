package com.pigeonmessenger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.holder.ContactHolder
import com.pigeonmessenger.adapter.holder.GroupHolder
import com.pigeonmessenger.adapter.holder.HeaderHolder
import com.pigeonmessenger.adapter.holder.SearchMessageHolder
import com.pigeonmessenger.database.room.entities.SearchMessageItem
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.fragment.SearchFragment
import com.pigeonmessenger.vo.ConversationItemMinimal
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter

class SearchAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyRecyclerHeadersAdapter<HeaderHolder> {

    var onItemClickListener: SearchFragment.OnSearchClickListener? = null

    var keyword: String? = null

    override fun getHeaderId(position: Int): Long = getItemViewType(position).toLong()

    override fun onBindHeaderViewHolder(holder: HeaderHolder, position: Int) {
        val context = holder.itemView.context
        when {
            getItemViewType(position) == 0 -> holder.bind(context.getText(R.string.search_title_contacts).toString())
            getItemViewType(position) == 2 -> holder.bind(context.getText(R.string.search_title_groups).toString())
            else -> holder.bind(context.getText(R.string.search_title_messages).toString())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_header, parent, false)
        return HeaderHolder(item)
    }

    fun setData(
            contactsList: List<User>?,
            messageList: List<SearchMessageItem>?,
            groupList: List<ConversationItemMinimal>?
    ) {
        this.userList = contactsList
        this.messageList = messageList
        dataList.clear()
        userList?.let { dataList.addAll(it) }
        groupList?.let { dataList.addAll(it) }
        messageList?.let { dataList.addAll(it) }
        notifyDataSetChanged()
    }

    private var dataList = ArrayList<Any>()
    private var userList: List<User>? = null
    private var messageList: List<SearchMessageItem>? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> {
                dataList[position].let {
                    (holder as ContactHolder).bind(it as User, onItemClickListener, keyword)
                }
            }
            1 -> {
                dataList[position].let {
                    (holder as SearchMessageHolder).bind(it as SearchMessageItem, onItemClickListener, keyword)
                }
            }
            2 -> {
                dataList[position].let {
                    (holder as GroupHolder).bind(it as ConversationItemMinimal, onItemClickListener, keyword!!)
                }
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                0 -> {
                    val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                    ContactHolder(item)
                }
                1 -> {
                    val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_message, parent, false)
                    SearchMessageHolder(item)
                }
                2 -> {
                    val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_group, parent, false)
                    GroupHolder(item)
                }
                else -> {
                    val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                    ContactHolder(item)
                }
            }

    override fun getItemViewType(position: Int): Int = when {
        dataList[position] is User -> 0
        dataList[position] is ConversationItemMinimal -> 2
        else -> 1
    }
}