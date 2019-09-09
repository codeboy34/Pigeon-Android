package com.pigeonmessenger.api

import com.pigeonmessenger.api.request.SettingRequest
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SettingsService {

    @POST("settings")
    fun updateSetting(@Body settingRequest: SettingRequest) : Observable<Response<Unit>>

    @GET("settings")
    fun getSettings() : Call<SettingRequest>


}