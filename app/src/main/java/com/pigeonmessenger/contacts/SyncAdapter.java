package com.pigeonmessenger.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by prem on 31/12/17.
 */

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String tag = "SyncAdapter";
    private AccountManager manager;



    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        manager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync: syncStarted");
        //Toast.makeText(getContext(), "SyncStarted", Toast.LENGTH_SHORT).show();
        //PigeonAppContactsManager.INSTANCE.sync(getContext());

    }

    public void addSyncAccount(AccountManager accountManager){
        String accountName="Pigeon";
        String accountType="com.pigeon.auth";
        final Account account = new Account(accountName, accountType);

        accountManager.addAccountExplicitly(account,null,null);
        ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY,true);
        ContentResolver.setIsSyncable(account,ContactsContract.AUTHORITY,1);
    }
}
