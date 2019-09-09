package com.pigeonmessenger.extension

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.ArrayMap
import androidx.fragment.app.Fragment
import com.pigeonmessenger.R
import com.pigeonmessenger.widget.PbDialog
import org.jetbrains.anko.*




fun Fragment.toast(textResource: Int) = requireActivity().toast(textResource)

fun Fragment.toast(text: CharSequence) = requireActivity().toast(text)

fun Fragment.selector(
        title: CharSequence? = null,
        items: List<CharSequence>,
        onClick: (DialogInterface, Int) -> Unit
): Unit = requireActivity().selector(title, items, onClick)

fun Fragment.alert(
        message: String,
        title: String? = null,
        init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().alert(message, title, init)

fun Fragment.indeterminateProgressDialog(message: String? = null, title: String? = null, init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().indeterminateProgressDialog(message, title, init)
}

fun Fragment.progressDialog():PbDialog {
    val pbDialog = PbDialog()
    pbDialog.isCancelable= false
    return  pbDialog
}

fun PbDialog.addMessage(context: Context, message: Int?){
    val args= Bundle()
    val msg = if (message!=null) context.getString(message) else context.getString(R.string.please_wait)
    args.putString("message",msg)
    this.arguments = args
}

fun Fragment.pigeonProgressDialog(message: Int? = null) : AlertDialog{
    val builder = AlertDialog.Builder(context)
    builder.setCancelable(false) // if you want user to wait for some process to finish,
    builder.setView(R.layout.item_progress_dialog)
    return builder.create()
}

fun Fragment.indeterminateProgressDialog(message: Int? = null, title: Int? = null, init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().indeterminateProgressDialog(message?.let { requireActivity().getString(it) }, title?.let { requireActivity().getString(it) }, init)
}

fun <K, V> arrayMapOf(): ArrayMap<K, V> = ArrayMap()
