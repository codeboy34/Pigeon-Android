package com.pigeonmessenger.crypto.storage

import android.content.Context
import android.util.Log
import com.pigeonmessenger.Session
import com.pigeonmessenger.crypto.SessionUtil
import com.pigeonmessenger.crypto.db.IdentityDao
import com.pigeonmessenger.crypto.db.SignalDatabase
import com.pigeonmessenger.crypto.vo.Identity
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.IdentityKeyStore

class MixinIdentityKeyStore(private val context: Context) : IdentityKeyStore {

    private val dao: IdentityDao = SignalDatabase.getDatabase(context).identityDao()

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val identity = dao.getIdentity(address.toString())
        return identity?.getIdentityKey()
    }

    override fun getIdentityKeyPair(): IdentityKeyPair {
        return dao.getLocalIdentity().getIdentityKeyPair()
    }

    override fun getLocalRegistrationId(): Int {
        return dao.getLocalIdentity().registrationId!!
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        return saveIdentityKey(address, identityKey)
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        synchronized(LOCK) {
            val ourNumber = Session.getUserId() //TODO
            val theirAddress = address.name

            if (ourNumber == null) {
                return false
            }
            if (ourNumber == address.name) {
                return identityKey == dao.getLocalIdentity().getIdentityKey()
            }

            return when (direction) {
                IdentityKeyStore.Direction.SENDING -> isTrustedForSending(identityKey, dao.getIdentity(theirAddress))
                IdentityKeyStore.Direction.RECEIVING -> true
                else -> throw AssertionError("Unknown direction: $direction")
            }
        }
    }

    private fun saveIdentityKey(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        synchronized(LOCK) {
            val signalAddress = address.name
            val identity = dao.getIdentity(signalAddress)
            if (identity == null) {
                Log.w(TAG, "Saving new identity..." + address.toString())
                dao.insert(Identity(signalAddress, null, identityKey.serialize(), null, null, System.currentTimeMillis()))
                return true
            }

            if (identity.getIdentityKey() != identityKey) {
                Log.w(TAG, "Replacing existing identity..." + address.toString())
                dao.insert(Identity(signalAddress, null, identityKey.serialize(), null, null, System.currentTimeMillis()))
                SessionUtil.archiveSiblingSessions(context, address)
                return true
            }
            return false
        }
    }

    private fun isTrustedForSending(identityKey: IdentityKey, identity: Identity?): Boolean {
        if (identity == null) {
            Log.w(TAG, "Nothing here, returning true...")
            return true
        }

        if (identityKey != identity.getIdentityKey()) {
            Log.w(TAG, "Identity keys don't match...")
            return false
        }
        return true
    }

    fun removeIdentity(address: SignalProtocolAddress) {
        synchronized(LOCK) {
            dao.deleteIdentity(address.name)
        }
    }

    companion object {

        private val TAG = MixinIdentityKeyStore::class.java.simpleName
        private val LOCK = Any()
    }
}