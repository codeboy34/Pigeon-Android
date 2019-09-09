
package com.pigeonmessenger.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.request.SignalKeyRequest
import com.pigeonmessenger.crypto.IdentityKeyUtil
import com.pigeonmessenger.crypto.PreKeyUtil
import java.util.UUID

class RefreshOneTimePreKeysJob : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork()
    .groupBy("refresh_pre_keys"), UUID.randomUUID().toString()) {

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "${p0}: ");
        return RetryConstraint.RETRY
    }

    override fun cancel() {
    }

    private val TAG = RefreshOneTimePreKeysJob::class.java.simpleName

    override fun onRun() {
        checkSignalKey()
    }

    private fun checkSignalKey() {
        Log.d(TAG, "checkSignalKey: ");
        val response = signalKeyService.getSignalKeyCount().execute()
        if (response.isSuccessful && response.body() != null) {
            val availableKeyCount = response.body()!!.preKeyCount
            Log.d(TAG, "preKeyCount ${availableKeyCount}: ");
            if (availableKeyCount >= PREKEY_MINI_NUM) {
                return
            }
            refresh()
        }else{
            Log.d(TAG, "${response.code()}: ");
        }
    }

    private fun refresh() {
        val signalKeysRequest = generateKeys()
        Log.w(TAG, "Registering new pre keys...")
        val response = signalKeyService.pushSignalKeys(signalKeysRequest).execute()
        if (response.isSuccessful) {

        }
    }

    companion object {
        private const val serialVersionUID = 1L
        const val PREKEY_MINI_NUM = 60
        fun generateKeys(): SignalKeyRequest {
            val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(App.get())
            val oneTimePreKeys = PreKeyUtil.generatePreKeys(App.get())
            val signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(App.get(), identityKeyPair, false)
            return SignalKeyRequest(identityKeyPair.publicKey, signedPreKeyRecord, oneTimePreKeys)
        }
    }
}