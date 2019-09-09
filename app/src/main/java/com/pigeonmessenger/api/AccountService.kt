package com.pigeonmessenger.api

import com.pigeonmessenger.api.request.AccountRequest
import com.pigeonmessenger.api.request.AccountUpdateRequest
import com.pigeonmessenger.vo.Account
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface AccountService{

    @POST("profile/me")
    fun update(@Body accountUpdateRepo : AccountUpdateRequest) : Observable<Response<Unit>>

    @POST("login")
    fun verification(@Body accountRequest: AccountRequest) : Observable<Response<Account>>


    @POST("/profile/remove")
    fun removeAvatar():Observable<Response<Unit>>

    @GET("/")
    fun ping(): Call<Response<Void>>

}