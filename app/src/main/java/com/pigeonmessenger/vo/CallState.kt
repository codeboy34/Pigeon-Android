package com.pigeonmessenger.vo

import android.content.Context
import androidx.lifecycle.LiveData
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.webrtc.CallService
import org.webrtc.SurfaceViewRenderer

class CallState : LiveData<CallState.CallInfo>() {
    var callInfo: CallInfo = CallInfo()
    var connectedTime: Long? = null
    var isInitiator: Boolean = true
    var user: User?=null
    var callType:CallType = CallType.NONE
    var localSurfaceRenderer:SurfaceViewRenderer?=null
    var remoteSurfaceRenderer:SurfaceViewRenderer?=null


    fun setCallState(callState: CallService.CallState) {
        if (callInfo.callState == callState) return

        callInfo = CallInfo(callState, callInfo.messageId)
        postValue(callInfo)
    }

    fun setMessageId(messageId: String) {
        if (callInfo.messageId == messageId) return

        callInfo = CallInfo(callInfo.callState, messageId)
        postValue(callInfo)
    }

    fun reset() {
        callInfo = CallInfo()
        connectedTime = null
        user=null
        isInitiator = true
        postValue(callInfo)
        localSurfaceRenderer?.release()
        localSurfaceRenderer = null
        remoteSurfaceRenderer?.release()
        remoteSurfaceRenderer = null
        callType = CallType.NONE
    }

    fun isIdle() = callInfo.callState == CallService.CallState.STATE_IDLE

    fun handleHangup(ctx: Context) {
        when (callInfo.callState) {
            CallService.CallState.STATE_DIALING -> CallService.cancel(ctx)
            CallService.CallState.STATE_RINGING -> CallService.decline(ctx)
            CallService.CallState.STATE_ANSWERING -> {
                if (isInitiator) {
                    CallService.cancel(ctx)
                } else {
                    CallService.decline(ctx)
                }
            }
            CallService.CallState.STATE_CONNECTED -> CallService.localEnd(ctx)
            else -> CallService.cancel(ctx)
        }
    }

    class CallInfo(
        val callState: CallService.CallState = CallService.CallState.STATE_IDLE,
        val messageId: String? = null
    )

    enum class CallType{
        VIDEO,AUDIO,NONE
    }
}