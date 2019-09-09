package com.pigeonmessenger.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.pigeonmessenger.R
import kotlinx.android.synthetic.main.card_title_view.view.*
import android.widget.FrameLayout


class CardTitleView : CardView{

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs,0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.card_title_view,this,true)
        val ta = context.obtainStyledAttributes(attrs,R.styleable.CardTitleView,defStyleAttr,0)
        val title =  ta.getString(R.styleable.CardTitleView_card_title)
        val elevation = ta.getDimension(R.styleable.CardTitleView_card_title_elevation,1.0f)
        cardElevation  = elevation
        val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0,0,0,elevation.toInt())
        layoutParams = params

        ta.recycle()
        title?.let {
            title_tv.text = it
        }

        setCardBackgroundColor(ContextCompat.getColor(context,R.color.title_bar_color))
        cardElevation = 1.0f
    }

}