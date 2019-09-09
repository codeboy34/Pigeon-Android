package com.pigeonmessenger.widget.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.extension.createAudioTemp
import com.pigeonmessenger.extension.getAudioPath
import com.pigeonmessenger.extension.vibrate
import com.pigeonmessenger.widget.DispatchQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import java.io.File

class OpusAudioRecorder private constructor(private val ctx: Context) {
    companion object {
        init {
            System.loadLibrary("mixin")
        }

        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE_FACTOR = 2

        const val STATE_NOT_INIT = 0
        const val STATE_IDLE = 1
        const val STATE_RECORDING = 2

        private const val MAX_RECORD_DURATION = 60000

        var state: Int = STATE_NOT_INIT

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: OpusAudioRecorder? = null

        @Synchronized
        fun get(): OpusAudioRecorder {
            if (INSTANCE == null) {
                INSTANCE = OpusAudioRecorder(App.get())
                state = STATE_IDLE
            }
            return INSTANCE as OpusAudioRecorder
        }
    }

    private var audioRecord: AudioRecord? = null
    private var recordBufferSize: Int
    private var recordingAudioFile: File? = null
    private val recordSamples = ShortArray(1024)
    private var samplesCount = 0L
    private var recordTimeCount = 0L
    private var sendAfterDone = false
    private var callStop = false

    private val recordQueue: DispatchQueue by lazy {
        DispatchQueue("recordQueue").apply {
            priority = Thread.MAX_PRIORITY
        }
    }
    private val fileEncodingQueue: DispatchQueue by lazy {
        DispatchQueue("fileEncodingQueue").apply {
            priority = Thread.MAX_PRIORITY
        }
    }

    private var callback: Callback? = null

    init {
        recordBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

        try {
            val phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                    if (state != TelephonyManager.CALL_STATE_IDLE) {
                        stopRecording(false)
                        callback?.onCancel()
                    }
                }
            }
            //ctx.getSystemService()
            //ctx.getSystemService()
            val telephonyManager  = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (ignore: Exception) {
        }
    }

    private val recordRunnable: Runnable by lazy {
        Runnable recordRunnable@{
            audioRecord?.let { audioRecord ->
                val shortArray = ShortArray(recordBufferSize)
                val len = audioRecord.read(shortArray, 0, shortArray.size)
                if (len > 0 && !callStop) {
                    var sum = 0
                    try {
                        val newSamplesCount = samplesCount + len / 2
                        val currPart = (samplesCount / newSamplesCount.toDouble() * recordSamples.size).toInt()
                        val newPart = recordSamples.size - currPart
                        var sampleStep: Float
                        if (currPart != 0) {
                            sampleStep = recordSamples.size / currPart.toFloat()
                            var currNum = 0f
                            for (i in 0 until currPart) {
                                recordSamples[i] = recordSamples[currNum.toInt()]
                                currNum += sampleStep
                            }
                        }
                        var currNum = currPart
                        var nextNum = 0f
                        sampleStep = len / 2f / newPart
                        for (i in 0 until len) {
                            val peak = shortArray[i]
                            if (peak > 2500) {
                                sum += peak * peak
                            }
                            if (i == nextNum.toInt() && currNum < recordSamples.size) {
                                recordSamples[currNum] = peak
                                nextNum += sampleStep
                                currNum++
                            }
                        }
                        samplesCount = newSamplesCount
                    } catch (e: Exception) {
                    }

                    fileEncodingQueue.postRunnable(Runnable encodingRunnable@{
                        if (callStop) return@encodingRunnable

                        writeFrame(shortArray, len)
                        recordTimeCount += len / 16

                        if (recordTimeCount >= MAX_RECORD_DURATION) {
                            stopRecording(true, false)
                        }
                    })
                    recordQueue.postRunnable(recordRunnable)
                } else {
                    stopRecordingInternal(sendAfterDone)
                }
            }
        }
    }

    private val recodeStartRunnable = Runnable {
        if (audioRecord != null) {
            return@Runnable
        }

        recordingAudioFile = ctx.getAudioPath().createAudioTemp("ogg")

        try {
            if (startRecord(recordingAudioFile!!.absolutePath) != 0) {
                return@Runnable
            }

            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, recordBufferSize * BUFFER_SIZE_FACTOR)

            if (audioRecord == null || audioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
                return@Runnable
            }
            callStop = false
            samplesCount = 0
            recordTimeCount = 0
            audioRecord?.startRecording()

            if (audioRecord != null && audioRecord!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.release()
                audioRecord = null
                return@Runnable
            }
            ctx.vibrate(longArrayOf(0, 10))
            state = STATE_RECORDING
        } catch (e: Exception) {
            recordingAudioFile?.delete()
            try {
                stopRecord()
                state = STATE_IDLE
                audioRecord?.release()
                audioRecord = null
            } catch (ignore: Exception) {
            }
            return@Runnable
        }

        recordQueue.postRunnable(recordRunnable)
    }

    fun startRecording(callback: Callback) {
        this.callback = callback
        recordQueue.postRunnable(recodeStartRunnable)
    }

    fun stopRecording(send: Boolean, vibrate: Boolean = true) {
        recordQueue.cancelRunnable(recodeStartRunnable)
        if (vibrate) {
            ctx.vibrate(longArrayOf(0, 10))
        }
        recordQueue.postRunnable(Runnable {
            audioRecord?.let { audioRecord ->
                try {
                    sendAfterDone = send
                    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                    }
                } catch (e: Exception) {
                    recordingAudioFile?.delete()
                }
                stopRecordingInternal(send)
            }
        })
    }

    fun stop() {
        callback = null
        stopRecording(false, false)
    }

    private fun stopRecordingInternal(send: Boolean) {
        callStop = true
        fileEncodingQueue.postRunnable(Runnable {
            stopRecord()

            if (send) {
                val duration = recordTimeCount
                val waveForm = getWaveform2(recordSamples, recordSamples.size)
                GlobalScope.launch(Dispatchers.Main) {
                    if (recordingAudioFile != null) {
                        callback?.sendAudio(recordingAudioFile!!, duration, waveForm)
                    }
                    callback = null
                    recordingAudioFile = null
                }
            }
        })
        state = STATE_IDLE
        try {
            audioRecord?.release()
            audioRecord = null
        } catch (ignore: Exception) {
        }
    }

    private external fun startRecord(path: String): Int
    private external fun writeFrame(frame: ShortArray, len: Int): Int
    private external fun stopRecord()
    private external fun getWaveform2(arr: ShortArray, len: Int): ByteArray

    interface Callback {
        fun onCancel()
        fun sendAudio(file: File, duration: Long, waveForm: ByteArray)
    }
}