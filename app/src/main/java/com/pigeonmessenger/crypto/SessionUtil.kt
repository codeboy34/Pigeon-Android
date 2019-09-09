package com.pigeonmessenger.crypto

import android.content.Context
import com.pigeonmessenger.crypto.storage.MixinSessionStore
import org.whispersystems.libsignal.SignalProtocolAddress

class SessionUtil {
    companion object {
        @JvmStatic
        fun archiveSiblingSessions(context: Context, address: SignalProtocolAddress) {
            val sessionStore = MixinSessionStore(context)
            sessionStore.archiveSiblingSessions(address)
        }
    }
}