package com.pigeonmessenger.job

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import android.os.PowerManager
import androidx.core.net.ConnectivityManagerCompat
import android.util.Log
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil


 class JobNetworkUtil(context: Context, _linkState :LinkState) :NetworkUtil, NetworkEventProvider {

    var TAG = "JobNetwork"
     var linkState = _linkState

    var networkProviderListener  : NetworkEventProvider.Listener? = null

    override fun setListener(listener: NetworkEventProvider.Listener?) {
        this.networkProviderListener = listener
    }

    init {
        Log.d(TAG, "------===============JobNetworkUtil-----------------------------------: ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            listenForIdle(context)
        }
        listenNetworkViaConnectivityManager(context)

        linkState.observeForever { dispatchNetworkChange(context) }
    }


    @TargetApi(23)
    private fun listenForIdle(context: Context) {
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "onReceive-- listenForIdle---------------")
                dispatchNetworkChange(context)
            }
        }, IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED))
    }

    private fun listenNetworkViaConnectivityManager(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG,"onAvailable() --NetworkChange")
                dispatchNetworkChange(context)
            }
        })
    }

    internal fun dispatchNetworkChange(context: Context) {
        if (networkProviderListener == null) { // shall not be but just be safe
            return
        }
        // http://developer.android.com/reference/android/net/ConnectivityManager.html#EXTRA_NETWORK_INFO
        // Since NetworkInfo can vary based on UID, applications should always obtain network information
        // through getActiveNetworkInfo() or getAllNetworkInfo().
        Log.d(TAG, "dispatchNetworkChange()")
        networkProviderListener!!.onNetworkChange(getNetworkStatus(context))
    }

     public fun isInternetConnected(context: Context):Boolean{
         return getNetworkStatus(context)!=NetworkUtil.DISCONNECTED
     }

    override  fun getNetworkStatus(context: Context): Int {
        if (isDozing(context)) {
            return NetworkUtil.DISCONNECTED
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo: NetworkInfo?
        try {
            netInfo = cm.activeNetworkInfo
            if (netInfo == null) return NetworkUtil.DISCONNECTED
        } catch (t: Throwable) {
            return NetworkUtil.DISCONNECTED
        }
        val metered = try {
            ConnectivityManagerCompat.isActiveNetworkMetered(cm)
        } catch (e: Exception) {
            return NetworkUtil.DISCONNECTED
        }
        if (netInfo.isConnected) {
            if (LinkState.isOnline(linkState.state)) {
                return NetworkUtil.WEB_SOCKET
            }
            return if (!metered) {
                NetworkUtil.UNMETERED
            } else {
                NetworkUtil.METERED
            }
        } else {
            return NetworkUtil.DISCONNECTED
        }
    }


    @TargetApi(23)
    private fun isDozing(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isDeviceIdleMode && !powerManager.isIgnoringBatteryOptimizations(
                    context.packageName
            )
        } else {
            return false
        }
    }




}