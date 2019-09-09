package com.pigeonmessenger.adapter.holder

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.pigeonmessenger.widget.timeAgoDate
import kotlinx.android.synthetic.main.item_chat_view.view.*

class TimeHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(time: String) {
        itemView.chat_time.timeAgoDate(time)
    }
}