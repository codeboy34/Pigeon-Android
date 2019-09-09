package com.pigeonmessenger.adapter.holder

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import androidx.core.content.ContextCompat
import com.pigeonmessenger.R
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.events.BlinkEvent
import com.pigeonmessenger.extension.dpToPx
import com.pigeonmessenger.vo.MessageStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable


abstract class BaseViewHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    companion object {
        val colors: IntArray = App.get().resources.getIntArray(R.array.name_colors)

        val HIGHLIGHTED = Color.parseColor("#CCEF8C")
        val LINK_COLOR = Color.parseColor("#5FA7E4")
        val SELECT_COLOR = Color.parseColor("#1D000000")
    }

    protected val dp10 by lazy {
        App.get().dpToPx(10f)
    }
    private val dp12 by lazy {
        App.get().dpToPx(12f)
    }

    private val tickColor by lazy {
        ContextCompat.getColor(App.get(), R.color.color_chat_date)
    }

    private val readTickColor by lazy {
        ContextCompat.getColor(App.get(), R.color.pigeonActionColor)
    }

    protected var isMe = false

    protected open fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean = false) {
        this.isMe = isMe
    }

    private fun chatLayout(isLast: Boolean) {
        chatLayout(isMe, isLast, true)
    }


    val meId by lazy {
        Session.getUserId()
    }

    private var disposable: Disposable? = null
    private var messageId: String? = null

    fun listen(bindId: String) {
        if (disposable == null) {
            disposable = RxBus.listen(BlinkEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it.messageId == this.messageId) {
                            if (it.type != null) {
                                chatLayout(it.type)
                            } else {
                                blink()
                            }
                        }
                    }
        }
        this.messageId = bindId
    }

    fun stopListen() {
        disposable?.dispose()
        disposable = null
    }

    private fun blink() {
        if (!blinkAnim.isRunning) {
            blinkAnim.start()
        }
    }

    private val argbEvaluator: ArgbEvaluator by lazy {
        ArgbEvaluator()
    }
    private val blinkAnim by lazy {
        ValueAnimator.ofFloat(0f, 1f, 0f)
                .setDuration(1200).apply {
                    this.addUpdateListener { valueAnimator ->
                        itemView.setBackgroundColor(
                                argbEvaluator.evaluate(valueAnimator.animatedValue as Float, Color.TRANSPARENT, SELECT_COLOR) as Int)
                    }
                }
    }

    protected fun setStatusIcon(
            isMe: Boolean,
            status: String,
            setIcon: (Drawable?) -> Unit,
            hideIcon: () -> Unit,
            isWhite: Boolean = false
    ) {
        if (isMe) {
            val drawable: Drawable? =
                    when (status) {
                        MessageStatus.SENDING.name ->
                            AppCompatResources.getDrawable(itemView.context,
                                    R.drawable.msg_status_gray_waiting)?.apply {
                                if (isWhite) this.setTint(Color.WHITE)
                                else this.setTint(tickColor)
                            }
                        MessageStatus.SENT.name ->
                            AppCompatResources.getDrawable(itemView.context,
                                    R.drawable.msg_status_server_receive)?.apply {
                                if (isWhite) this.setTint(Color.WHITE)
                                else this.setTint(tickColor)
                            }
                        MessageStatus.DELIVERED.name ->
                            AppCompatResources.getDrawable(itemView.context,
                                    R.drawable.msg_status_client_received)?.apply {
                                if (isWhite) this.setTint(Color.WHITE)
                                else this.setTint(tickColor)
                            }
                        MessageStatus.READ.name ->
                            AppCompatResources.getDrawable(itemView.context, R.drawable.msg_status_client_received)?.apply {
                                this.setTint(readTickColor)
                            }
                        else -> null
                    }



            drawable.also {
                it?.setBounds(0, 0, dp10, dp10)
            }
            setIcon(drawable)
        } else {
            hideIcon()
        }
    }
}