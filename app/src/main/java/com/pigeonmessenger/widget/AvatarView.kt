package com.pigeonmessenger.widget

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.net.toUri
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.*
import kotlinx.android.synthetic.main.view_avatar.view.*

class AvatarView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_avatar, this, true)
    }

    companion object {
        private const val TAG = "AvatarView"
    }

    fun setGroup(url: Uri?) {
        avatar_simple.loadImage(url, R.drawable.ic_groupme, "")
        // Glide.with(this)
        //   .load(url)
        // .apply(RequestOptions().dontAnimate().placeholder(R.drawable.ic_group_place_holder))
        //  .into(avatar_simple)
    }

    fun setGroup(conversationId: String, thumbnail: String?) {
        val iconUrl = context.getGroupAvatarPath(conversationId)
        if (iconUrl.exists() && iconUrl.length() > 0) {
            avatar_simple.loadImage(iconUrl.toUri(), R.drawable.ic_groupme, iconUrl.length().toString())
        } else if (thumbnail != null) {
            avatar_simple.loadThumbnailDrawable(thumbnail, R.drawable.ic_groupme)
        } else {
            avatar_simple.loadPlaceHolder(R.drawable.ic_groupme)
        }
    }

    fun setUserAvatar(userId:String,thumbnail: String?){
        val avatarFile = context.avatarFile(userId)
        if (avatarFile.exists() && avatarFile.length()>0){
            avatar_simple.loadImage(avatarFile.absolutePath, R.drawable.ic_groupme, avatarFile.length().toString())
        }else if (thumbnail!=null){
            avatar_simple.loadThumbnailDrawable(thumbnail,R.drawable.ic_groupme)
        }else{
            avatar_simple.loadPlaceHolder(R.drawable.ic_groupme)
        }
    }

    fun setUrl(url: String?, placeHolder: Int) {

        avatar_simple.loadCircleImage(url, placeHolder)
    }

    fun setOwnInfo( avatarBase: String?, thumbnail: String?) {

        if (avatarBase != null && avatarBase.isNotEmpty()) {
            avatar_simple.loadBaseAvatar(avatarBase, thumbnail)
        } else {
            avatar_simple.loadPlaceHolder(R.drawable.ic_user_round)
        }
    }

    fun setInfoWithThumbnail(name: String?, thumbnail: String?, url: String?) {
        //avatar_tv.text = checkEmoji(name)
        if (url != null && url.isNotEmpty()) {
            val avatarUrl = context!!.getAvatarFileDir(url)
            avatar_simple.loadImage(avatarUrl.absolutePath, R.drawable.ic_user_round, avatarUrl.length().toString())

        } else if (thumbnail != null) {
            avatar_simple.loadThumbnailDrawable(thumbnail, R.drawable.ic_user_round)
        } else {
            avatar_simple.loadPlaceHolder(R.drawable.ic_user_round)
        }
    }
}