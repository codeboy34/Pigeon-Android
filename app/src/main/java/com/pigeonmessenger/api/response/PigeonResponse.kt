package com.pigeonmessenger.api.response

import com.pigeonmessenger.api.ResponseError
import retrofit2.Response

class PigeonResponse<T>() {

    constructor(response: Response<T>) : this() {
        if (response.isSuccessful) {
            if (response.body()!=null)
            data = response.body()
        } else {
            error = ResponseError(response.code(), response.code(), response.errorBody().toString())
        }
    }

    constructor(response: Throwable) : this() {
        error = ResponseError(500, 500, response.message ?: "")
    }

    var data: T? = null
    var error: ResponseError? = null
    var prev: String? = null
    var next: String? = null

    val isSuccess: Boolean
        get() = error == null

    val errorCode: Int
        get() = if (error != null) error!!.code else 0

    val errorDescription: String
        get() = if (error != null) error!!.description else ""
}