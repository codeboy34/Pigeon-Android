package com.pigeonmessenger.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.avatarFile
import com.pigeonmessenger.extension.loadAvatar
import kotlinx.android.synthetic.main.view_title.view.*


class TitleView : CardView {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_title, this, true)
    }


    fun audioCallClickListener(clickListener: OnClickListener) {
        audiocall_ib.setOnClickListener(clickListener)
    }

    fun setOnUserClickListener(onClickListener: OnClickListener) {
        title_wrapper.setOnClickListener(onClickListener)
        avatar_iv.setOnClickListener(onClickListener)
    }

    fun renderUser(recipient: User) {
        title_tv.text = recipient.getName()
        avatar_iv.loadAvatar(context.avatarFile(recipient.userId), recipient.thumbnail, R.drawable.avatar_contact)
    }

    fun renderGroup(group: Conversation) {
        title_tv.text = group.name
        avatar_iv.loadAvatar(context.avatarFile(group.conversationId), group.groupIconThumbnail, R.drawable.ic_groupme)
    }

    companion object {
        val POS_TEXT = 1
    }
}