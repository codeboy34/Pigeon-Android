package com.pigeonmessenger.adapter.holder

import android.view.View

class TransparentHolder(containerView: View) : BaseViewHolder(containerView) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
    }
}
