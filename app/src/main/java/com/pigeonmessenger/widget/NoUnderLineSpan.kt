package com.pigeonmessenger.widget

import android.text.TextPaint
import android.text.style.URLSpan
import android.view.View
import com.pigeonmessenger.adapter.ConversationAdapter

class NoUnderLineSpan(url: String, private val onItemListener: ConversationAdapter.OnItemListener? = null)
    : URLSpan(url) {

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
    }

    override fun onClick(widget: View) {
        if (onItemListener == null) {
         //   widget.context.openUrl(url)
        } else {
            onItemListener.onUrlClick(url)
        }
    }
}
