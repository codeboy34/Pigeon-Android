package com.pigeonmessenger.adapter.holder

import android.view.View
import com.pigeonmessenger.extension.displayHeight
import com.pigeonmessenger.extension.displaySize
import com.pigeonmessenger.extension.dpToPx


abstract class MediaHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    protected val dp6 by lazy {
        itemView.context.dpToPx(6f)
    }

    protected val mediaWidth by lazy {
        (itemView.context.displaySize().x * 0.6).toInt()
    }

    protected val mediaHeight by lazy {
        (itemView.context.displayHeight() * 2 / 3)
    }
}
