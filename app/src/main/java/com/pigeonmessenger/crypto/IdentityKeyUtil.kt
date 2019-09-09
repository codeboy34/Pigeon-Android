package com.pigeonmessenger.crypto

import android.content.Context
import com.pigeonmessenger.crypto.vo.Identity
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.pigeonmessenger.crypto.db.SignalDatabase
import org.whispersystems.libsignal.util.KeyHelper

open class IdentityKeyUtil {

    companion object {

        fun generateIdentityKeys(ctx: Context) {
            val registrationId = KeyHelper.generateRegistrationId(false)
            CryptoPreference.setLocalRegistrationId(ctx, registrationId)
            val identityKeyPair = KeyHelper.generateIdentityKeyPair()
            val identity = Identity("-1",
                registrationId,
                identityKeyPair.publicKey.serialize(),
                identityKeyPair.privateKey.serialize(),
                0,
                System.currentTimeMillis())
            GlobalScope.launch(SINGLE_DB_THREAD) {
                SignalDatabase.getDatabase(ctx).identityDao().insert(identity)
            }
        }

        fun getIdentityKeyPair(context: Context) =
                SignalDatabase.getDatabase(context).identityDao().getLocalIdentity().getIdentityKeyPair()

        fun getIdentityKey(context: Context) =
                SignalDatabase.getDatabase(context).identityDao().getLocalIdentity().getIdentityKey()
    }
}