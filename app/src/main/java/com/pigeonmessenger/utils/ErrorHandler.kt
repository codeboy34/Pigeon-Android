package com.pigeonmessenger.utils

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.ServerErrorException
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorHandler {

    companion object {
        private val ctx = App.get()

        fun handleError(e:Throwable){
            App.get().runOnUiThread {
                when(e){
                    is FirebaseException -> firebaseException(e)
                    is HttpException ->{ handleCode(e.code())}
                    is SocketTimeoutException -> ctx.toast(ctx.getString(R.string.error_connection_timeout))
                    is UnknownHostException -> ctx.toast(ctx.getString(R.string.error_no_connection))
                    is ServerErrorException -> ctx.toast("Opps! Something went wrong.")
                }
            }
        }

        private fun firebaseException(e:Throwable){

            when(e) {
                is FirebaseNetworkException -> ctx.toast(ctx.getString(R.string.error_no_connection))
                //is FirebaseAuthInvalidCredentialsException-> ctx.toast()
                else -> ctx.toast(ctx.getString(R.string.error_something_wrong))
            }
        }

        fun handleCode(code :Int){
            ctx.runOnUiThread {
                when (code) {
                    BAD_REQUEST -> {
                    }
                    AUTHENTICATION -> {
                        toast(getString(R.string.error_authentication, AUTHENTICATION))
                      //  MixinApplication.get().closeAndClear()
                    }
                    FORBIDDEN -> {
                        toast(R.string.error_forbidden)
                    }
                   // NOT_FOUND -> {
                     //   toast(getString(R.string.error_not_found, NOT_FOUND))
                  //  }
                    TOO_MANY_REQUEST -> {
                        toast(getString(R.string.error_too_many_request, TOO_MANY_REQUEST))
                    }
                    SERVER -> {
                        toast(R.string.error_server_5xx)
                    }
                    TIME_INACCURATE -> {
                    }
                    else -> {
                        toast(getString(R.string.error_unknown_with_code, code))
                    }
                }
            }
        }


        private const val BAD_REQUEST = 400
        const val AUTHENTICATION = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val TOO_MANY_REQUEST = 429
        private const val SERVER = 500
        const val TIME_INACCURATE = 911
    }
}