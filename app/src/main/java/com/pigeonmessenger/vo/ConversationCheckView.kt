package com.pigeonmessenger.vo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.LinearLayout
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ForwardAdapter
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.viewmodals.ConversationItem
import kotlinx.android.synthetic.main.view_conversation_check.view.*

class ConversationCheckView : LinearLayout, Checkable {
    private var checked = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.view_conversation_check, this, true)
        setOnClickListener {
            toggle()
        }
    }

    override fun setChecked(b: Boolean) {
        checked = b
        if (b) {
            check_iv.visibility = View.VISIBLE
            layout.setBackgroundColor(Color.parseColor("#F5F5F5"))
        } else {
            check_iv.visibility = View.GONE
            layout.setBackgroundColor(Color.WHITE)
        }
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        isChecked = !isChecked
    }

    fun bind(item: ConversationItem, listener: ForwardAdapter.ForwardListener?) {
        normal.text = item.name()
        if (item.isGroup()) avatar.setGroup(item.conversationId, item.groupThumbnail)
        else avatar.setUserAvatar(item.ownerId!!, item.avatarThumbnail)
        setOnClickListener {
            toggle()
            listener?.onConversationItemClick(item)
        }
    }

    fun bind(item: User, listener: ForwardAdapter.ForwardListener?) {
        normal.text = item.getName()
        avatar.setUserAvatar(item.userId, item.thumbnail)
        setOnClickListener {
            toggle()
            listener?.onUserItemClick(item)
        }
    }
}
