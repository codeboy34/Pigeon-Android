package com.pigeonmessenger.utils

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.CountDownLatch

class AttachmentUtil{
    val firebaseStorage = FirebaseStorage.getInstance()
    val attachmentRef = firebaseStorage.reference.child("attachment")


    fun uploadAttachment(uri: Uri ,key:String, listener : (Long,Long)->Unit):Int{
        var result = 0;

        var latch = CountDownLatch(1)
        //attachmentRef.putFile(uri,)
        val ref= attachmentRef.child(key).putFile(uri)
                .addOnCompleteListener {
                    result = 1
                    latch.countDown()
                }.addOnCanceledListener {
                    result = -1
                    latch.countDown()
                }.addOnProgressListener {
                    listener(it.bytesTransferred,it.totalByteCount)
                }.addOnPausedListener {

                }
        latch.await()
        return result
    }
}