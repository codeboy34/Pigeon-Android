package com.pigeonmessenger.api

import java.io.IOException

class ServerErrorException(val code: Int) : IOException() {

    fun shouldRetry(): Boolean {
        return true
    }
}