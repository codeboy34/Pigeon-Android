package com.pigeonmessenger.events


class SocketConnectionEvents {
    lateinit var eventsType : ConnectionEventType

    enum class ConnectionEventType(var i :Int){
        CONNECTED(0),DISCONNECTED(1),AUTH_SUCCESS(2),AUTH_FAILED(3)
    }
}