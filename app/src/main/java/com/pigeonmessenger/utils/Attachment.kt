package com.pigeonmessenger.utils

import android.net.Uri

data class Attachment(val uri: Uri, val filename: String, val mimeType: String, val fileSize: Long)
