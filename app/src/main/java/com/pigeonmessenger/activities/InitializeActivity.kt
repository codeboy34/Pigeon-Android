package com.pigeonmessenger.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.extension.putBoolean
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.AccountViewModel
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable

class InitializeActivity : AppCompatActivity() {

    companion object {
        const val IS_LOADED = "is_loaded"
        private const val TAG = "InitializeActivity"

        fun show(context: Context) {
            context.startActivity(Intent(context, InitializeActivity::class.java))
        }
    }

    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    private val accountViewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this).get(AccountViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize)
        load()
    }


    private fun load() {
        if (count > 0) {
            count--
            accountViewModel.pushAsyncSignalKeys().autoDisposable(scopeProvider).subscribe({
                when {
                    it?.isSuccessful == true -> {
                        Log.d(TAG, "Successfully uploaded : ");
                        defaultSharedPreferences.putBoolean(IS_LOADED, true)
                        SplashActivity.show(this)
                        finish()
                    }
                    it?.code() == ErrorHandler.AUTHENTICATION -> {
                        // App.get().closeAndClear() //TODO clear and close implents
                        finish()
                    }
                    else -> load()
                }
            }, {
                load()
                ErrorHandler.handleError(it)
            })
        } else {
            finish()
        }
    }

    private var count = 2

}
