package com.pigeonmessenger.adapter.holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.extension.getSpannableBuilder
import com.pigeonmessenger.fragment.SearchFragment
import com.pigeonmessenger.vo.ConversationItemMinimal
import kotlinx.android.synthetic.main.item_search_group.view.*

class GroupHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {


    fun bind(
        conversation: ConversationItemMinimal,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
        keyword:String
    ) {
      itemView.search_name.setText(conversation.groupName!!.getSpannableBuilder(itemView.context,keyword), TextView.BufferType.SPANNABLE)
        itemView.search_avatar_iv.setGroup(conversation.conversationId,conversation.groupIconThumbnail)
        itemView.setOnClickListener {
            onItemClickListener?.onGroupClick(conversation)
        }
    }
}