package com.pigeonmessenger.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pigeonmessenger.utils.NetworkUtil;


/**
 * Created by prem on 29/10/17.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";
    private static final String CONNECTED = "NETWORK CONNECTED";
    private static final String DISCONNECTED = "NETWORK DISCONNECTED";
    private static boolean firstConnect = true;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (NetworkUtil.isNetworkConnected(context)) {
            if (firstConnect) {
                Log.e(TAG, CONNECTED);
              //  context.startService(new Intent("action.CONNECT", null, context, MessageService.class));
                firstConnect = false;
            }
        } else {
            firstConnect = true;
            Log.e(TAG, DISCONNECTED);
        }
    }
}
