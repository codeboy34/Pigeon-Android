package com.pigeonmessenger.adapter.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.extension.dpToPx
import com.pigeonmessenger.extension.formatMillis
import com.pigeonmessenger.utils.AudioPlayer
import com.pigeonmessenger.vo.MediaStatus
import com.pigeonmessenger.vo.MessageItem
import com.pigeonmessenger.widget.timeAgoClock
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_audio.view.*
import org.jetbrains.anko.textResource
import kotlin.math.min

class AudioHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    companion object {
        private const val TAG ="AudioHolder"
    }
    init {
        itemView.chat_flag.visibility = View.GONE
    }

    private val maxWidth by lazy {
        itemView.context.dpToPx(255f)
    }

    private val minWidth by lazy {
        itemView.context.dpToPx(180f)
    }

    private val dp15 by lazy {
        itemView.context.dpToPx(15f)
    }

    fun bind(
            messageItem: MessageItem,
            isFirst: Boolean,
            isLast: Boolean,
            hasSelect: Boolean,
            isSelect: Boolean,
            onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        val isMe = meId == messageItem.senderId

        chatLayout(isMe, isLast)

        itemView.chat_name.visibility = View.GONE

        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            itemView.audio_duration.textResource = R.string.chat_expired
        } else {
            itemView.audio_duration.text = messageItem.mediaDuration?.toLong()?.formatMillis()
        }

        messageItem.mediaDuration?.let {
            (it.toLong()).let {
                itemView.chat_layout.layoutParams.width = min((minWidth + (it / 1000f) * dp15).toInt(), maxWidth)
            }
        }
        setStatusIcon(isMe, messageItem.mediaStatus!!, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })
        messageItem.mediaWaveform?.let {
            itemView.audio_waveform.setWaveform(it)
        }
        if (AudioPlayer.get().isLoaded(messageItem.id)) {
            itemView.audio_waveform.setProgress(AudioPlayer.get().progress)
        } else {
            itemView.audio_waveform.setProgress(0f)
        }
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.audio_expired.visibility = View.VISIBLE
                    itemView.audio_progress.visibility = View.INVISIBLE
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.PENDING.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.enableLoading()
                    itemView.audio_progress.setBindOnly(messageItem.id)
                    itemView.audio_progress.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.DONE.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.setBindOnly(messageItem.id)
                    itemView.audio_waveform.setBind(messageItem.id)
                    if (AudioPlayer.get().isPlay(messageItem.id)) {
                        itemView.audio_progress.setPause()
                    } else {
                        itemView.audio_progress.setPlay()
                    }
                    itemView.audio_progress.setOnClickListener {
                        if (!hasSelect) {
                            if (AudioPlayer.get().isPlay(messageItem.id)) {
                                AudioPlayer.get().pause()
                            } else {
                                AudioPlayer.get().play(messageItem)
                            }
                        }
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }

                    itemView.setOnClickListener {
                        if (!hasSelect) {
                            if (AudioPlayer.get().isPlay(messageItem.id)) {
                                AudioPlayer.get().pause()
                            } else {
                                AudioPlayer.get().play(messageItem)
                            }
                        }
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.CANCELED.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    if (isMe) {
                        itemView.audio_progress.enableUpload()
                    } else {
                        itemView.audio_progress.enableDownload()
                    }
                    itemView.audio_progress.setBindOnly(messageItem.id)
                    itemView.audio_progress.setProgress(-1)
                    itemView.audio_progress.setOnClickListener {
                        if (isMe) {
                            onItemListener.onRetryUpload(messageItem.id)
                        } else {
                            onItemListener.onRetryDownload(messageItem.id)
                        }
                    }
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
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
    }

    private fun handlerClick(
            hasSelect: Boolean,
            isSelect: Boolean,
            isMe: Boolean,
            messageItem: MessageItem,
            onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect) {
            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
        } else if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
            if (isMe) {
                onItemListener.onRetryUpload(messageItem.id)
            } else {
                onItemListener.onRetryDownload(messageItem.id)
            }
        } else if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
            onItemListener.onCancel(messageItem.id)
        } else if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
        } else {
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            }
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
               itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            } else {
               itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }
}