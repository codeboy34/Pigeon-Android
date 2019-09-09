package com.pigeonmessenger.crypto

import android.content.Context
import android.util.Log
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.crypto.storage.MixinSenderKeyStore
import com.pigeonmessenger.crypto.storage.SignalProtocolStoreImpl
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.vo.MessageEntity
import org.whispersystems.libsignal.*
import org.whispersystems.libsignal.SessionCipher.SESSION_LOCK
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.CiphertextMessage.*
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import org.whispersystems.libsignal.state.PreKeyBundle

class SignalProtocol(ctx: Context) {

    data class ComposeMessageData(
            val keyType: Int,
            val cipher: ByteArray,
            val resendMessageId: String? = null
    )

    companion object {

        val TAG = SignalProtocol::class.java.simpleName
        const val DEFAULT_DEVICE_ID = 1

        fun initSignal(context: Context) {
            IdentityKeyUtil.generateIdentityKeys(context)
        }

        fun encodeMessageData(data: ComposeMessageData): String {
            return if (data.resendMessageId == null) {
                val header = byteArrayOf(CURRENT_VERSION.toByte(), data.keyType.toByte(), 0, 0, 0, 0, 0, 0)
                val cipherText = header + data.cipher
                Base64.encodeBytes(cipherText)
            } else {
                val header = byteArrayOf(CURRENT_VERSION.toByte(), data.keyType.toByte(), 1, 0, 0, 0, 0, 0)
                val messageId = data.resendMessageId.toByteArray()
                val cipherText = header + messageId + data.cipher
                Base64.encodeBytes(cipherText)
            }
        }

        fun decodeMessageData(encoded: String): ComposeMessageData {
            val cipherText = Base64.decode(encoded)
            val header = cipherText.sliceArray(IntRange(0, 7))
            val version = header[0].toInt()
            if (version != CURRENT_VERSION) {
                throw InvalidMessageException("Unknown version: $version")
            }
            val dataType = header[1].toInt()
            val isResendMessage = header[2].toInt() == 1
            return if (isResendMessage) {
                val messageId = String(cipherText.sliceArray(IntRange(8, 43)))
                val data = cipherText.sliceArray(IntRange(44, cipherText.size - 1))
                ComposeMessageData(dataType, data, messageId)
            } else {
                val data = cipherText.sliceArray(IntRange(8, cipherText.size - 1))
                ComposeMessageData(dataType, data, null)
            }
        }
    }

    private val signalProtocolStore = SignalProtocolStoreImpl(App.get())
    private val senderKeyStore: MixinSenderKeyStore = MixinSenderKeyStore(ctx)

    fun encryptSenderKey(conversationId: String, recipientId: String): EncryptResult {
        val senderKeyDistributionMessage = getSenderKeyDistribution(conversationId, Session.getUserId())
        return try {
            val cipherMessage = encryptSession(senderKeyDistributionMessage.serialize(), recipientId)
            val compose = ComposeMessageData(cipherMessage.type, cipherMessage.serialize())
            val cipher = encodeMessageData(compose)
            EncryptResult(cipher, senderKeyDistributionMessage.id, false)
        } catch (e: UntrustedIdentityException) {
            val remoteAddress = SignalProtocolAddress(recipientId, DEFAULT_DEVICE_ID)
            signalProtocolStore.removeIdentity(remoteAddress)
            signalProtocolStore.deleteSession(remoteAddress)
            EncryptResult(null, null, true)
        }
    }


    fun encryptPrivateMessage(recipientId: String, data: String): String {
        val chiper = SessionCipher(signalProtocolStore, SignalProtocolAddress(recipientId, DEFAULT_DEVICE_ID))
        val composeMsg = chiper.encrypt(data.toByteArray())
        val dec = chiper.decrypt(SignalMessage(composeMsg.serialize()))
        Log.d(TAG, "$dec: ");
        return Base64.encodeBytes(composeMsg.serialize())
    }

    fun decryptPrivateMessage(recipientId: String, data: String): String {
        val chiper = SessionCipher(signalProtocolStore, SignalProtocolAddress(recipientId, DEFAULT_DEVICE_ID))
        return chiper.decrypt(SignalMessage(Base64.decode(data))).toString(charset("UTF-8"))
    }

    private fun encryptSession(content: ByteArray, destination: String): CiphertextMessage {
        val remoteAddress = SignalProtocolAddress(destination, DEFAULT_DEVICE_ID)
        val sessionCipher = SessionCipher(signalProtocolStore, remoteAddress)
        return sessionCipher.encrypt(content)
    }

    fun decrypt(
            groupId: String,
            senderId: String,
            dataType: Int,
            cipherText: ByteArray,
            category: String,
            callback: DecryptionCallback
    ) {
        val address = SignalProtocolAddress(senderId, DEFAULT_DEVICE_ID)
        val sessionCipher = SessionCipher(signalProtocolStore, address)
        if (category == MessageCategory.SIGNAL_KEY.name) {
            if (dataType == PREKEY_TYPE) {
                sessionCipher.decrypt(PreKeySignalMessage(cipherText)) { plaintext ->
                    processGroupSession(groupId, address, SenderKeyDistributionMessage(plaintext))
                    callback.handlePlaintext(plaintext)
                }
            } else if (dataType == WHISPER_TYPE) {
                sessionCipher.decrypt(SignalMessage(cipherText)) { plaintext ->
                    processGroupSession(groupId, address, SenderKeyDistributionMessage(plaintext))
                    callback.handlePlaintext(plaintext)
                }
            }
        } else {
            when (dataType) {
                PREKEY_TYPE -> sessionCipher.decrypt(PreKeySignalMessage(cipherText), callback)
                WHISPER_TYPE -> sessionCipher.decrypt(SignalMessage(cipherText), callback)
                SENDERKEY_TYPE -> decryptGroupMessage(groupId, address, cipherText, callback)
                else -> throw InvalidMessageException("Unknown type: $dataType")
            }
        }
    }

