package com.pigeonmessenger.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.daos.ContactsDao
import com.pigeonmessenger.database.room.daos.ConversationDao
import com.pigeonmessenger.extension.*
import kotlinx.android.synthetic.main.activity_avatar_preview.*
import javax.inject.Inject

class AvatarPreviewActivity : AppCompatActivity() {

    @Inject
    lateinit var contactsDao: ContactsDao

    @Inject
    lateinit var conversationDao: ConversationDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avatar_preview)

        App.get().appComponent.inject(this)

        val userId = intent.getStringExtra(USER_ID)
        notNullElse(userId, {
            contactsDao.findLiveUser(userId).observe(this, Observer { user ->
                val avatarFile = avatarFile(userId)
                if (avatarFile.exists() && avatarFile.length() > 0) {
                    avatar_iv.loadImage(avatarFile.absolutePath, R.drawable.ic_user_round, avatarFile.length().toString())
                } else if (user.thumbnail != null) {
                    avatar_iv.loadThumbnailDrawable(user.thumbnail, R.drawable.ic_groupme)
                } else {
                    avatar_iv.loadPlaceHolder(R.drawable.ic_groupme)
                }
            })
        }, {
            val conversationId = intent.getStringExtra(CONVERSATION_ID)
            conversationDao.getConversationById(conversationId).observe(this@AvatarPreviewActivity, Observer { conversation ->

                val iconUrl = getGroupAvatarPath(conversationId)
                if (iconUrl.exists() && iconUrl.length() > 0) {
                    avatar_iv.loadImage(iconUrl.toUri(), R.drawable.ic_groupme, iconUrl.length().toString())
                } else if (conversation.groupIconThumbnail != null) {
                    avatar_iv.loadThumbnailDrawable(conversation.groupIconThumbnail, R.drawable.ic_groupme)
                } else {
                    avatar_iv.loadPlaceHolder(R.drawable.ic_groupme)
                }
            })
        })
    }

    companion object {
        const val isGroup = false
        private const val USER_ID = "USER_ID"
        private const val CONVERSATION_ID = "CONVERSATION_ID"
        fun show(context: Context, userId: String? = null, conversationId: String? = null) {
            val intent = Intent(context, AvatarPreviewActivity::class.java)
            intent.putExtra(USER_ID, userId)
            intent.putExtra(CONVERSATION_ID, conversationId)
            context.startActivity(intent)
        }
    }
}
