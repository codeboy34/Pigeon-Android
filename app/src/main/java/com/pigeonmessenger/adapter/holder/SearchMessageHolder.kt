package com.pigeonmessenger.adapter.holder

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.SearchMessageItem
import com.pigeonmessenger.extension.getSpannableBuilder
import com.pigeonmessenger.fragment.SearchFragment
import com.pigeonmessenger.viewmodals.isGroup
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.widget.timeAgo
import kotlinx.android.synthetic.main.item_search_message.view.*
import org.jetbrains.anko.dip

class SearchMessageHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
    val icon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_file).apply {
            this?.setBounds(0, 0, itemView.dip(12f), itemView.dip(12f))
        }
    }

    fun bind(message: SearchMessageItem, onItemClickListener: SearchFragment.OnSearchClickListener?, keyword: String?) {
        itemView.search_name_tv.text = message.displayName
        if (message.type == MessageCategory.SIGNAL_DATA.name || message.type == MessageCategory.PLAIN_DATA.name) {
            TextViewCompat.setCompoundDrawablesRelative(itemView.search_msg_tv, icon, null, null, null)
            if (keyword != null && message.mediaName != null)
                itemView.search_msg_tv.setText(message.mediaName.getSpannableBuilder(itemView.context, keyword), TextView.BufferType.SPANNABLE)
            else itemView.search_msg_tv.text = message.mediaName
        } else {
            TextViewCompat.setCompoundDrawablesRelative(itemView.search_msg_tv, null, null, null, null)
            if (keyword != null && message.message != null)
                itemView.search_msg_tv.setText(message.message.getSpannableBuilder(itemView.context, keyword), TextView.BufferType.SPANNABLE)
            else itemView.search_msg_tv.text = message.message
        }

        itemView.search_name_tv.text = message.name()
        if (isGroup(message.conversationId)){
            itemView.search_avatar_iv.setGroup(message.conversationId,message.groupIconThumbnail)
        }else{
            //TODO Set avatar
        }
        //itemView.search_avatar_iv.setInfoWithThumbnail()
        itemView.search_time_tv.timeAgo(message.createdAt)
        //itemView.search_avatar_iv.setInfo(message.botFullName, message.botAvatarUrl, message.botUserId)
        itemView.setOnClickListener {
            onItemClickListener?.onMessageClick(message)
        }
    }
}