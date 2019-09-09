package com.pigeonmessenger.contacts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * Created by prem on 1/1/18.
 */

public class ContactsSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    private static  SyncAdapter syncAdapter=null;
    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (syncAdapter == null)
                syncAdapter = new SyncAdapter(getApplicationContext(), true);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
