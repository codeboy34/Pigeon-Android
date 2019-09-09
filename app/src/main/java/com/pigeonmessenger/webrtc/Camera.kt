package com.pigeonmessenger.webrtc

import android.content.Context
import android.util.Log
import com.pigeonmessenger.webrtc.CameraState.Direction.*
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer

class Camera internal constructor(context: Context, private val cameraEventListener: CameraEventListener) :
        CameraVideoCapturer.CameraSwitchHandler {

    internal val capturer: CameraVideoCapturer?
    internal val count: Int

    private var activeDirection: CameraState.Direction? = null
    private var enabled: Boolean = false

    init {
        val enumerator = getCameraEnumerator(context)
        count = enumerator.deviceNames.size
        var capturerCandidate = createVideoCapturer(enumerator, FRONT)
        if (capturerCandidate != null) {
            activeDirection = FRONT
        } else {
            capturerCandidate = createVideoCapturer(enumerator, BACK)
            if (capturerCandidate != null) {
                activeDirection = BACK
            } else {
                activeDirection = NONE
            }
        }
        capturer = capturerCandidate
    }

    internal fun flip() {
        if (capturer == null || count < 2) {
            Log.w(TAG, "Tried to flip the camera, but we only have $count of them.")
            return
        }

        if (activeDirection == PENDING)
            return

        activeDirection = PENDING

        capturer.switchCamera(this)
    }

    internal fun setEnabled(enabled: Boolean) {
        this.enabled = enabled

        if (capturer == null) {
            return
        }

        try {
            if (enabled) {
                capturer.startCapture(1280, 720, 30)
            } else {
                capturer.stopCapture()
            }
        } catch (e: InterruptedException) {
            Log.w(TAG, "Got interrupted while trying to stop video capture", e)
        }

    }

    internal fun dispose() {
        capturer?.dispose()
    }

    internal fun getActiveDirection(): CameraState.Direction {
        return if (enabled) activeDirection ?: NONE else NONE
    }

    private fun createVideoCapturer(enumerator: CameraEnumerator,
                                    direction: CameraState.Direction): CameraVideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (direction == FRONT && enumerator.isFrontFacing(deviceName) || direction == BACK && enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }

        return null
    }

    private fun getCameraEnumerator(context: Context): CameraEnumerator {
        var camera2EnumeratorIsSupported = false
        try {
            camera2EnumeratorIsSupported = Camera2Enumerator.isSupported(context)
        } catch (throwable: Throwable) {
            Log.w(TAG, "Camera2Enumator.isSupport() threw.", throwable)
        }

        Log.i(TAG, "Camera2 enumerator supported: $camera2EnumeratorIsSupported")

        return if (camera2EnumeratorIsSupported)
            Camera2Enumerator(context)
        else
            Camera1Enumerator(true)
    }

    override fun onCameraSwitchDone(isFrontFacing: Boolean) {
        activeDirection = if (isFrontFacing) FRONT else BACK
        cameraEventListener.onCameraSwitchCompleted(CameraState(getActiveDirection(), count))
    }

    override fun onCameraSwitchError(errorMessage: String) {
        Log.e(TAG, "onCameraSwitchError: $errorMessage")
        cameraEventListener.onCameraSwitchCompleted(CameraState(getActiveDirection(), count))
    }

    interface CameraEventListener {
        fun onCameraSwitchCompleted(newCameraState: CameraState)
    }

    companion object {

        val TAG = "Camera"
    }
}
