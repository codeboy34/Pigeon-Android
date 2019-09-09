package com.pigeonmessenger.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.pigeonmessenger.R

class BottomNavigationView : LinearLayout {

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs,0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.bottom_nav_view,this,true)
    }
}