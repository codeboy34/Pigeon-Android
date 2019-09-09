package com.pigeonmessenger.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.pigeonmessenger.R
import kotlinx.android.synthetic.main.item_mute_time.view.*

class MuteItem : LinearLayout {

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        LayoutInflater.from(context).inflate(R.layout.item_mute_time, this, true)
        orientation = LinearLayout.HORIZONTAL
    }

    fun makeActive() {
        check_iv.visibility = View.VISIBLE
        time_tv.setTextColor(ContextCompat.getColor(context, R.color.dotRed))
    }

    fun unActive() {
        check_iv.visibility = View.INVISIBLE
        time_tv.setTextColor(Color.BLACK)
    }

    fun isActive() = check_iv.isVisible

}