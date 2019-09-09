package com.pigeonmessenger.adapter.holder

import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import androidx.appcompat.content.res.AppCompatResources
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.gson.Gson
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.extension.dpToPx
import com.pigeonmessenger.extension.formatMillis
import com.pigeonmessenger.extension.loadImageCenterCrop
import com.pigeonmessenger.extension.notNullElse
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.vo.MessageItem
import com.pigeonmessenger.vo.QuoteMessageItem
import com.pigeonmessenger.widget.linktext.AutoLinkMode
import com.pigeonmessenger.widget.round
import com.pigeonmessenger.widget.timeAgoClock
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_reply.view.*

class ReplyHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    private val dp16 = itemView.context.dpToPx(16f)
    private val dp8 = itemView.context.dpToPx(8f)
    private val dp6 = itemView.context.dpToPx(6f)

    init {
        itemView.chat_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        itemView.chat_tv.setUrlModeColor(LINK_COLOR)

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
        val lp = (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams)
        if (isMe) {
            lp.gravity = Gravity.END
            itemView.chat_tv.setTextColor(Color.BLACK)
            if (isLast) {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            } else {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            }
        } else {
            itemView.chat_tv.setTextColor(Color.BLACK)
            lp.gravity = Gravity.START
            if (isLast) {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            } else {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }

    init {
        itemView.reply_layout.round(dp6)
        itemView.reply_iv.round(dp6)
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

        itemView.chat_layout.setOnLongClickListener {
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

        itemView.chat_time.timeAgoClock(messageItem.createdAt)
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
        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.senderId

            itemView.chat_name.setTextColor(colors[messageItem.senderId!!.toLong().rem(colors.size).toInt()])
            //itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.senderId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        setStatusIcon(isMe, messageItem.status!!, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })

        itemView.chat_layout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onMessageClick(messageItem.quoteMessageId)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        val quoteMessage = Gson().fromJson(messageItem.quoteContent, QuoteMessageItem::class.java)
        itemView.reply_name_tv.text = quoteMessage.userFullName
        itemView.reply_name_tv.setTextColor(colors[quoteMessage.senderId.toLong().rem(colors.size).toInt()])
        itemView.reply_layout.setBackgroundColor(colors[quoteMessage.senderId.toLong().rem(colors.size).toInt()])
        itemView.reply_layout.background.alpha = 0x0D
        itemView.start_view.setBackgroundColor(colors[quoteMessage.senderId.toLong().rem(colors.size).toInt()])
        when {
            quoteMessage.type.endsWith("_TEXT") -> {
                itemView.reply_content_tv.text = quoteMessage.message
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                setIcon()
            }
            quoteMessage.type.endsWith("_IMAGE") -> {
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.mediaUrl, R.drawable.image_holder)
                itemView.reply_content_tv.setText(R.string.photo)
                setIcon(R.drawable.ic_status_pic)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }
            quoteMessage.type.endsWith("_VIDEO") -> {
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.mediaUrl, R.drawable.image_holder)
                itemView.reply_content_tv.setText(R.string.video)
                setIcon(R.drawable.ic_status_video)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }
            quoteMessage.type.endsWith("_DATA") -> {
                notNullElse(quoteMessage.mediaName, {
                    itemView.reply_content_tv.text = it
                }, {
                    itemView.reply_content_tv.setText(R.string.document)
                })
                setIcon(R.drawable.ic_status_file)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
            }
            quoteMessage.type.endsWith("_AUDIO") -> {
                notNullElse(quoteMessage.mediaDuration, {
                    itemView.reply_content_tv.text = it.toLong().formatMillis()
                }, {
                    itemView.reply_content_tv.setText(R.string.audio)
                })
                setIcon(R.drawable.ic_status_audio)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
            }
           /* quoteMessage.type.endsWith("_STICKER") -> {
                itemView.reply_content_tv.setText(R.string.conversation_status_sticker)
                setIcon(R.drawable.ic_status_stiker)
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.assetUrl, R.drawable.image_holder)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }
            quoteMessage.type.endsWith("_CONTACT") -> {
                itemView.reply_content_tv.text = quoteMessage.sharedUserIdentityNumber
                setIcon(R.drawable.ic_status_contact)
                itemView.reply_avatar.setInfo(quoteMessage.sharedUserFullName, quoteMessage.sharedUserAvatarUrl, quoteMessage.sharedUserIdentityNumber
                    ?: "0")
                itemView.reply_avatar.visibility = View.VISIBLE
                itemView.reply_iv.visibility = View.INVISIBLE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }*/
            quoteMessage.type == MessageCategory.APP_BUTTON_GROUP.name || quoteMessage.type == MessageCategory.APP_CARD.name -> {
                itemView.reply_content_tv.setText(R.string.extensions)
                setIcon(R.drawable.ic_touch_app)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
            }
            else -> {
                itemView.reply_iv.visibility = View.GONE
            }
        }
        chatLayout(isMe, isLast)
    }

    private fun setIcon(@DrawableRes icon: Int? = null) {
        notNullElse(icon, {
            AppCompatResources.getDrawable(itemView.context, it).let {
                it?.setBounds(0, 0, itemView.context.dpToPx(12f), itemView.context.dpToPx(12f))
                TextViewCompat.setCompoundDrawablesRelative(itemView.reply_content_tv, it, null, null, null)
            }
        }, {
            TextViewCompat.setCompoundDrawablesRelative(itemView.reply_content_tv, null, null, null, null)
        })
    }
}