package com.pigeonmessenger.manager

import com.pigeonmessenger.activities.App
import org.jetbrains.anko.runOnUiThread
import java.util.*

class TypingManager {

    companion object {
        private const val STOP_DELAY = 5000
    }
    private val typingUserList = arrayListOf<TypingUser>()

    var typingListener: OnStartTypingListener? = null

    fun onTypingEvent(conversationId: String, userId: String) {
        val typingUser = typingUserList.find { it.conversationId == conversationId }
        if (typingUser != null) {
            if (typingUser.userId == userId)
                typingUser.restart()
        } else {
            typingUserList.add(TypingUser(conversationId, userId) {
                typingUserList.remove(typingUserList.find { it.conversationId == conversationId })
                if (App.conversationWith == conversationId) {
                    App.get().runOnUiThread { typingListener?.onStopTyping() }
                }
            })
            if (App.conversationWith == conversationId) {
                App.get().runOnUiThread { typingListener?.onStartTyping(userId) }
            }
        }
    }

    fun getTypingUser(conversationId: String): String? {
        return typingUserList.find { it.conversationId == conversationId }?.userId
    }

    fun onNewChatMessage(conversationId: String, userId: String) {
        val typingUser = typingUserList.find { it.conversationId == conversationId }
        if (typingUser != null) {
            if (typingUser.userId == userId) {
                typingUser.stop()
            }
        }
    }

    inner class TypingUser(val conversationId: String, val userId: String, private var action: (userId: String) -> Unit) {

        private var timer: Timer? = null

        init {
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    action(conversationId)
                }
            }, 5000)
        }

        fun stop() {
            timer?.cancel()
            action(conversationId)
        }

        fun restart() {
            timer?.cancel()
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    action(conversationId)
                }
            }, 5000)
        }
    }

    interface OnStartTypingListener {
        fun onStartTyping(userId: String)
        fun onStopTyping()
    }
}