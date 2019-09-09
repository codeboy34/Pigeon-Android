package com.pigeonmessenger.utils

import android.content.Intent
import android.net.Uri
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.extension.getFilePath
import com.pigeonmessenger.vo.ForwardCategory
import com.pigeonmessenger.vo.ForwardMessage
import com.pigeonmessenger.vo.addTo

class ShareHelper {

    companion object {
        @Volatile
        private var INSTANCE: ShareHelper? = null

        fun get(): ShareHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShareHelper().also { INSTANCE = it }
            }
    }

    fun generateForwardMessageList(intent: Intent): ArrayList<ForwardMessage>? {
        val action = intent.action
        val type = intent.type
        if (action == null || type == null) {
            return null
        }
        val result = arrayListOf<ForwardMessage>()
        if (Intent.ACTION_SEND == action) {
            if ("text/plain" == type) {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ForwardMessage(ForwardCategory.TEXT.name, content = text).addTo(result)
            } else if (type.startsWith("image/")) {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                generateShareMessage(imageUri)?.addTo(result)
            } else if (type.startsWith("video/")) {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                generateShareMessage(imageUri, ForwardCategory.VIDEO.name)?.addTo(result)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            if (type.startsWith("image/")) {
                val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                for (item in imageUriList) {
                    generateShareMessage(item)?.addTo(result)
                }
            } else if (type.startsWith("video/")) {
                val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                for (item in imageUriList) {
                    generateShareMessage(item, ForwardCategory.VIDEO.name)?.addTo(result)
                }
            }
        }
        return result
    }

    private fun generateShareMessage(imageUri: Uri?, type: String = ForwardCategory.IMAGE.name): ForwardMessage? {
        if (imageUri == null) {
            return null
        }
        return ForwardMessage(type,
            mediaUrl = imageUri.getFilePath(App.get()))
    }
}