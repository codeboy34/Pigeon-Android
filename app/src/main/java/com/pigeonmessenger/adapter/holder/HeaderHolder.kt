package com.pigeonmessenger.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_header.view.*

class HeaderHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(text: String) {
        itemView.search_header_tv.text = text
    }
}