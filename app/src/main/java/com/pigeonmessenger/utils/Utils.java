package com.pigeonmessenger.utils;

import android.content.Context;

import java.util.Random;

public class Utils {

    public static int dp2px(Context context ,float dips)
    {
        return (int) (dips * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static String genrateMessageId(){
        String str = getSaltString();
        String l = String.valueOf(System.currentTimeMillis());
        l = l.substring(l.length()/2);
        return str+"-"+l;
    }


    private static String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 8) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

}


