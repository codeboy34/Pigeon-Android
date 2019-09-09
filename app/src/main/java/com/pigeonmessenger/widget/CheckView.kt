package com.pigeonmessenger.widget

import android.content.Context
import androidx.annotation.StringRes
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.FrameLayout
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.fileSize
import kotlinx.android.synthetic.main.view_check.view.*

class CheckView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), Checkable {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_check, this, true)
        setOnClickListener {
            toggle()
        }
    }

    override fun isChecked(): Boolean {
        return check_box.isChecked
    }

    override fun toggle() {
        check_box.isChecked = !check_box.isChecked
    }

    override fun setChecked(checked: Boolean) {
        check_box.isChecked = checked
    }

    fun setName(name: String) {
        name_tv.text = name
    }

    fun setName(@StringRes name: Int) {
        name_tv.setText(name)
    }

    fun setSize(size: Long) {
        storage_tv.text = size.fileSize()
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener) {
        check_box.setOnCheckedChangeListener(listener)
    }
}
