package com.pigeonmessenger.api

import com.pigeonmessenger.api.request.ContactRequest
import com.pigeonmessenger.api.response.ContactResponse
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface ContactsService {

    @POST("/contacts")
    fun syncContacts(@Body contacts: List<ContactRequest>): Call<List<ContactResponse>>

    @GET("avatar/{userid}")
    fun fetchAvatar(@Path("userid") userid: String): Call<ContactResponse>

    @GET("profiles/fetch")
    fun fetchProfiles(): Call<List<ContactResponse>>

    @GET("profile/{userid}")
    fun fetchProfile(@Path("userid") userid: String): Call<ContactResponse>

    @GET("users/fetch")
    fun fetchUsers(list: List<String>): Call<List<ContactResponse>>

    @POST("user/{userid}/block")
    fun block(@Path("userid") userid: String): Observable<Response<Void>>


    @POST("user/{userid}/unblock")
    fun unblock(@Path("userid") userid: String): Observable<Response<Void>>
}