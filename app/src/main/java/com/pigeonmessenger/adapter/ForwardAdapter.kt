package com.pigeonmessenger.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.viewmodals.ConversationItem
import com.pigeonmessenger.vo.ConversationCheckView
import com.pigeonmessenger.widget.inflate
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_contact_header.view.*

class ForwardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyRecyclerHeadersAdapter<ForwardAdapter.HeaderViewHolder> {

    companion object {
        const val TYPE_CONVERSATION = 0
        const val TYPE_FRIEND = 1
    }

    var selectItem = ArrayList<Any>()

    private var listener: ForwardListener? = null
    var conversations: List<ConversationItem>? = null
    var friends: List<User>? = null
    var showHeader: Boolean = true

    override fun getItemCount(): Int {
        return if (conversations == null && friends == null) {
            0
        } else if (conversations == null) {
            friends?.size ?: 0
        } else if (friends == null) {
            conversations?.size ?: 0
        } else {
            conversations!!.size + friends!!.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            TYPE_CONVERSATION
        } else {
            TYPE_FRIEND
        }
    }

    override fun getHeaderId(position: Int): Long {
        if (!showHeader) {
            return -1
        }
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            1
        } else {
            2
        }
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (conversations == null || conversations!!.isEmpty() && friends == null && friends!!.isEmpty()) {
            return
        }
        if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            holder.itemView.header.text = holder.itemView.context.getString(R.string.chat_item_title)
        } else {
            holder.itemView.header.text = holder.itemView.context.getString(R.string.contact_item_title)
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val view = parent.inflate(R.layout.item_contact_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (conversations == null || conversations!!.isEmpty() && friends == null && friends!!.isEmpty()) {
            return
        }
        if (holder is ConversationViewHolder) {
            val conversationItem = conversations!![position]
            holder.bind(conversationItem, listener, selectItem.contains(conversationItem))
        } else {
            holder as FriendViewHolder
            val pos = if (conversations != null && conversations!!.isNotEmpty())
                position - conversations!!.size else position
            val user = friends!![pos]
            holder.bind(user, listener, selectItem.contains(user))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CONVERSATION) {
            ConversationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_forward_conversation,
                parent, false))
        } else {
            FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_friend,
                parent, false))
        }
    }

    fun setForwardListener(listener: ForwardListener) {
        this.listener = listener
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: User, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ConversationItem, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface ForwardListener {
        fun onUserItemClick(user: User)
        fun onConversationItemClick(item: ConversationItem)
    }
}