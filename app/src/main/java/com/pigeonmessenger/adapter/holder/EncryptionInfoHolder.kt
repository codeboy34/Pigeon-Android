package com.pigeonmessenger.adapter.holder

import android.view.View
import com.pigeonmessenger.R
import kotlinx.android.synthetic.main.item_encryption_info.view.*

class EncryptionInfoHolder(itemView: View) : BaseViewHolder(itemView) {

    fun bind() {
        itemView.chat_info.text = itemView.context.getString(R.string.info)
    }
}
