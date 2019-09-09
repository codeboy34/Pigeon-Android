package com.pigeonmessenger.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by prem on 10/10/17.
 */

public class PrefHelper {


    public static boolean isSessionExist(Context context) {
        return getSharedPreferences(context).getBoolean("session", false);
    }

    public static void setSession(Context context, boolean session) {
        getSharedPreferences(context).edit().putBoolean("session", session).apply();

    }

    public static boolean isProfileUploaded(Context context) {
        return getSharedPreferences(context).getBoolean("profileUploaded", false);
    }

    public static void setProfileUploaded(Context context) {
        getSharedPreferences(context).edit().putBoolean("profileUploaded", true).apply();
    }

    public static void setIsNodeCreated(Context context) {
        getSharedPreferences(context).edit().putBoolean("isNodeCreated", false).apply();
    }

    public static boolean isNodeCreated(Context context) {
        return getSharedPreferences(context).getBoolean("isNodeCreated", true);
    }

    public static void removeIsNodeCreated(Context context) {
        getSharedPreferences(context).edit().remove("isNodeCreated").apply();
    }

    public static void setMyDisplayName(Context context, String displayName) {
        getSharedPreferences(context, "profile").edit().putString("displayName", displayName).apply();
    }

    public static String getMyDisplayName(Context context) {
        return getSharedPreferences(context).getString("myDisplayName", null);
    }

    public static boolean isContactLoaded(Context context) {
        return getSharedPreferences(context).getBoolean("isContactLoaded", false);
    }

    public static void setContactsLoaded(Context context, boolean b) {
        getSharedPreferences(context).edit().putBoolean("isContactLoaded", b).apply();
    }

    public static void setIsSignUpForFirstTime(Context context) {
        getSharedPreferences(context).edit().putBoolean("isSignUpForFirstTime", true).apply();
    }

    public static boolean IsSignUpForFirstTime(Context context) {
        return getSharedPreferences(context).getBoolean("isSignUpForFirstTime", false);
    }


    public static void removeIsSignUpForFirstTime(Context context) {
        getSharedPreferences(context).edit().remove("isSignUpForFirstTime").apply();

    }

    public static void setPhoneNumber(Context context, String mob) {
        getSharedPreferences(context).edit().putString("registered_mobile", mob).apply();
    }

    public static String getPhoneNumber(Context context) {
        return getSharedPreferences(context).getString("registered_mobile", null);
    }



    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setStatus(Context context, String status) {
        SharedPreferences preferences = getSharedPreferences(context, "profile");
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("status", status);
        editor.apply();

    }

    public static void setAvatar(Context context, String avatar) {
        SharedPreferences preferences = getSharedPreferences(context, "profile");
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("avatar", avatar);
        editor.apply();

    }


    public static SharedPreferences getProfileSharedPref(Context context){
        return getSharedPreferences(context,"profile");
    }
    public static String getStatus(Context context) {
        return getSharedPreferences(context, "profile").getString("status", null);
    }

    public static String getAvatarKey(Context context){
        return getSharedPreferences(context, "profile").getString("avatar", null);
    }

    public static SharedPreferences getSharedPreferences(Context context, String arg1) {
        return context.getSharedPreferences(arg1, Context.MODE_PRIVATE);
    }
}
