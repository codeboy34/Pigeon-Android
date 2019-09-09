package com.pigeonmessenger.api

import com.pigeonmessenger.api.request.SignalKeyRequest
import com.pigeonmessenger.api.response.SignalKeyCount
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SignalKeyService {

    @POST("signal/keys")
    fun pushSignalKeys(@Body signalKeyRequest: SignalKeyRequest): Call<Void>

    @GET("signal/prekeys/count")
    fun getSignalKeyCount(): Call<SignalKeyCount>

    @GET("signal/keys/{userid}")
    fun consumeSignalKeys(@Path("userid") userId :String) : Call<List<SignalKey>>

    @POST("prekeybundle/fetch")
    fun consumeSignalKeys(@Body users:List<String>) : Call<List<SignalKey>>

}