package com.pigeonmessenger.widget

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import com.pigeonmessenger.R
import com.pigeonmessenger.customviews.ContentEditText
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.fragment.settings.PREF_IS_ENTER_SEND
import com.pigeonmessenger.widget.audio.SlidePanelView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.vanniktech.emoji.EmojiPopup
import kotlinx.android.synthetic.main.view_chat_controlview.view.*
import org.jetbrains.anko.dip


@SuppressLint("CheckResult")
class ChatControlView : FrameLayout {

    companion object {
        const val REPLY = -1
        const val SEND = 0
        const val AUDIO = 1
        const val VIDEO = 2
        const val UP = 3
        const val DOWN = 4

        const val STICKER = 0
        const val KEYBOARD = 1

        const val RECORD_DELAY = 200L
        const val RECORD_TIP_MILLIS = 2000L
    }

    lateinit var callback: Callback
    lateinit var recordTipView: View

    private var sendStatus = AUDIO
        set(value) {
            if (value == field) return

            field = value
            checkSend()
        }
    private var stickerStatus = STICKER
        set(value) {
            if (value == field) return

            field = value
            checkSticker()
        }

    private var lastSendStatus = AUDIO
    private var isUp = true

    var isRecording = false

    var activity: Activity? = null
    private lateinit var recordCircle: RecordCircleView
    lateinit var cover: View
    private var upBeforeGrant = false
    private var keyboardShown = false

    private var emojiPopup: EmojiPopup? = null

