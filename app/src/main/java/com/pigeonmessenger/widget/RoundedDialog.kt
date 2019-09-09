package com.pigeonmessenger.widget

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.pigeonmessenger.R
import kotlinx.android.synthetic.main.rounded_layout.*



class RoundedDialog : DialogFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.rounded_layout, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        //dialog?.window.
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }


    fun setPositiveButton(name: String, action: () -> Unit) {
        positive_bt.text = name
        positive_bt.setOnClickListener { action() }
    }

    fun setNegetiveButton(name: String, action: () -> Unit) {
        divider.visibility = VISIBLE
        negative_bt.visibility = VISIBLE
        negative_bt.setOnClickListener { action() }
    }

}