package com.pigeonmessenger.adapter.holder

import android.view.View
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.extension.formatMillis
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.vo.MessageItem
import kotlinx.android.synthetic.main.item_chat_call.view.*

class CallHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
       // itemView.chat_flag.visibility = GONE
    }

    fun bind(
            messageItem: MessageItem,
            isLast: Boolean,
            hasSelect: Boolean,
            isSelect: Boolean,
            onItemListener: ConversationAdapter.OnItemListener
    ) {
        val ctx = itemView.context
        val isMe = meId == messageItem.senderId
       // itemView.chat_time.timeAgoClock(messageItem.createdAt)
        itemView.call_tv.text = when (messageItem.type) {
            MessageCategory.WEBRTC_VIDEO_CANCEL.name -> {
                if (isMe) {
                    ctx.getString(R.string.video_call_canceled)
                } else {
                    ctx.getString(R.string.video_call_canceled_by_caller)
                }
            }
            MessageCategory.WEBRTC_VIDEO_DECLINE.name -> {
                if (isMe) {
                    ctx.getString(R.string.video_call_declined_other)
                } else {
                    ctx.getString(R.string.video_call_declined)
                }
            }
            MessageCategory.WEBRTC_VIDEO_END.name -> {
                val duration = messageItem.mediaDuration?.toLong()?.formatMillis()
                ctx.getString(R.string.video_call_duration, duration)
            }
            MessageCategory.WEBRTC_VIDEO_BUSY.name -> {
                if (isMe) {
                    ctx.getString(R.string.video_call_remote_busy)
                } else {
                    ctx.getString(R.string.video_call_local_busy)
                }
            }
            MessageCategory.WEBRTC_VIDEO_FAILED.name -> {
                ctx.getString(R.string.video_call_failed)
            }
            MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                if (isMe) {
                    ctx.getString(R.string.audio_call_canceled)
                } else {
                    ctx.getString(R.string.audio_call_canceled_by_caller)
                }
            }
            MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                if (isMe) {
                    ctx.getString(R.string.audio_call_declined_other)
                } else {
                    ctx.getString(R.string.audio_call_declined)
                }
            }
            MessageCategory.WEBRTC_AUDIO_END.name -> {
                val duration = messageItem.mediaDuration?.toLong()?.formatMillis()
                ctx.getString(R.string.audio_call_duration, duration)
            }
            MessageCategory.WEBRTC_AUDIO_BUSY.name -> {
                if (isMe) {
                    ctx.getString(R.string.audio_call_local_busy)
                } else {
                    ctx.getString(R.string.audio_call_local_busy)
                }
            }
            else -> {
                ctx.getString(R.string.audio_call_failed)
            }
        }
    }
}