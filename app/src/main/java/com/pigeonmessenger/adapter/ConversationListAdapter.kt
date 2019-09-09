package com.pigeonmessenger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.holder.ConversationItemHolder
import com.pigeonmessenger.extension.notNullElse
import com.pigeonmessenger.viewmodals.ConversationItem

class ConversationListAdapter: RecyclerView.Adapter<ConversationItemHolder>() {

        var conversations: List<ConversationItem>? = null

        var onItemClickListener: OnItemClickListener? = null

        fun setConversationList(newConversations: List<ConversationItem>) {
            if (conversations == null) {
                conversations = newConversations
                notifyItemRangeInserted(0, newConversations.size)
            } else {
                val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return conversations!!.size
                    }

                    override fun getNewListSize(): Int {
                        return newConversations.size
                    }

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val old = conversations!![oldItemPosition]
                        val newItem = newConversations[newItemPosition]
                        return old.conversationId == newItem.conversationId
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val old = conversations!![oldItemPosition]
                        val newItem = newConversations[newItemPosition]
                        return old == newItem
                    }
                })
                conversations = newConversations
                diffResult.dispatchUpdatesTo(this)
            }
        }

        override fun onBindViewHolder(holder: ConversationItemHolder, position: Int) {
            holder.bind(onItemClickListener, position, conversations!![position])
        }

        override fun getItemCount() = notNullElse(conversations, { list -> list.size }, 0)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationItemHolder =
            ConversationItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_list_conversation, parent, false))



    interface OnItemClickListener {
        fun click(position: Int, conversation: ConversationItem)
        fun longClick(conversation: ConversationItem): Boolean
    }
    }