package com.pigeonmessenger.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import com.google.android.exoplayer2.util.Util
import com.pigeonmessenger.activities.App
import java.util.*


fun Long.formatMillis(): String {
    val formatBuilder = StringBuilder()
    val formatter = Formatter(formatBuilder, Locale.getDefault())
    Util.getStringForTime(formatBuilder, formatter, this)
    return formatBuilder.toString()
}

inline fun Long.toLeByteArray(): ByteArray {
    var num = this
    val result = ByteArray(8)
    for (i in (0..7)) {
        result[i] = (num and 0xffL).toByte()
        num = num shr 8
    }
    return result
}


fun String.getSpannableBuilder(context: Context, keyword: String): SpannableStringBuilder {
    val builder = SpannableStringBuilder()

    val firstIndex = this.indexOf(keyword, 0, false)
    val lastIndex = firstIndex + keyword.length

    val spannableString = SpannableString(this)
    spannableString.setSpan(ForegroundColorSpan(context.resources.getColor(com.pigeonmessenger.R.color.search_highlight)), firstIndex, lastIndex, 0)
    builder.append(spannableString)
    return builder
}

fun String.isNumber(): Boolean = this.matches(Regex("\\d+(?:\\.\\d+)?"))

fun String.formatPhoneNumber(): String? {
    var num = this.replace(Regex("[^0-9]"), "")
    if (num[0] == '0') num = num.substring(1)
    if (num.length == 12) num = num.replaceFirst("91".toRegex(), "")
    if (num.length==10) return num
    return null
}

fun randomMediaKey():String{
    val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    val salt = StringBuilder()
    val rnd = Random()
    while (salt.length < 20) { // length of the random string.
        val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
        salt.append(SALTCHARS[index])
    }
    return salt.toString()
}

 fun SpannableString.setLinkSpan(text: String, url: String) {
    val textIndex = this.indexOf(text)
    setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }.also {
                        startActivity(App.get(),it,null)
                    }
                }
            },
            textIndex,
            textIndex + text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}