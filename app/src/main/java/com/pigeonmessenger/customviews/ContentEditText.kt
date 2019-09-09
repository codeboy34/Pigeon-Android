package com.pigeonmessenger.customviews

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.vanniktech.emoji.EmojiEditText

class ContentEditText : EmojiEditText, TextWatcher {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    // constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var listener: OnCommitContentListener? = null

    private val mimeTypes = arrayOf(MimeType.PNG.toString(), MimeType.GIF.toString(), MimeType.JPEG.toString(), MimeType.WEBP.toString())
    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic = super.onCreateInputConnection(editorInfo)
        if (listener == null) {
            return ic
        }

        EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes)
        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0)) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    return@OnCommitContentListener false
                }
            }

            var supported = false
            for (mimeType in mimeTypes) {
                if (inputContentInfo.description.hasMimeType(mimeType)) {
                    supported = true
                    break
                }
            }
            if (!supported) {
                return@OnCommitContentListener false
            }
            if (this.listener != null) {
                return@OnCommitContentListener this.listener!!.onCommitContent(inputContentInfo, flags, opts)
            }
            return@OnCommitContentListener false
        }
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }

    fun setCommitContentListener(listener: OnCommitContentListener) {
        this.listener = listener
    }


    private var isTyping = false
    var typingListener: OnTypingListener? = null
    private val STOP_DELAY = 3000L
    private val DISPATCH_DELAY = 5000L
    private val lock: Any = Any()

    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (!text.isNullOrEmpty() && text.trim().isNotBlank()) {
            synchronized(lock) {
                if (!isTyping) {
                    typingListener?.onDispatchTyping()
                    isTyping = true
                    postDelayed(typingEventDispatcher, DISPATCH_DELAY)
                }
                removeCallbacks(typingRunnable)
                postDelayed(typingRunnable, STOP_DELAY)
            }
        }
    }

    private val typingRunnable = Runnable {
        synchronized(lock) {
            isTyping = false
        }
    }

    fun stopTyping() {
        synchronized(lock) {
            isTyping = false
            removeCallbacks(typingEventDispatcher)
            removeCallbacks(typingRunnable)
        }
    }

    private val typingEventDispatcher = object : Runnable {
        override fun run() {
            synchronized(lock) {
                if (isTyping) {
                    typingListener?.onDispatchTyping()
                    postDelayed(this, DISPATCH_DELAY)
                }
            }
        }
    }

    interface OnCommitContentListener {
        fun onCommitContent(inputContentInfo: InputContentInfoCompat?, flags: Int, opts: Bundle?): Boolean
    }

    interface OnTypingListener {
        fun onDispatchTyping()
    }

}