package com.pigeonmessenger.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by prem on 29/10/17.
 */

public class NetworkUtil {

    public static boolean isNetworkConnected(Context context){
        ConnectivityManager manager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info= manager.getActiveNetworkInfo();
        return info!=null && info.isConnected();
    }
}
