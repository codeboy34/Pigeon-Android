package com.pigeonmessenger.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.isTablet
import com.pigeonmessenger.extension.notNullElse
import org.jetbrains.anko.dip
import org.jetbrains.anko.displayMetrics
import kotlin.math.abs
import kotlin.math.min

class BottomSheet(context: Context, private val focusable: Boolean) : Dialog(context, R.style.TransparentDialog) {

    private var startAnimationRunnable: Runnable? = null
    private var curSheetAnimation: AnimatorSet? = null
    private var isDismissed = false
    private var lastInsets: WindowInsets? = null

    private val container: ContainerView by lazy { ContainerView(context) }
    private val sheetContainer: FrameLayout by lazy { FrameLayout(context) }
    private var customView: View? = null
    private var customViewHeight: Int = 0

    private val speed = context.dip(0.5f)

    private val backDrawable = ColorDrawable(-0x1000000)

    private var bottomSheetListener: BottomSheetListener? = null

    var fullScreen = false

    private inner class ContainerView(context: Context) : FrameLayout(context) {

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(ev: MotionEvent?): Boolean {
            if (ev != null && (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE)) {
                val startX = ev.x.toInt()
                val startY = ev.y.toInt()
                if (startY < sheetContainer.top || startX < sheetContainer.left || startX > sheetContainer.right) {
                    dismiss()
                    return true
                }
            }
            return super.onTouchEvent(ev)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = View.MeasureSpec.getSize(widthMeasureSpec)
            var height = View.MeasureSpec.getSize(heightMeasureSpec)
            if (lastInsets != null) {
                height -= lastInsets!!.systemWindowInsetBottom
            }
            setMeasuredDimension(width, height)
            val isPortrait = width < height
            val widthSpec = if (context.isTablet()) {
                View.MeasureSpec.makeMeasureSpec(
                    (minOf(context.displayMetrics.widthPixels, context.displayMetrics.heightPixels) * 0.8f).toInt(),
                    View.MeasureSpec.EXACTLY)
            } else {
                View.MeasureSpec.makeMeasureSpec(
                    if (isPortrait) width
                    else Math.max(width * 0.8f, minOf(context.dip(480f), width).toFloat()).toInt(),
                    View.MeasureSpec.EXACTLY)
            }
            sheetContainer.measure(widthSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
        }
    }

    interface BottomSheetListener {
        fun onOpenAnimationStart()

        fun onOpenAnimationEnd()

        fun canDismiss(): Boolean
    }

    open class BottomSheetListenerAdapter : BottomSheetListener {
        override fun onOpenAnimationStart() {
        }

        override fun onOpenAnimationEnd() {
        }

        override fun canDismiss() = false
    }

    init {
        window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        container.background = backDrawable
        container.fitsSystemWindows = true
        container.setOnApplyWindowInsetsListener { v, insets ->
            if (lastInsets == null && customView != null && !fullScreen) {
                val params = customView!!.layoutParams as FrameLayout.LayoutParams
                params.topMargin = insets.systemWindowInsetTop
                customView!!.layoutParams = params
            }
            lastInsets = insets
            v.requestLayout()
            insets.consumeSystemWindowInsets()
        }
        container.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        backDrawable.alpha = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setWindowAnimations(R.style.DialogNoAnimation)
        if (Build.VERSION.SDK_INT >= 26) {
            window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        setContentView(container, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        sheetContainer.fitsSystemWindows = true
        sheetContainer.visibility = INVISIBLE
        container.addView(sheetContainer, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))

        if (customView != null) {
            if (customView!!.parent != null) {
                (customView!!.parent as ViewGroup).removeView(customView)
            }
            sheetContainer.addView(customView,
                FrameLayout.LayoutParams(MATCH_PARENT,
                    if (customViewHeight > 0) customViewHeight else WRAP_CONTENT,
                    Gravity.START or Gravity.TOP))
        }

        window?.let { window ->
            val params = window.attributes
            params.width = MATCH_PARENT
            params.height = MATCH_PARENT
            params.gravity = Gravity.START or Gravity.TOP
            params.dimAmount = 0f
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            if (!focusable) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            }
            window.attributes = params
        }
    }

    override fun show() {
        try {
            super.show()
        } catch (ignored: Exception) {
        }
        if (focusable) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        isDismissed = false
        cancelSheetAnimation()
        sheetContainer.measure(View.MeasureSpec.makeMeasureSpec(context.displayMetrics.widthPixels, AT_MOST),
            View.MeasureSpec.makeMeasureSpec(context.displayMetrics.heightPixels, AT_MOST))
        backDrawable.alpha = 0
        sheetContainer.translationY = sheetContainer.measuredHeight.toFloat()
        startAnimationRunnable = object : Runnable {
            override fun run() {
                if (startAnimationRunnable != this || isDismissed) {
                    return
                }
                startAnimationRunnable = null
                startOpenAnimation()
            }
        }
        sheetContainer.post(startAnimationRunnable)
    }

    private fun startOpenAnimation() {
        if (isDismissed) {
            return
        }
        sheetContainer.visibility = VISIBLE
        container.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        sheetContainer.translationY = sheetContainer.measuredHeight.toFloat()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(sheetContainer, "translationY", 0f),
            ObjectAnimator.ofInt(backDrawable, "alpha", 51)
        )
        animatorSet.duration = 200
        animatorSet.startDelay = 20
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                    bottomSheetListener?.onOpenAnimationEnd()
                    container.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                }
            }
        })
        animatorSet.start()
        curSheetAnimation = animatorSet
    }

    override fun dismiss() {
        if (isDismissed) {
            return
        }
        isDismissed = true
        fakeDismiss(false)
    }

    fun fakeDismiss(fake: Boolean = true) {
        cancelSheetAnimation()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(sheetContainer, "translationY", sheetContainer.measuredHeight.toFloat()),
            ObjectAnimator.ofInt(backDrawable, "alpha", 0)
        )
        animatorSet.duration = 180
        animatorSet.interpolator = AccelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                    sheetContainer.post {
                        if (!fake) {
                            try {
                                dismissInternal()
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                }
            }
        })
        animatorSet.start()
        curSheetAnimation = animatorSet
    }

    fun setListener(listener: BottomSheetListener) {
        bottomSheetListener = listener
    }

    fun setCustomView(view: View) {
        customView = view
    }

    fun getCustomView() = customView

    fun setCustomViewHeight(height: Int, endAction: (() -> Unit)? = null) {
        customViewHeight = height
        val params = customView?.layoutParams
        val duration = notNullElse(customView?.layoutParams, {
            try {
                min(abs(height - it.height) / speed, 200)
            } catch (e: ArithmeticException) {
                200
            }
        }, 200).toLong()

        if (duration == 0L) {
            return
        }
        if (params != null) {
            val anim = ValueAnimator.ofInt(customView!!.height, height)
            anim.interpolator = LinearInterpolator()
            anim.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                params.height = value
                customView?.layoutParams = params
                if (value == height) {
                    endAction?.let { it() }
                }
            }
            anim.duration = duration
            anim.start()
        }
    }

    fun setCustomViewHeightSync(height: Int) {
        customViewHeight = height
        customView?.layoutParams?.let { params ->
            params.height = height
            customView?.layoutParams = params
        }
    }

    fun getCustomViewHeight() = customViewHeight

    private fun cancelSheetAnimation() {
        curSheetAnimation?.cancel()
        curSheetAnimation = null
    }

    private fun dismissInternal() {
        try {
            super.dismiss()
        } catch (e: Exception) {
        }
    }

    class Builder {
        private var bottomSheet: BottomSheet

        constructor(context: Context) {
            bottomSheet = BottomSheet(context, false)
        }

        constructor(context: Context, needFocus: Boolean) {
            bottomSheet = BottomSheet(context, needFocus)
        }

        fun setCustomView(view: View): Builder {
            bottomSheet.customView = view
            return this
        }

        fun setListener(bottomSheetListener: BottomSheetListener): Builder {
            bottomSheet.bottomSheetListener = bottomSheetListener
            return this
        }

        fun create(): BottomSheet = bottomSheet

        fun show(): BottomSheet {
            bottomSheet.show()
            return bottomSheet
        }
    }
}
