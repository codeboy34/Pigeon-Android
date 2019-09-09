package com.pigeonmessenger.api

import com.pigeonmessenger.api.request.ConversationRequest
import com.pigeonmessenger.api.request.ParticipantRequest
import com.pigeonmessenger.api.response.ConversationResponse
import com.pigeonmessenger.api.response.IconResponse
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ConversationService {

    @POST("conversations")
    fun create(@Body request: ConversationRequest): Call<ConversationResponse>

    @GET("conversations/{id}")
    fun getConversation(@Path("id") id: String): Call<ConversationResponse>

    @GET("conversation/{id}")
    fun findConversation(@Path("id") id: String): Call<ConversationResponse>

    @POST("conversations/{id}/participants/{action}")
    fun participants(
            @Path("id") id: String,
            @Path("action") action: String,
            @Body requests: List<ParticipantRequest>
    ): Call<ConversationResponse>

    @POST("conversations/{id}")
    fun update(@Path("id") id: String, @Body request: ConversationRequest):
            Call<ConversationResponse>

    @POST("conversations/{id}")
    fun updateAsync(@Path("id") id: String, @Body request: ConversationRequest): Observable<Response<ConversationResponse>>

    @POST("conversations/{id}/exit")
    fun exit(@Path("id") id: String): Call<ConversationResponse>

    @POST("conversations/{code_id}/join")
    fun join(@Path("code_id") codeId: String): Observable<ConversationResponse>

    @POST("conversations/{id}/rotate")
    fun rotate(@Path("id") id: String): Observable<ConversationResponse>

    @POST("conversations/{id}/mute")
    fun mute(@Path("id") id: String, @Body request: ConversationRequest):
            Call<ConversationResponse>

    @GET("conversation/icon/{id}")
    fun iconDownload(@Path("id") id: String) : Call<IconResponse>


    @GET("conversation/name/{id}")
    fun name(@Path("id") id: String) : Call<IconResponse>

}