    private fun getSenderKeyDistribution(groupId: String, senderId: String): SenderKeyDistributionMessage {
        val senderKeyName = SenderKeyName(groupId, SignalProtocolAddress(senderId, DEFAULT_DEVICE_ID))
        val senderKeyRecord = senderKeyStore.loadSenderKey(senderKeyName)
        return if (senderKeyRecord.isEmpty) {
            val builder = GroupSessionBuilder(senderKeyStore)
            builder.create(senderKeyName)
        } else {
            val state = senderKeyRecord.senderKeyState
            SenderKeyDistributionMessage(state.keyId,
                    state.senderChainKey.iteration,
                    state.senderChainKey.seed,
                    state.signingKeyPublic)
        }
    }

    fun isExistSenderKey(groupId: String, senderId: String): Boolean {
        val senderKeyName = SenderKeyName(groupId, SignalProtocolAddress(senderId, DEFAULT_DEVICE_ID))
        val senderKeyRecord = senderKeyStore.loadSenderKey(senderKeyName)
        return !senderKeyRecord.isEmpty
    }

    fun containsSession(recipientId: String): Boolean {
        val signalProtocolAddress = SignalProtocolAddress(recipientId, SignalProtocol.DEFAULT_DEVICE_ID)
        return signalProtocolStore.containsSession(signalProtocolAddress)
    }

    fun clearSenderKey(groupId: String, senderId: String) {
        val senderKeyName = SenderKeyName(groupId, SignalProtocolAddress(senderId, DEFAULT_DEVICE_ID))
        senderKeyStore.removeSenderKey(senderKeyName)
    }

    fun removeSession(userId: String) {
        synchronized(SESSION_LOCK) {
            val signalProtocolAddress = SignalProtocolAddress(userId, SignalProtocol.DEFAULT_DEVICE_ID)
            signalProtocolStore.deleteSession(signalProtocolAddress)
            signalProtocolStore.removeIdentity(signalProtocolAddress)
        }
    }

    fun processSession(userId: String, preKeyBundle: PreKeyBundle) {
        val signalProtocolAddress = SignalProtocolAddress(userId, SignalProtocol.DEFAULT_DEVICE_ID)
        val sessionBuilder = SessionBuilder(signalProtocolStore, signalProtocolAddress)
        try {
            Log.d(TAG, "processing : ");
            sessionBuilder.process(preKeyBundle)
            Log.d(TAG, "Processed successfully : ");
        } catch (e: UntrustedIdentityException) {
            Log.d(TAG, "process Exception :${e} ");
            signalProtocolStore.removeIdentity(signalProtocolAddress)
            sessionBuilder.process(preKeyBundle)
        }
    }

    fun encryptSessionMessage(message: MessageEntity, conversationId: String, resendMessageId: String? = null): String {
        val cipher = encryptSession(message.message!!.toByteArray(), conversationId)
        return encodeMessageData(ComposeMessageData(cipher.type, cipher.serialize(), resendMessageId))
    }

    fun decryptSessionMessage(
            conversationId: String,
            senderId: String,
            dataType: Int,
            cipherText: ByteArray,
            callback: DecryptionCallback
    ) {
        val address = SignalProtocolAddress(senderId, DEFAULT_DEVICE_ID)
        val sessionCipher = SessionCipher(signalProtocolStore, address)
        when (dataType) {
            PREKEY_TYPE -> sessionCipher.decrypt(PreKeySignalMessage(cipherText), callback)
            WHISPER_TYPE -> sessionCipher.decrypt(SignalMessage(cipherText), callback)
            SENDERKEY_TYPE -> decryptGroupMessage(conversationId, address, cipherText, callback)
            else -> throw InvalidMessageException("Unknown type: $dataType")
        }
    }



        fun encryptGroupMessage(message: MessageEntity, conversationId: String? = null, resendMessageId: String? = null): String {
            val address = SignalProtocolAddress(Session.getUserId(), DEFAULT_DEVICE_ID)
            val senderKeyName = SenderKeyName(message.conversationId, address)
            val groupCipher = GroupCipher(senderKeyStore, senderKeyName)
            var cipher = byteArrayOf(0)
            try {
                cipher = groupCipher.encrypt(message.message!!.toByteArray())
            } catch (e: NoSessionException) {
                Log.e(TAG, "NoSessionException", e)
            }

            return encodeMessageData(ComposeMessageData(SENDERKEY_TYPE, cipher, resendMessageId))

        }
    private fun processGroupSession(
            groupId: String,
            address: SignalProtocolAddress,
            senderKeyDM: SenderKeyDistributionMessage
    ) {
        val builder = GroupSessionBuilder(senderKeyStore)
        val senderKeyName = SenderKeyName(groupId, address)
        builder.process(senderKeyName, senderKeyDM)
    }

    private fun decryptGroupMessage(
            groupId: String,
            address: SignalProtocolAddress,
            cipherText: ByteArray,
            callback: DecryptionCallback
    ): ByteArray {
        val senderKeyName = SenderKeyName(groupId, address)
        val groupCipher = GroupCipher(senderKeyStore, senderKeyName)
        return groupCipher.decrypt(cipherText, callback)
    }
}