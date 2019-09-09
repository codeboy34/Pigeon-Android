package com.pigeonmessenger.utils

import com.bumptech.glide.load.Key

import java.io.UnsupportedEncodingException
import java.security.MessageDigest

class StringSignature(private val signature: String?) : Key {

    init {
        if (signature == null) {
            throw NullPointerException("Signature cannot be null!")
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val that = o as StringSignature?

        return signature == that!!.signature
    }

    override fun hashCode(): Int {
        return signature!!.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        try {
            messageDigest.update(signature!!.toByteArray(charset(Key.STRING_CHARSET_NAME)))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    override fun toString(): String {
        return ("StringSignature{" +
            "signature='" + signature + '\''.toString() +
            '}'.toString())
    }
}
