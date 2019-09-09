package com.pigeonmessenger.adapter.holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.getSpannableBuilder
import com.pigeonmessenger.extension.isNumber
import com.pigeonmessenger.fragment.SearchFragment
import kotlinx.android.synthetic.main.item_search_contact.view.*


class ContactHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {


    fun bind(user: User, onItemClickListener: SearchFragment.OnSearchClickListener?, keyword: String?) {
        if (keyword != null) {
            if (keyword.isNumber()){
                itemView.name_tv.text = user.getName()
                itemView.phone_tv.setText(user.userId.getSpannableBuilder(itemView.context, keyword), TextView.BufferType.SPANNABLE)
            }else {
                itemView.name_tv.setText(user.getName().getSpannableBuilder(itemView.context, keyword), TextView.BufferType.SPANNABLE)
                itemView.phone_tv.text = user.userId
            }
        } else {
            itemView.name_tv.text = user.getName()
        }

        itemView.phone_tv.text = user.userId

        itemView.setOnClickListener {
            onItemClickListener?.onContactClick(user)
        }
    }
}