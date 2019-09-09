package com.pigeonmessenger.manager;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class FirebaseTokenManager {

    private static String TAG = "FirebaseTokenManager";

    private static FirebaseTokenManager instance;

    private FirebaseTokenManager() {
    }

    public synchronized static FirebaseTokenManager getInstance() {
        if (instance == null)
            instance = new FirebaseTokenManager();
        return instance;
    }

    public String getToken() throws Exception {
        final BlockingQueue<Object> blockingQueue = new SynchronousQueue<>();
        FirebaseAuth.getInstance().getAccessToken(true).addOnCompleteListener(task -> {
            try {
                if (task.isSuccessful()) {
                    String token = task.getResult().getToken();
                    Log.d(TAG, "getToken:success");
                    blockingQueue.put(token);
                }
            } catch (InterruptedException e) {

            }
        }).addOnFailureListener(e -> {
            try {
                Log.d(TAG, "getToken: onFailer ");
                blockingQueue.put(e);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        });

        Object result = blockingQueue.take();

        if (result == null)
            throw new Exception();
        if (result instanceof String)
            return (String) result;
        else if (result instanceof Exception) {
            Exception exception = (Exception) result;
            throw exception;
        } else throw new Exception();

    }
}
