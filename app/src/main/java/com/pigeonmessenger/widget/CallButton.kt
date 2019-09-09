package com.pigeonmessenger.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.LinearLayout
import com.pigeonmessenger.R

import kotlinx.android.synthetic.main.view_call_button.view.*
import org.jetbrains.anko.backgroundResource

class CallButton(context: Context, attr: AttributeSet) : LinearLayout(context, attr), Checkable {

    private var checkable: Boolean = false
    private var checked: Boolean = true
    private var bgChecked: Int = 0
    private var bgUnchecked: Int = 0
    private var srcChecked = 0
    private var srcUnchecked = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.view_call_button, this, true)
        val ta = context.obtainStyledAttributes(attr, R.styleable.CallButton)
        ta?.let {
            checkable = ta.getBoolean(R.styleable.CallButton_android_checkable, false)
            checked = ta.getBoolean(R.styleable.CallButton_android_checked, true)
            bgChecked = ta.getResourceId(R.styleable.CallButton_bg_circle_checked, 0)
            bgUnchecked = ta.getResourceId(R.styleable.CallButton_bg_circle_unchecked, 0)
            srcChecked = ta.getResourceId(R.styleable.CallButton_ic_checked, 0)
            srcUnchecked = ta.getResourceId(R.styleable.CallButton_ic_unchecked, 0)
            text.text = ta.getText(R.styleable.CallButton_android_text)

            ta.recycle()
        }
        update(isChecked)

        if (checkable) {
            setOnClickListener {
                toggle()
            }
        }
    }

    override fun isChecked() = checked

    override fun toggle() {
        isChecked = !checked
    }

    override fun setChecked(checked: Boolean) {
        if (this.checked != checked) {
            listener?.onCheckedChanged(id, checked)
        }
        this.checked = checked
        update(checked)
    }

    private fun update(isChecked: Boolean) {
        if (isChecked) {
            icon.backgroundResource = bgChecked
            icon.setImageResource(srcChecked)
        } else {
            icon.backgroundResource = bgUnchecked
            icon.setImageResource(srcUnchecked)
        }
    }

    private var listener: OnCheckedChangeListener? = null

    interface OnCheckedChangeListener {
        fun onCheckedChanged(id: Int, checked: Boolean)
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.listener = listener
    }
}