package com.pigeonmessenger.adapter.holder

import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.vo.MediaStatus
import com.pigeonmessenger.vo.MessageItem
import com.pigeonmessenger.widget.round
import com.pigeonmessenger.widget.timeAgoClock
import kotlinx.android.synthetic.main.item_chat_video.view.*

class VideoHolder constructor(containerView: View) : MediaHolder(containerView) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_image.round(radius)
        itemView.chat_time.round(radius)
        itemView.progress.round(radius)
    }

    private val dp4 by lazy {
        itemView.context.dpToPx(4f)
    }

    fun bind(
            messageItem: MessageItem,
            isLast: Boolean,
            isFirst: Boolean,
            hasSelect: Boolean,
            isSelect: Boolean,
            onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnClickListener {
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

        val isMe = meId == messageItem.senderId


        if (messageItem.mediaStatus == MediaStatus.DONE.name) {
            notNullElse(messageItem.mediaDuration, {
                itemView.duration_tv.visibility = VISIBLE
                itemView.duration_tv.text = it.toLong().formatMillis()
            }, {
                itemView.duration_tv.visibility = GONE
            })
        } else {
            notNullElse(messageItem.mediaSize, {
                itemView.duration_tv.visibility = VISIBLE
                itemView.duration_tv.text = it.fileSize()
            }, {
                itemView.duration_tv.visibility = GONE
            })
        }

        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.chat_warning.visibility = View.VISIBLE
                    itemView.progress.visibility = View.GONE
                    itemView.play.visibility = GONE
                    itemView.chat_image.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            true
                        }
                    }
                    itemView.chat_image.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        }
                    }
                }
                MediaStatus.PENDING.name -> {
                    itemView.chat_warning.visibility = GONE
                    itemView.progress.visibility = VISIBLE
                    itemView.play.visibility = GONE
                    itemView.progress.enableLoading()
                    itemView.progress.setBindId(messageItem.id)
                    itemView.progress.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            false
                        }
                    }
                    itemView.progress.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        } else {
                            onItemListener.onCancel(messageItem.id)
                        }
                    }
                    itemView.chat_image.setOnClickListener { }
                    itemView.chat_image.setOnLongClickListener { false }
                }
                MediaStatus.DONE.name -> {
                    itemView.chat_warning.visibility = GONE
                    itemView.progress.visibility = GONE
                    itemView.play.visibility = VISIBLE
                    itemView.progress.setBindId(messageItem.id)
                    itemView.progress.setOnClickListener {}
                    itemView.progress.setOnLongClickListener { false }
                    itemView.chat_image.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            true
                        }
                    }
                    itemView.chat_image.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        } else {
                            onItemListener.onImageClick(messageItem, itemView.chat_image)
                        }
                    }
                }
                MediaStatus.CANCELED.name -> {
                    itemView.chat_warning.visibility = GONE
                    itemView.progress.visibility = VISIBLE
                    itemView.play.visibility = GONE
                    if (isMe) {
                        itemView.progress.enableUpload()
                    } else {
                        itemView.progress.enableDownload()
                    }
                    itemView.progress.setBindId(messageItem.id)
                    itemView.progress.setProgress(-1)
                    itemView.progress.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            false
                        }
                    }
                    itemView.progress.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        } else {
                            if (isMe) {
                                onItemListener.onRetryUpload(messageItem.id)
                            } else {
                                onItemListener.onRetryDownload(messageItem.id)
                            }
                        }
                    }
                    itemView.chat_image.setOnClickListener {}
                    itemView.chat_image.setOnLongClickListener { false }
                }
            }
        }

        setStatusIcon(isMe, messageItem.mediaStatus!!, {
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, null, null, it, null)
        }, {
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, null, null, null, null)
        }, true)

        dataWidth = messageItem.mediaWidth
        dataHeight = messageItem.mediaHeight
        dataUrl = messageItem.mediaUrl
        dataThumbImage = messageItem.thumbImage
        chatLayout(isMe, isLast)
    }

    private var dataUrl: String? = null
    private var dataThumbImage: String? = null
    private var dataWidth: Int? = null
    private var dataHeight: Int? = null

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
             //   itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow_last)
            } else {
             //   itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            }
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (itemView.duration_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
        } else {
            if (isLast) {
                //itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            } else {
               // itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            }
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (itemView.duration_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp10
        }

        var width = mediaWidth - dp6
        when {
            isLast -> {
                width = mediaWidth
                (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
            }
            isMe -> {
                (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp6
                (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
            }
            else -> {
                (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp6
            }
        }
        if (dataWidth == null || dataHeight == null ||
            dataWidth!! <= 0 || dataHeight!! <= 0) {
            itemView.chat_image.layoutParams.width = width
            itemView.chat_image.layoutParams.height = width
        } else {
            itemView.chat_image.layoutParams.width = width
            itemView.chat_image.layoutParams.height = width * dataHeight!! / dataWidth!!
        }

        itemView.chat_image.setShape(R.drawable.msg_out_img_shape)
        if (isBlink) {
            itemView.chat_image.loadVideoMark(dataUrl, 1)
        } else {
            itemView.chat_image.loadVideoMark(dataUrl, dataThumbImage,1 )
        }
    }
}