package com.pigeonmessenger.adapter.holder

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.vo.MessageItem
import com.pigeonmessenger.vo.SystemConversationAction
import com.pigeonmessenger.vo.participantName
import com.pigeonmessenger.vo.senderName
import kotlinx.android.synthetic.main.item_chat_action.view.*

class ActionHolder(containerView: View) : BaseViewHolder(containerView) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
    }

    companion object {
        const val TAG = "ActionHolder"
    }

    var context: Context = itemView.context
    private fun getText(id: Int) = context.getText(id).toString()

    fun bind(
            messageItem: MessageItem,
            hasSelect: Boolean,
            isSelect: Boolean,
            onItemListener: ConversationAdapter.OnItemListener
    ) {
        val id = meId

        Log.d(TAG, ": ${messageItem}");
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
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

        when (messageItem.action) {
            SystemConversationAction.CREATE.name -> {
                itemView.chat_info.text =
                        String.format(getText(R.string.chat_group_create),
                                if (id == messageItem.senderId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    messageItem.senderName()
                                }, messageItem.groupName)
            }
            SystemConversationAction.ADD.name -> {
                itemView.chat_info.text =
                        String.format(getText(R.string.chat_group_add),
                                if (id == messageItem.senderId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    messageItem.senderName()
                                },
                                if (id == messageItem.participantId) {
                                    getText(R.string.chat_you)
                                } else {
                                    messageItem.participantName()
                                })
            }
            SystemConversationAction.REMOVE.name -> {
                itemView.chat_info.text =
                        String.format(getText(R.string.chat_group_remove),
                                if (id == messageItem.senderId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    messageItem.senderName()
                                },
                                if (id == messageItem.participantId) {
                                    getText(R.string.chat_you)
                                } else {
                                    messageItem.participantName()
                                })
            }
            SystemConversationAction.JOIN.name -> {
                itemView.chat_info.text =
                        String.format(getText(R.string.chat_group_join),
                                if (id == messageItem.participantId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    messageItem.participantName()
                                })
            }
            SystemConversationAction.EXIT.name -> {
                itemView.chat_info.text =
                        String.format(getText(R.string.chat_group_exit),
                                if (id == messageItem.participantId) {
                                    getText(R.string.chat_you_start)
                                } else {
                                    messageItem.participantName()
                                })
            }
            SystemConversationAction.ROLE.name -> {
                itemView.chat_info.text = getText(R.string.group_role)
            }

            SystemConversationAction.UPDATE_NAME.name -> {
                itemView.chat_info.text = String.format(getText(R.string.group_name),
                        if (id == messageItem.senderId) {
                            getText(R.string.chat_you_start)
                        } else {
                            messageItem.senderName()
                        })
            }
            SystemConversationAction.UPDATE_ICON.name -> {
                itemView.chat_info.text = String.format(getText(R.string.group_icon),
                        if (id == messageItem.senderId) {
                            getText(R.string.chat_you_start)
                        } else {
                            messageItem.senderName()
                        })
            }
            else -> {
                itemView.chat_info.text = getText(R.string.unknown)
            }
        }
    }
}