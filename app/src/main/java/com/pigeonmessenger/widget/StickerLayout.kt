package com.pigeonmessenger.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.pigeonmessenger.widget.keyboard.InputAwareLayout


class StickerLayout : FrameLayout, InputAwareLayout.InputView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun show(height: Int, immediate: Boolean) {
        val params = layoutParams
        params.height = height
        layoutParams = params
        visibility = View.VISIBLE
    }

    override fun hide(immediate: Boolean) {
        visibility = GONE
    }

    override fun isShowing() = visibility == View.VISIBLE
}