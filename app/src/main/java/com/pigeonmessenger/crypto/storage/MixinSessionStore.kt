package com.pigeonmessenger.crypto.storage

import android.content.Context
import android.util.Log
import com.pigeonmessenger.crypto.SignalProtocol
import com.pigeonmessenger.crypto.db.SessionDao
import com.pigeonmessenger.crypto.vo.Session
import com.pigeonmessenger.crypto.db.SignalDatabase
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore

import java.io.IOException

class MixinSessionStore(context: Context) : SessionStore {

    private val sessionDao: SessionDao = SignalDatabase.getDatabase(context).sessionDao()

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        synchronized(FILE_LOCK) {
            val session = sessionDao.getSession(address.name, address.deviceId)
            if (session != null) {
                try {
                    return SessionRecord(session.record)
                } catch (e: IOException) {
                    Log.w(TAG, "No existing session information found.")
                }
            }
            return SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int>? {
        synchronized(FILE_LOCK) {
            return sessionDao.getSubDevice(name)
        }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        synchronized(FILE_LOCK) {
            sessionDao.insert(Session(address.name, address.deviceId, record.serialize(), System.currentTimeMillis()))
        }
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        synchronized(FILE_LOCK) {
            sessionDao.getSession(address.name, address.deviceId) ?: return false
            val sessionRecord = loadSession(address)

            return sessionRecord.sessionState.hasSenderChain() && sessionRecord.sessionState.sessionVersion == CiphertextMessage.CURRENT_VERSION
        }
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        synchronized(FILE_LOCK) {
            val session = sessionDao.getSession(address.name, address.deviceId)
            if (session != null) {
                sessionDao.delete(session)
            }
        }
    }

    override fun deleteAllSessions(name: String) {
        synchronized(FILE_LOCK) {
            val devices = getSubDeviceSessions(name)

            deleteSession(SignalProtocolAddress(name, SignalProtocol.DEFAULT_DEVICE_ID))

            for (device in devices!!) {
                deleteSession(SignalProtocolAddress(name, device))
            }
        }
    }

    fun archiveSiblingSessions(address: SignalProtocolAddress) {
        synchronized(FILE_LOCK) {
            val sessions = sessionDao.getSessions(address.name)
            try {
                for (row in sessions!!) {
                    if (row.device != address.deviceId) {
                        val record = SessionRecord(row.record)
                        record.archiveCurrentState()
                        storeSession(SignalProtocolAddress(row.address, row.device), record)
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "archiveSiblingSessions new SessionRecord failed")
            }
        }
    }

    companion object {

        private val TAG = MixinSessionStore::class.java.simpleName
        private val FILE_LOCK = Any()
    }
}
