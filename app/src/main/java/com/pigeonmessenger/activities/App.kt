package com.pigeonmessenger.activities

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.jakewharton.threetenabp.AndroidThreeTen
import com.pigeonmessenger.di.AppComponent
import com.pigeonmessenger.di.AppModule
import com.pigeonmessenger.di.DaggerAppComponent
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.extension.putBoolean
import com.pigeonmessenger.services.NetworkService
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.vo.OnlineStatus
import com.pigeonmessenger.webrtc.CallService
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.ctx
import org.jetbrains.anko.notificationManager
import java.util.concurrent.atomic.AtomicBoolean
import android.os.StrictMode.VmPolicy
import android.os.StrictMode
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider


class App : Application(), LifecycleObserver {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build())

        StrictMode.setVmPolicy(VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build())

        super.onCreate()
        instance = this
        AndroidThreeTen.init(this)

        appComponent = DaggerAppComponent.builder()
                .appModule(AppModule())
                .build()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        EmojiManager.install(GoogleEmojiProvider())
    }

    var onlining = AtomicBoolean(false)

    fun gotoTimeWrong(serverTime: Long) {
        if (onlining.compareAndSet(true, false)) {
            val ise = IllegalStateException("Time error: Server-Time $serverTime - Local-Time ${System.currentTimeMillis()}")
            //   Crashlytics.logException(ise)
            NetworkService.stopService(ctx)
            CallService.disconnect(ctx)
            notificationManager.cancelAll()
            defaultSharedPreferences.putBoolean(Constant.Account.PREF_WRONG_TIME, true)
            SplashActivity.show(this)
        }
    }

    companion object {
        private var TAG: String = "App"

        private var instance: App? = null

        @JvmStatic
        fun get(): App {
            return instance!!
        }


        @JvmStatic
        var conversationWith: String? = null

        var isAppRunning = false

    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        isAppRunning = false
        EventBus.getDefault().post(OnlineStatus.OFFLINE)
        Log.d("MyApp", "App in background")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        isAppRunning = true
        EventBus.getDefault().post(OnlineStatus.ONLINE)
        Log.d("MyApp", "App in foreground")
    }

}