    private val sendDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_send, null) }
    private val audioDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_record_mic_black, null) }
    private val audioActiveDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_record_mic_blue, null) }
    private val videoDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_record_mic_black, null) }
    private val upDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_arrow_up, null) }
    private val downDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_arrow_down, null) }

    private val stickerDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_sticket, null) }
    private val keyboardDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_keyboard, null) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_chat_controlview, this, true)

        chat_et.addTextChangedListener(editTextWatcher)
        chat_send_ib.setOnTouchListener(sendOnTouchListener)
        chat_sticker_ib.setOnClickListener(stickerClickListener)
        chat_slide.callback = chatSlideCallback
        chat_et.typingListener = typingDispatchListener

        if (context.defaultSharedPreferences.getBoolean(PREF_IS_ENTER_SEND, false)) {
            chat_et.imeOptions = EditorInfo.IME_ACTION_SEND
            chat_et.setOnEditorActionListener { v, actionId, event ->

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    clickSend()
                    true
                }
                false
            }
        }


    }

    fun setCircle(record_circle: RecordCircleView) {
        recordCircle = record_circle
        recordCircle.callback = recordCircleCallback
    }

    fun setSend() {
        if (sendStatus == REPLY) {
            return
        }
        if (!post(safeSetSendRunnable)) {
            realSetSend()
        }
    }

    fun setEmojiPopup(rootView: View) {
        emojiPopup = EmojiPopup.Builder.fromRootView(rootView).build(chat_et)
    }


    fun reset() {
        stickerStatus = STICKER
        isUp = true
        setSend()
    }

    fun cancelExternal() {
        removeCallbacks(recordRunnable)
        cleanUp()
        updateRecordCircleAndSendIcon()
        chat_slide.parent.requestDisallowInterceptTouchEvent(false)
    }

    fun updateUp(up: Boolean) {
        isUp = up
        setSend()
    }

    private fun checkSend() {
        val d = when (sendStatus) {
            REPLY -> sendDrawable
            SEND -> sendDrawable
            AUDIO -> if (isRecording) audioActiveDrawable else audioDrawable
            VIDEO -> videoDrawable
            UP -> upDrawable
            DOWN -> downDrawable
            else -> throw IllegalArgumentException("error send status")
        }
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        chat_send_ib.setImageDrawable(d)
    }

    private fun checkSticker() {
        val d = when (stickerStatus) {
            STICKER -> stickerDrawable
            KEYBOARD -> keyboardDrawable
            else -> null
        }
        d?.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        chat_sticker_ib.setImageDrawable(d)
    }

    private fun cleanUp(locked: Boolean = false) {
        startX = 0f
        originX = 0f
        isUp = true
        if (!locked) {
            isRecording = false
        }
        checkSend()
    }

    private fun handleCancelOrEnd(cancel: Boolean) {
        if (cancel) callback.onRecordCancel() else callback.onRecordEnd()
        cleanUp()
        updateRecordCircleAndSendIcon()
    }

    private fun updateRecordCircleAndSendIcon() {
        if (isRecording) {
            recordCircle.visibility = View.VISIBLE
            recordCircle.setAmplitude(.0)
            ObjectAnimator.ofFloat(recordCircle, "scale", 1f).apply {
                interpolator = DecelerateInterpolator()
                duration = 200
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        recordCircle.visibility = View.VISIBLE
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        recordCircle.visibility = View.VISIBLE
                    }

                    override fun onAnimationStart(animation: Animator?) {

                    }

                })
            }.start()

            chat_send_ib.animate().setDuration(200).alpha(0f).start()
            chat_slide.onStart()
        } else {
            ObjectAnimator.ofFloat(recordCircle, "scale", 0f).apply {
                interpolator = AccelerateInterpolator()
                duration = 200
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        recordCircle.visibility = View.GONE
                        recordCircle.setSendButtonInvisible()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        recordCircle.visibility = View.GONE
                        recordCircle.setSendButtonInvisible()
                    }

                    override fun onAnimationStart(animation: Animator?) {

                    }

                    override fun onAnimationRepeat(animation: Animator?) {

                    }

                })
            }.start()
            chat_send_ib.animate().setDuration(200).alpha(1f).start()
            chat_slide.onEnd()
        }
    }

    private fun audioOrVideo() = sendStatus == AUDIO || sendStatus == VIDEO

    fun toggleKeyboard(shown: Boolean) {
        keyboardShown = shown
        if (shown) {
            isUp = true
            cover.alpha = 0f
            activity?.window?.statusBarColor = Color.TRANSPARENT
            stickerStatus = STICKER
        } else {
        }
        setSend()
    }

    private fun clickSend() {
        when (sendStatus) {
            SEND, REPLY -> {
                chat_et.text?.let {
                    callback.onSendClick(it.trim().toString())
                }
            }
            AUDIO -> {
                if (recordTipView.visibility == View.INVISIBLE) {
                    recordTipView.fadeIn()
                    postDelayed(hideRecordTipRunnable, RECORD_TIP_MILLIS)
                } else {
                    removeCallbacks(hideRecordTipRunnable)
                }
                postDelayed(hideRecordTipRunnable, RECORD_TIP_MILLIS)
            }
            VIDEO -> {
                sendStatus = AUDIO
                lastSendStatus = sendStatus
            }
            UP -> {
                callback.onUp()
            }
            DOWN -> {
                callback.onDown()
            }
        }
    }

    private fun realSetSend() {
        val editEmpty = chat_et.text.toString().trim().isEmpty()
        sendStatus = if (!editEmpty) {
            if (chat_more_ib.visibility != View.GONE) {
                chat_more_ib.visibility = View.GONE
            }
            SEND
        } else {
            if (chat_more_ib.visibility != View.VISIBLE) {
                chat_more_ib.visibility = View.VISIBLE
            }
            lastSendStatus
        }
    }

    private val stickerClickListener = OnClickListener {
        if (!emojiPopup!!.isShowing) {
            stickerStatus = STICKER
            // inputLayout.showSoftKey(chat_et)
        } else {
            stickerStatus = KEYBOARD
            //inputLayout.show(chat_et, stickerContainer)
            // callback.onStickerClick()

            //if (stickerStatus == KEYBOARD && inputLayout.isInputOpen &&
            //       sendStatus == AUDIO && lastSendStatus == AUDIO) {
            //  setSend()
            //}
        }
        emojiPopup!!.toggle()
    }

    private val editTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            setSend()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private val typingDispatchListener = object : ContentEditText.OnTypingListener {
        override fun onDispatchTyping() {
            callback.onDispatchTyping()
        }
    }
    private var startX = 0f
    private var originX = 0f
    private var startTime = 0L
    private var triggeredCancel = false
    private var hasStartRecord = false
    private var locked = false
    private var maxScrollX = context.dip(100f)
    var calling = false

    private val sendOnTouchListener = OnTouchListener { _, event ->
        if (calling && sendStatus == AUDIO) {
            callback.onCalling()
            return@OnTouchListener false
        }
        chat_send_ib.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (recordCircle.sendButtonVisible) {
                    return@OnTouchListener false
                }

                originX = event.rawX
                startX = event.rawX
                val w = chat_slide.slideWidth
                if (w > 0) {
                    maxScrollX = w
                }
                startTime = System.currentTimeMillis()
                hasStartRecord = false
                locked = false
                if (audioOrVideo()) {
                    postDelayed(recordRunnable, RECORD_DELAY)
                }
                return@OnTouchListener true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!audioOrVideo() || recordCircle.sendButtonVisible || !hasStartRecord) return@OnTouchListener false

                val x = recordCircle.setLockTranslation(event.y)
                if (x == 2) {
                    ObjectAnimator.ofFloat(recordCircle, "lockAnimatedTranslation",
                            recordCircle.startTranslation).apply {
                        duration = 150
                        interpolator = DecelerateInterpolator()
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationEnd(animation: Animator?) {
                                locked = true
                            }

                            override fun onAnimationCancel(animation: Animator?) {

                            }

                            override fun onAnimationStart(animation: Animator?) {

                            }

                            override fun onAnimationRepeat(animation: Animator?) {

                            }

                        })
                    }.start()
                    chat_slide.toCancel()
                    return@OnTouchListener false
                }

                val moveX = event.rawX
                if (moveX != 0f) {
                    chat_slide.slideText(startX - moveX)
                    if (originX - moveX > maxScrollX) {
                        removeCallbacks(recordRunnable)
                        handleCancelOrEnd(true)
                        chat_slide.parent.requestDisallowInterceptTouchEvent(false)
                        triggeredCancel = true
                        return@OnTouchListener false
                    }
                }
                startX = moveX
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (triggeredCancel) {
                    cleanUp()
                    triggeredCancel = false
                    return@OnTouchListener false
                }

                if (!hasStartRecord) {
                    removeCallbacks(recordRunnable)
                    removeCallbacks(checkReadyRunnable)
                    cleanUp()
                    if (!post(sendClickRunnable)) {
                        clickSend()
                    }
                } else if (hasStartRecord && !locked && System.currentTimeMillis() - startTime < 500) {
                    removeCallbacks(recordRunnable)
                    removeCallbacks(checkReadyRunnable)
                    // delay check sendButtonVisible
                    postDelayed({
                        if (!recordCircle.sendButtonVisible) {
                            handleCancelOrEnd(true)
                        } else {
                            recordCircle.sendButtonVisible = false
                        }
                    }, 200)
                    return@OnTouchListener false
                }

                if (isRecording && !recordCircle.sendButtonVisible) {
                    handleCancelOrEnd(event.action == MotionEvent.ACTION_CANCEL)
                } else {
                    cleanUp(true)
                }

                if (!callback.isReady()) {
                    upBeforeGrant = true
                }
            }
        }
        return@OnTouchListener true
    }

    private val safeSetSendRunnable = Runnable { realSetSend() }

    private val sendClickRunnable = Runnable { clickSend() }

    private val hideRecordTipRunnable = Runnable {
        if (recordTipView.visibility == View.VISIBLE) {
            recordTipView.fadeOut()
        }
    }

    private val recordRunnable: Runnable by lazy {
        Runnable {
            hasStartRecord = true
            removeCallbacks(hideRecordTipRunnable)
            post(hideRecordTipRunnable)

            if (activity == null || !audioOrVideo()) return@Runnable

            if (sendStatus == AUDIO) {
                if (!RxPermissions(activity!!).isGranted(Manifest.permission.RECORD_AUDIO)) {
                    RxPermissions(activity!!).request(Manifest.permission.RECORD_AUDIO)
                            .subscribe({}, {
                                //Bugsnag.notify(it)
                            })
                    return@Runnable
                }
            } else {
                if (RxPermissions(activity!!).isGranted(Manifest.permission.RECORD_AUDIO) &&
                        RxPermissions(activity!!).isGranted(Manifest.permission.CAMERA)) {

                    RxPermissions(activity!!).request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            .subscribe({}, {
                                //Bugsnag.notify(it)
                            })
                    return@Runnable
                }
            }
            callback.onRecordStart(sendStatus == AUDIO)
            upBeforeGrant = false
            post(checkReadyRunnable)
            chat_send_ib.parent.requestDisallowInterceptTouchEvent(true)
        }
    }

    private val checkReadyRunnable: Runnable by lazy {
        Runnable {
            if (callback.isReady()) {
                if (upBeforeGrant) {
                    upBeforeGrant = false
                    return@Runnable
                }
                isRecording = true
                checkSend()
                updateRecordCircleAndSendIcon()
                recordCircle.setLockTranslation(10000f)
            } else {
                postDelayed(checkReadyRunnable, 50)
            }
        }
    }

    private val chatSlideCallback = object : SlidePanelView.Callback {
        override fun onTimeout() {
            handleCancelOrEnd(false)
        }

        override fun onCancel() {
            handleCancelOrEnd(true)
        }
    }

    private val recordCircleCallback = object : RecordCircleView.Callback {
        override fun onSend() {
            handleCancelOrEnd(false)
        }

        override fun onCancel() {
            handleCancelOrEnd(true)
        }
    }

    fun hideOtherInput() {
        if (!botHide) {
            //   chat_bot_ib.visibility = View.GONE
        }
        chat_sticker_ib.visibility = View.GONE
        chat_more_ib.visibility = View.GONE
        sendStatus = REPLY
    }

    fun showOtherInput() {
        if (!botHide) {
            // chat_bot_ib.visibility = View.VISIBLE
        }
        chat_sticker_ib.visibility = View.VISIBLE
        chat_more_ib.visibility = View.VISIBLE
        if (sendStatus == REPLY && chat_et.text.toString().trim().isNotEmpty()) {
            return
        }
        sendStatus = lastSendStatus
    }

    private var botHide = false

    fun hideBot() {
        botHide = true
        //  chat_bot_ib.visibility = View.GONE
    }

    fun showBot() {
        botHide = false
        /// chat_bot_ib.visibility = View.VISIBLE
    }

    interface Callback {
        fun onStickerClick()
        fun onSendClick(text: String)
        fun onRecordStart(audio: Boolean)
        fun isReady(): Boolean
        fun onRecordEnd()
        fun onRecordCancel()
        fun onUp()
        fun onDown()
        fun onCalling()
        fun onDispatchTyping()
    }
}