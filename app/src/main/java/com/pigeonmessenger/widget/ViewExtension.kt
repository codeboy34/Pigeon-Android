package com.pigeonmessenger.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import com.pigeonmessenger.extension.timeAgo
import com.pigeonmessenger.extension.timeAgoClock
import com.pigeonmessenger.extension.timeAgoDate
import com.pigeonmessenger.extension.timeAgoDay
import org.jetbrains.anko.dip

const val ANIMATION_DURATION_SHORTEST = 260L

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    requestFocus()
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, SHOW_IMPLICIT)
}

fun View.fadeIn(alpha: Float = 1f) {
    this.fadeIn(ANIMATION_DURATION_SHORTEST,alpha)
}

fun View.fadeIn(duration: Long,alpha:Float) {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    ViewCompat.animate(this).alpha(alpha).setDuration(duration).setListener(object : ViewPropertyAnimatorListener {
        override fun onAnimationStart(view: View) {
        }

        override fun onAnimationEnd(view: View) {
        }

        override fun onAnimationCancel(view: View) {}
    }).start()
}

fun View.fadeOut(apha: Float=1f) {
    this.fadeOut(ANIMATION_DURATION_SHORTEST,apha = apha)
}

fun View.fadeOut(duration: Long, delay: Long = 0,apha: Float) {
    this.alpha = apha
    ViewCompat.animate(this).alpha(0f).setStartDelay(delay).setDuration(duration).setListener(object : ViewPropertyAnimatorListener {
        override fun onAnimationStart(view: View) {
            view.isDrawingCacheEnabled = true
        }

        override fun onAnimationEnd(view: View) {
            view.visibility = View.INVISIBLE
            view.alpha = 0f
            view.isDrawingCacheEnabled = false
        }

        override fun onAnimationCancel(view: View) {}
    })
}

fun View.translationX(value: Float) {
    this.translationX(value, ANIMATION_DURATION_SHORTEST)
}

fun View.translationX(value: Float, duration: Long) {
    ViewCompat.animate(this).setDuration(duration).translationX(value).start()
}

fun View.translationY(value: Float, endAction: (() -> Unit)? = null) {
    this.translationY(value, ANIMATION_DURATION_SHORTEST, endAction)
}

fun View.translationY(value: Float, duration: Long, endAction: (() -> Unit)? = null) {
    ViewCompat.animate(this).setDuration(duration).translationY(value)
            .setListener(object : ViewPropertyAnimatorListener {
                override fun onAnimationEnd(view: View?) {
                    endAction?.let { it() }
                }

                override fun onAnimationCancel(view: View?) {
                    endAction?.let { it() }
                }

                override fun onAnimationStart(view: View?) {}
            })
            .start()
}

fun View.shaking() {
    val dp20 = dip(20).toFloat()
    val dp10 = dip(10).toFloat()
    val dp5 = dip(5).toFloat()
    ObjectAnimator.ofFloat(this, "translationX", -dp20, dp20, -dp20, dp20, -dp10, dp10, -dp5, dp5, 0f)
            .setDuration(600).start()
}

fun View.animateWidth(form: Int, to: Int) {
    this.animateWidth(form, to, ANIMATION_DURATION_SHORTEST)
}

fun View.animateWidth(form: Int, to: Int, duration: Long) {
    val anim = ValueAnimator.ofInt(form, to)
    anim.addUpdateListener { valueAnimator ->
        layoutParams.width = valueAnimator.animatedValue as Int
        requestLayout()
    }
    anim.duration = duration
    anim.start()
}

fun View.animateHeight(form: Int, to: Int) {
    this.animateHeight(form, to, ANIMATION_DURATION_SHORTEST)
}

fun View.animateHeight(form: Int, to: Int, duration: Long) {
    val anim = ValueAnimator.ofInt(form, to)
    anim.addUpdateListener { valueAnimator ->
        layoutParams.height = valueAnimator.animatedValue as Int
        requestLayout()
    }
    anim.duration = duration
    if (to == 0 || form == 0) {
        anim.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator?) {
                if (to == 0) {
                    this@animateHeight.visibility = GONE
                }
            }

            override fun onAnimationStart(animation: Animator?) {
                if (form == 0) {
                    this@animateHeight.visibility = VISIBLE
                }
            }
        })
    }
    anim.start()
}

fun View.getPositionX(): Int {
    val intArray = IntArray(2)
    getLocationOnScreen(intArray)
    return intArray[0]
}


fun View.getPositionY(): Int {
    val intArray = IntArray(2)
    getLocationOnScreen(intArray)
    return intArray[1]
}

fun View.round(radius: Float) {
    this.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    this.clipToOutline = true
}

fun View.round(radius: Int) {
    round(radius.toFloat())
}

fun EditText.showCursor() {
    this.requestFocus()
    this.isCursorVisible = true
}

fun EditText.hideCursor() {
    this.clearFocus()
    this.isCursorVisible = false
}

fun ViewGroup.inflate(
        @LayoutRes layoutRes: Int,
        attachToRoot: Boolean = false
) = LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)!!

fun TextView.timeAgo(str: String) {
    text = str.timeAgo(context)
}

fun TextView.timeAgoClock(str: String) {
    text = str.timeAgoClock()
}

fun TextView.timeAgoDate(str: String) {
    text = str.timeAgoDate(context)
}

fun TextView.timeAgoDay(str: String) {
    text = str.timeAgoDay()
}
