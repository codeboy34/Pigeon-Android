package com.pigeonmessenger.events

class ContactsSyncEvent{
    var success = false
    var newContacts =0;

    var error = false
    var throwable : Throwable ? =null
    var errorCode : Int ? =null
}