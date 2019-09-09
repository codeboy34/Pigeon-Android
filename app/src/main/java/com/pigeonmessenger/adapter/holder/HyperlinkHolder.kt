package com.pigeonmessenger.adapter.holder

import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.View
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.extension.maxItemWidth
import com.pigeonmessenger.extension.notNullElse
import com.pigeonmessenger.vo.MessageItem
import com.pigeonmessenger.widget.linktext.AutoLinkMode
import com.pigeonmessenger.widget.timeAgoClock
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_hyperlink.view.*

class HyperlinkHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        itemView.chat_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        itemView.chat_tv.setUrlModeColor(LINK_COLOR)
        (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams).also {
            it.matchConstraintMaxWidth = itemView.context.maxItemWidth()
        }

        itemView.chat_tv.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener?.onUrlClick(matchedText)
                }
                AutoLinkMode.MODE_MENTION -> {
                    onItemListener?.onMentionClick(matchedText)
                }
                else -> {
                }
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
            messageItem: MessageItem,
            keyword: String?,
            isLast: Boolean,
            isFirst: Boolean = false,
            hasSelect: Boolean,
            isSelect: Boolean,
            onItemListener: ConversationAdapter.OnItemListener
    ) {

        Log.d("HyperlinkHolder", ":onBind() ");
        this.onItemListener = onItemListener
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.chat_tv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }

        itemView.chat_tv.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        notNullElse(keyword, { k ->
            messageItem.message?.let { str ->
                val start = str.indexOf(k, 0, true)
                if (start >= 0) {
                    val sp = SpannableString(str)
                    sp.setSpan(BackgroundColorSpan(HIGHLIGHTED), start,
                        start + k.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    itemView.chat_tv.text = sp
                } else {
                    itemView.chat_tv.text = messageItem.message
                }
            }
        }, {
            itemView.chat_tv.text = messageItem.message
        })
        val isMe = meId == messageItem.senderId

        itemView.chat_name.visibility = View.GONE


        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status!!, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        itemView.chat_name_tv.visibility = if (messageItem.siteName.isNullOrBlank()) {
            View.GONE
        } else {
            itemView.chat_name_tv.text = messageItem.siteName
            View.VISIBLE
        }

        itemView.chat_description_tv.visibility = if (messageItem.siteDescription.isNullOrBlank()) {
            View.GONE
        } else {
            itemView.chat_description_tv.text = messageItem.siteDescription
            View.VISIBLE
        }
        chatLayout(isMe, isLast)
    }
}