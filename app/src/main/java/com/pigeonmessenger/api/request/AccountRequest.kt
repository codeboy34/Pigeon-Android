package com.pigeonmessenger.api.request

import android.os.Build
import com.pigeonmessenger.BuildConfig

 class AccountRequest(
        var user_id:String,
        val registration_id: Int? = null,
        val platform_version: String = Build.VERSION.RELEASE,
        val app_version: String = BuildConfig.VERSION_NAME
)