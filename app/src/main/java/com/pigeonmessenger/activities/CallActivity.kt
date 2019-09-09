package com.pigeonmessenger.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.getSystemService
import androidx.lifecycle.Observer
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.vo.CallState
import com.pigeonmessenger.webrtc.CallService
import com.pigeonmessenger.widget.*
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.view_call_button.view.*
import java.util.*
import javax.inject.Inject

class CallActivity : AppCompatActivity(), SensorEventListener {

    private var disposable: Disposable? = null

    @Inject
    lateinit var callState: CallState

    private var sensorManager: SensorManager? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var localSurfaceVisible = false

    private var isAdded = false
    private var isButtonVisible = true

    private val toggleRunnable = Runnable {
        toggle(ANIMATION_DURATION)
    }

    val valueY: Int by lazy {
        displayHeight() - hangup_cb.getPositionY()
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.get().appComponent.inject(this)
        belowOreo {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContentView(R.layout.activity_call)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        sensorManager = getSystemService<SensorManager>()
        powerManager = getSystemService<PowerManager>()
        wakeLock = powerManager?.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "mixin")
        val answer = intent.getParcelableExtra<User?>(ARGS_ANSWER)
        if (answer != null) {
            name_tv.text = answer.getName()
            caller_name_tv.text = answer.getName()
            Log.d(TAG,"thumbnail ${answer.thumbnail}")
            avatar_iv.loadAvatar(avatarFile(answer.userId), answer.thumbnail, R.drawable.avatar_contact)
            avatar_small.loadAvatar(avatarFile(answer.userId), answer.thumbnail, R.drawable.avatar_contact)
        }
        hangup_cb.setOnClickListener {
            handleHangup()
            handleDisconnected()
        }
        answer_cb.setOnClickListener {
            handleAnswer()
        }
        mute_cb.setOnCheckedChangeListener(object : CallButton.OnCheckedChangeListener {
            override fun onCheckedChanged(id: Int, checked: Boolean) {
                CallService.muteAudio(this@CallActivity, checked)
            }
        })
        voice_cb.setOnCheckedChangeListener(object : CallButton.OnCheckedChangeListener {
            override fun onCheckedChanged(id: Int, checked: Boolean) {
                CallService.speakerPhone(this@CallActivity, checked)
            }
        })

        video_cb.setOnClickListener {
            CallService.changeCamera(this@CallActivity)
        }

        side_surface_holder.setOnClickListener {
            if (localSurfaceVisible) {
                side_surface_holder.removeAllViews()
                full_surface_holder.removeAllViews()
                full_surface_holder.addView(callState.remoteSurfaceRenderer, MATCH_PARENT, MATCH_PARENT)
                side_surface_holder.addView(callState.localSurfaceRenderer, MATCH_PARENT, MATCH_PARENT)
            } else {
                side_surface_holder.removeAllViews()
                full_surface_holder.removeAllViews()
                full_surface_holder.addView(callState.localSurfaceRenderer, MATCH_PARENT, MATCH_PARENT)
                side_surface_holder.addView(callState.remoteSurfaceRenderer, MATCH_PARENT, MATCH_PARENT)
            }
            localSurfaceVisible = !localSurfaceVisible
        }

        if (callState.callType == CallState.CallType.VIDEO)
            call_cl.setOnClickListener {
                if (callState.callInfo.callState == CallService.CallState.STATE_CONNECTED)
                    toggle(ANIMATION_DURATION)
            }

        callState.observe(this, Observer { callInfo ->
            when (callInfo.callState) {
                CallService.CallState.STATE_DIALING -> {
                    volumeControlStream = AudioManager.STREAM_VOICE_CALL
                    call_cl.post { handleDialingConnecting() }
                }
                CallService.CallState.STATE_RINGING -> {
                    call_cl.post { handleRinging() }
                }
                CallService.CallState.STATE_ANSWERING -> {
                    call_cl.post { handleAnswering() }
                }
                CallService.CallState.STATE_CONNECTED -> {
                    call_cl.post { handleConnected() }
                }
                CallService.CallState.STATE_BUSY -> {
                    call_cl.post { handleBusy() }
                }
                CallService.CallState.STATE_IDLE -> {
                    call_cl.post { handleDisconnected() }
                }
            }

            if (callInfo.callState == CallService.CallState.STATE_CONNECTED && callState.callType == CallState.CallType.VIDEO) {
                full_surface_holder.removeAllViews()
                full_surface_holder.addView(callState.remoteSurfaceRenderer!!, MATCH_PARENT, MATCH_PARENT)
                side_surface_holder.fadeIn()
                side_surface_holder.addView(callState.localSurfaceRenderer!!, MATCH_PARENT, MATCH_PARENT)
            } else if (!isAdded && callState.callType == CallState.CallType.VIDEO) {
                full_surface_holder.visibility = VISIBLE
                cover.visibility = GONE
                full_surface_holder.addView(callState.localSurfaceRenderer!!, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                isAdded = true
            }
        })

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        window.decorView.systemUiVisibility =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
        window.statusBarColor = Color.TRANSPARENT
    }

    override fun onResume() {
        sensorManager?.registerListener(this, sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_UI)
        if (callState.connectedTime != null) {
            startTimer()
        }
        super.onResume()
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopTimber()
        super.onPause()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        if (event.sensor.type == TYPE_PROXIMITY) {
            if (values[0] == 0.0f) {
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(10 * 60 * 1000L)
                }
            } else {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        }
    }


    private fun toggle(duration: Long) {
        if (isButtonVisible) {
            mute_cb.translationY(valueY.toFloat(), duration)
            hangup_cb.translationY(valueY.toFloat(), duration)
            video_cb.translationY(valueY.toFloat(), duration)
            side_surface_holder.translationY(valueY.toFloat(), duration)
            call_details.fadeOut()
            call_details.removeCallbacks(toggleRunnable)
        } else {
            mute_cb.translationY(0.toFloat(), duration)
            hangup_cb.translationY(0.toFloat(), duration)
            video_cb.translationY(0.toFloat(), duration)
            side_surface_holder.translationY(0.toFloat(), duration)
            call_details.fadeIn()
            call_details.postDelayed(toggleRunnable, TOGGLE_DELAY)
        }
        isButtonVisible = !isButtonVisible
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (callState.isIdle()) {
            handleHangup()
        }
        handleDisconnected()
    }

    private fun handleHangup() {
        callState.handleHangup(this)
    }

    @SuppressLint("CheckResult")
    private fun handleAnswer() {
        RxPermissions(this)
                .request(Manifest.permission.RECORD_AUDIO)
                .subscribe { granted ->
                    if (granted) {
                        handleAnswering()
                        CallService.answer(this@CallActivity)
                    } else {
                        callState.handleHangup(this@CallActivity)
                        handleDisconnected()
                    }
                }
    }

    private fun handleDialingConnecting() {
        voice_cb.visibility = INVISIBLE
        mute_cb.visibility = INVISIBLE
        answer_cb.visibility = INVISIBLE
        video_cb.visibility = INVISIBLE
        moveHangup(true, 0)
        action_tv.text = getString(R.string.call_notification_outgoing)
    }

    private fun handleRinging() {
        voice_cb.visibility = INVISIBLE
        mute_cb.visibility = INVISIBLE
        answer_cb.visibility = VISIBLE
        answer_cb.text.text = getString(R.string.call_accept)
        hangup_cb.text.text = getString(R.string.call_decline)
        moveHangup(false, 0)
        if (callState.callType==CallState.CallType.VIDEO)
            action_tv.text = getString(R.string.call_notification_incoming_video)
        else action_tv.text = getString(R.string.call_notification_incoming_voice)
    }

    private fun handleAnswering() {
        voice_cb.visibility = INVISIBLE
        mute_cb.visibility = INVISIBLE
        answer_cb.fadeOut()
        moveHangup(true, 250)
        action_tv.text = getString(R.string.call_connecting)
    }

    private fun handleConnected() {
        answer_cb.fadeOut()

        if (callState.callType == CallState.CallType.VIDEO) {
            avatar_iv.visibility = GONE
            voice_cb.visibility = GONE
            mute_cb.visibility = VISIBLE
            call_details.visibility = VISIBLE
            video_cb.visibility = VISIBLE
            call_details.visibility = VISIBLE
            action_tv.visibility = GONE
            name_tv.visibility = GONE
            toggle(0)
            moveHangup(true, 0)
        } else {
            voice_cb.fadeIn()
            mute_cb.fadeIn()
            moveHangup(true, 250)
        }

        startTimer()
    }

    private fun handleDisconnected() {
        finishAndRemoveTask()
    }

    private fun handleBusy() {
        handleDisconnected()
    }

    private fun moveHangup(center: Boolean, duration: Long) {
        hangup_cb.visibility = VISIBLE
        val constraintSet = ConstraintSet().apply {
            clone(call_cl)
            setHorizontalBias(hangup_cb.id, if (center) 0.5f else 0.0f)
        }
        val transition = AutoTransition().apply {
            this.duration = duration
        }
        TransitionManager.beginDelayedTransition(call_cl, transition)
        constraintSet.applyTo(call_cl)
    }

    private var timer: Timer? = null

    private fun startTimer() {
        timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (callState.connectedTime != null) {
                        val duration = System.currentTimeMillis() - callState.connectedTime!!
                        action_tv.text = duration.formatMillis()
                        caller_action_tv.text = duration.formatMillis()
                    }
                }
            }
        }
        timer?.schedule(timerTask, 0, 1000)
    }

    private fun stopTimber() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }

    companion object {
        const val TAG = "CallActivity"

        const val ARGS_ANSWER = "answer"
        private const val ANIMATION_DURATION: Long = 300
        private const val TOGGLE_DELAY = 3000L
        fun show(context: Context, answer: User? = null) {
            Intent(context, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(ARGS_ANSWER, answer)
            }.run {
                context.startActivity(this)
            }
        }
    }
}