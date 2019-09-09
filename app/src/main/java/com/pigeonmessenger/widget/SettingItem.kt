package com.pigeonmessenger.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import com.pigeonmessenger.R
import kotlinx.android.synthetic.main.item_setting.view.*
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.textColor

class SettingItem: LinearLayout {

    private var titleString: String? = null
    private var iconDrawable: Drawable? = null
    private var tintColor: Int? = null


    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context!!.layoutInflater.inflate(R.layout.item_setting, this, true)
        orientation = LinearLayout.HORIZONTAL
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SettingItem, defStyleAttr, 0)
        if (ta != null) {
            titleString = ta.getString(R.styleable.SettingItem_setting_title)
            iconDrawable = ta.getDrawable(R.styleable.SettingItem_setting_icon)
            tintColor = ta.getColor(R.styleable.SettingItem_setting_color, context.resources.getColor(R.color.black))
        }
        ta.recycle()

        title.text = titleString
        title.textColor = tintColor!!
        iconDrawable!!.setTint(tintColor!!)
        icon.setImageDrawable(iconDrawable)
    }

}