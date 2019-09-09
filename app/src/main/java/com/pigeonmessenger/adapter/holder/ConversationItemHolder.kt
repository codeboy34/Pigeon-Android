package com.pigeonmessenger.adapter.holder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.adapter.ConversationListAdapter.OnItemClickListener
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.viewmodals.ConversationItem
import com.pigeonmessenger.viewmodals.isCallMessage
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.vo.MessageStatus
import com.pigeonmessenger.vo.SystemConversationAction
import com.pigeonmessenger.widget.timeAgo
import kotlinx.android.synthetic.main.item_list_conversation.view.*
import org.jetbrains.anko.textColor

@Suppress("IMPLICIT_CAST_TO_ANY")
class ConversationItemHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    var context: Context = itemView.context
    private fun getText(id: Int) = context.getText(id).toString()

    private val tickColor by lazy {
        ContextCompat.getColor(App.get(), R.color.color_chat_date)
    }

    private val readTickColor by lazy {
        ContextCompat.getColor(App.get(), R.color.pigeonActionColor)
    }

    companion object {
        private const val TAG = "ConItemHolder"
    }

    fun bind(onItemClickListener: OnItemClickListener?, position: Int, conversationItem: ConversationItem) {
        val id = Session.getUserId()

        itemView.name_tv.text = conversationItem.name()

        when {
            conversationItem.messageStatus == MessageStatus.FAILED.name -> {
                conversationItem.message?.let {
                    itemView.msg_tv.setText(R.string.conversation_waiting)
                }
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_fail)
            }
            conversationItem.messageType == MessageCategory.SIGNAL_TEXT.name ||
                    conversationItem.messageType == MessageCategory.PLAIN_TEXT.name -> {
                conversationItem.message?.let {
                    //itemView.msg_tv.text
                    itemView.msg_tv.text = it
                }
                null
            }

            conversationItem.messageType == MessageCategory.SIGNAL_STICKER.name ||
                    conversationItem.messageType == MessageCategory.PLAIN_STICKER.name -> {
                //itemView.msg_tv.setText(R.string.conversation_status_sticker)
                setConversationName(conversationItem)
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_stiker)
            }
            conversationItem.messageType == MessageCategory.SIGNAL_IMAGE.name ||
                    conversationItem.messageType == MessageCategory.PLAIN_IMAGE.name -> {
                setConversationName(conversationItem)
                itemView.msg_tv.setText(R.string.conversation_status_pic)

                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_pic)
            }
            conversationItem.messageType == MessageCategory.SIGNAL_VIDEO.name ||
                    conversationItem.messageType == MessageCategory.PLAIN_VIDEO.name -> {
                setConversationName(conversationItem)
                itemView.msg_tv.setText(R.string.conversation_status_video)
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_video)
            }
            conversationItem.messageType == MessageCategory.SIGNAL_DATA.name ||
                    conversationItem.messageType == MessageCategory.PLAIN_DATA.name -> {
                setConversationName(conversationItem)
                itemView.msg_tv.setText(R.string.conversation_status_file)
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_file)
            }
            conversationItem.messageType == MessageCategory.SIGNAL_AUDIO.name ||
                    conversationItem.messageType == MessageCategory.PLAIN_AUDIO.name -> {
                setConversationName(conversationItem)
                itemView.msg_tv.setText(R.string.conversation_status_audio)
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_audio)
            }

            conversationItem.messageType == MessageCategory.SIGNAL_CONTACT.name ||
                    conversationItem.messageType == MessageCategory.PLAIN_CONTACT.name -> {
                setConversationName(conversationItem)
                itemView.msg_tv.setText(R.string.contact_less_title)
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_contact)
            }

            conversationItem.isCallMessage() -> {
                setConversationName(conversationItem)
                itemView.msg_tv.setText(R.string.conversation_status_voice)
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_voice)
            }

            conversationItem.messageType == MessageCategory.SYSTEM_CONVERSATION.name -> {
                when (conversationItem.action) {
                    SystemConversationAction.CREATE.name -> {
                        itemView.msg_tv.text =
                                String.format(getText(R.string.chat_group_create),
                                        if (id == conversationItem.senderId) {
                                            getText(R.string.chat_you_start)
                                        } else {
                                            conversationItem.name()
                                        }, conversationItem.groupName)
                    }
                    SystemConversationAction.ADD.name -> {
                        itemView.msg_tv.text =
                                String.format(getText(R.string.chat_group_add),
                                        if (id == conversationItem.senderId) {
                                            getText(R.string.chat_you_start)
                                        } else {
                                            conversationItem.participantName()
                                        },
                                        if (id == conversationItem.participantId) {
                                            getText(R.string.chat_you)
                                        } else {
                                            conversationItem.participantName()
                                        })
                    }
                    SystemConversationAction.REMOVE.name -> {
                        itemView.msg_tv.text =
                                String.format(getText(R.string.chat_group_remove),
                                        if (id == conversationItem.senderId) {
                                            getText(R.string.chat_you_start)
                                        } else {
                                            conversationItem.senderName()
                                        },
                                        if (id == conversationItem.participantId) {
                                            getText(R.string.chat_you)
                                        } else {
                                            conversationItem.participantName()
                                        })
                    }
                    SystemConversationAction.JOIN.name -> {
                        itemView.msg_tv.text =
                                String.format(getText(R.string.chat_group_join),
                                        if (id == conversationItem.participantId) {
                                            getText(R.string.chat_you_start)
                                        } else {
                                            conversationItem.participantName()
                                        })
                    }
                    SystemConversationAction.EXIT.name -> {
                        itemView.msg_tv.text =
                                String.format(getText(R.string.chat_group_exit),
                                        if (id == conversationItem.participantId) {
                                            getText(R.string.chat_you_start)
                                        } else {
                                            conversationItem.participantName()
                                        })
                    }
                    SystemConversationAction.ROLE.name -> {
                        itemView.msg_tv.text = String.format(getText(R.string.group_icon),
                                if (id == conversationItem.participantId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    conversationItem.senderName()
                                })
                    }

                    SystemConversationAction.UPDATE_ICON.name -> {
                        itemView.msg_tv.text = String.format(getText(R.string.group_icon),
                                if (id == conversationItem.participantId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    conversationItem.senderName()
                                })
                    }

                    SystemConversationAction.UPDATE_NAME.name -> {
                        itemView.msg_tv.text = String.format(getText(R.string.group_name),
                                if (id == conversationItem.participantId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    conversationItem.senderName()
                                })
                    }
                    else -> {
                        itemView.msg_tv.text = ""
                    }
                }
                null
            }
            else -> null
        }.also {
             it?.setBounds(0, 0, itemView.context.dpToPx(12f), itemView.context.dpToPx(12f))
             TextViewCompat.setCompoundDrawablesRelative(itemView.msg_tv, it, null, null, null)
        }


        if (conversationItem.senderId == Session.getUserId() &&
                conversationItem.messageType != MessageCategory.SYSTEM_CONVERSATION.name
                &&
                !conversationItem.isCallMessage()) {
            when (conversationItem.messageStatus) {
                MessageStatus.SENDING.name ->
                    AppCompatResources.getDrawable(itemView.context,
                            R.drawable.msg_status_gray_waiting)?.apply {
                        this.setTint(tickColor)
                    }
                MessageStatus.SENT.name ->
                    AppCompatResources.getDrawable(itemView.context,
                            R.drawable.msg_status_server_receive)?.apply {
                        this.setTint(tickColor)
                    }
                MessageStatus.DELIVERED.name -> AppCompatResources.getDrawable(itemView.context,
                        R.drawable.msg_status_client_received)?.apply {
                    this.setTint(tickColor)
                }
                MessageStatus.READ.name ->
                    AppCompatResources.getDrawable(itemView.context, R.drawable.msg_status_client_received)?.apply {
                        this.setTint(readTickColor)
                    }
                else -> {
                    AppCompatResources.getDrawable(itemView.context,
                            R.drawable.msg_status_gray_waiting)?.apply {
                        this.setTint(tickColor)
                    }
                }
            }.also {
               // it?.setBounds(0, 0, itemView.context.dpToPx(12f), itemView.context.dpToPx(12f))
                itemView.msg_flag.setImageDrawable(it)
                itemView.msg_flag.visibility = VISIBLE
            }
        } else {
            itemView.msg_flag.visibility = GONE
        }


        conversationItem.createdAt?.let {
            itemView.time_tv.timeAgo(it)
        }
        notEmptyOrElse(conversationItem.unseenCount,
                {
                    itemView.unread_tv.text = "$it"; itemView.unread_tv.visibility = VISIBLE;
                    // itemView.name_tv.textColor = Color.BLACK
                    itemView.msg_tv.textColor = Color.BLACK
                    itemView.time_tv.textColor = Color.BLACK
                    itemView.time_tv.textColor = ContextCompat.getColor(context, R.color.pigeonActionColor)
                },
                {
                    //   itemView.name_tv.textColor = ContextCompat.getColor(context, R.color.secondaryTextColor)
                    itemView.msg_tv.textColor = ContextCompat.getColor(context, R.color.text_gray)
                    itemView.time_tv.textColor = ContextCompat.getColor(context, R.color.text_gray)
                    itemView.time_tv.textColor = ContextCompat.getColor(context, R.color.text_gray)
                    itemView.unread_tv.visibility = GONE
                }
        )

        itemView.mute_iv.visibility = if (conversationItem.isMute()) VISIBLE else GONE
        notNullElse(conversationItem.pinTime, { itemView.msg_pin.visibility = VISIBLE }, { itemView.msg_pin.visibility = GONE })

        if (!conversationItem.isGroup()) itemView.avatar_iv.loadAvatar(context.avatarFile(conversationItem.ownerId!!),
                conversationItem.avatarThumbnail, R.drawable.avatar_contact)
        else itemView.avatar_iv.loadAvatar((context.avatarFile(conversationItem.conversationId)),
                conversationItem.groupThumbnail, R.drawable.ic_groupme)

        itemView.setOnClickListener { onItemClickListener?.click(position, conversationItem) }
        itemView.setOnLongClickListener {
            notNullElse(onItemClickListener, { it.longClick(conversationItem) }, false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setConversationName(conversationItem: ConversationItem) {
        if (conversationItem.isGroup() && conversationItem.senderId != Session.getUserId()) {
            itemView.group_name_tv.text = "${conversationItem.senderName()}: "
            itemView.group_name_tv.visibility = View.VISIBLE
        } else {
            itemView.group_name_tv.visibility = View.GONE
        }
    }
}
