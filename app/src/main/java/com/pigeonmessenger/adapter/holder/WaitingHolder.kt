package com.pigeonmessenger.adapter.holder

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.vo.MessageItem
import com.pigeonmessenger.vo.senderName
import com.pigeonmessenger.widget.NoUnderLineSpan
import com.pigeonmessenger.widget.timeAgoClock
import kotlinx.android.synthetic.main.item_chat_waiting.view.*

class WaitingHolder constructor(
    containerView: View,
    private val onItemListener: ConversationAdapter.OnItemListener
) : BaseViewHolder(containerView) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
    }

    init {
        itemView.chat_tv.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun highlightLinkText(
        source: String,
        color: Int,
        texts: Array<String>,
        links: Array<String>
    ): SpannableString {
        if (texts.size != links.size) {
            throw IllegalArgumentException("texts's length should equals with links")
        }
        val sp = SpannableString(source)
        for (i in texts.indices) {
            val text = texts[i]
            val link = links[i]
            val start = source.indexOf(text)
            sp.setSpan(NoUnderLineSpan(link, onItemListener), start,
                start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(ForegroundColorSpan(color), start, start + text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sp
    }

    fun bind(
            messageItem: MessageItem,
            isLast: Boolean,
            isFirst: Boolean,
            onItemListener: ConversationAdapter.OnItemListener
    ) {
        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        val colorPrimary = ContextCompat.getColor(App.get(),R.color.colorBlue)
        val learn: String = App.get().getString(R.string.chat_learn)
        val info = App.get().getString(R.string.chat_waiting, messageItem.senderName(), learn)
        val learnUrl = App.get().getString(R.string.chat_waiting_url) //TODO change url
        itemView.chat_tv.text = highlightLinkText(
            info,
            colorPrimary,
            arrayOf(learn),
            arrayOf(learnUrl))

        if (isFirst) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.senderName()

            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.senderId) }
            itemView.chat_name.setTextColor(colors[messageItem.senderId.toLong().rem(colors.size).toInt()])
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        chatLayout(false, isLast)
    }
}