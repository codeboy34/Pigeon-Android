package com.pigeonmessenger.events

import com.pigeonmessenger.widget.CircleProgress.Companion.STATUS_ERROR

class ProgressEvent(val id: String, var progress: Float, val status: Int = STATUS_ERROR)
