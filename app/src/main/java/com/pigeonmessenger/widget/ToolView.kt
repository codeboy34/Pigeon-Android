package com.pigeonmessenger.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import com.pigeonmessenger.R


class ToolView  : CardView {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs,0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.view_tool, this, true)
    }
}
