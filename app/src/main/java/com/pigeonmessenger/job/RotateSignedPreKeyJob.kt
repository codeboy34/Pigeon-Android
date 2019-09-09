package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.api.request.SignalKeyRequest
import com.pigeonmessenger.crypto.IdentityKeyUtil
import com.pigeonmessenger.crypto.PreKeyUtil

class RotateSignedPreKeyJob : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().groupBy("rotate_signed_pre_key"),"") {
    override fun cancel() {

    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        return RetryConstraint.RETRY
    }

    companion object {
        private const val serialVersionUID = 1L
        const val ROTATE_SIGNED_PRE_KEY = "rotate_signed_pre_key"
    }

    private val TAG = RotateSignedPreKeyJob::class.java.simpleName

    override fun onRun() {
        Log.w(TAG, "Rotating signed pre key...")

        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(applicationContext)
        val signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(applicationContext, identityKeyPair, false)

        val response = signalKeyService
            .pushSignalKeys(SignalKeyRequest(ik = identityKeyPair.publicKey, spk = signedPreKeyRecord))
            .execute()
        if (response.isSuccessful) {
            PreKeyUtil.setActiveSignedPreKeyId(applicationContext, signedPreKeyRecord.id)
        }
    }
}