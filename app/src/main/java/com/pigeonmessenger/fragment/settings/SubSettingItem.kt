package com.pigeonmessenger.fragment.settings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.pigeonmessenger.R
import kotlinx.android.synthetic.main.sub_setting_item.view.*

class SubSettingItem  :LinearLayout {

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs,0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr,0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes){
        LayoutInflater.from(context).inflate(R.layout.sub_setting_item,this,true)
        val ta= context!!.obtainStyledAttributes(attrs, R.styleable.SubSettingItem,defStyleAttr,defStyleRes)
        val title =  ta.getString(R.styleable.SubSettingItem_sub_setting_title)
        title_tv.text = title
        ta?.recycle()
        orientation= LinearLayout.HORIZONTAL
        background = context.getDrawable(R.drawable.setting_back)
    }
}