package com.pigeonmessenger.viewmodals

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.AccountService
import com.pigeonmessenger.api.SignalKeyService
import com.pigeonmessenger.api.request.AccountRequest
import com.pigeonmessenger.api.request.AccountUpdateRequest
import com.pigeonmessenger.api.request.ConversationRequest
import com.pigeonmessenger.api.request.SignalKeyRequest
import com.pigeonmessenger.database.room.daos.ContactsDao
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.job.ConversationJob
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.job.RefreshOneTimePreKeysJob
import com.pigeonmessenger.job.SendEventJob
import com.pigeonmessenger.repo.AccountRepo
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.SINGLE_DB_THREAD
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import retrofit2.Response
import javax.inject.Inject

class AccountViewModel : ViewModel() {

    init {
        App.get().appComponent.inject(this)
    }

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var accountRepo: AccountRepo

    @Inject
    lateinit var jabManager: PigeonJobManager

    @Inject
    lateinit var signalKeyService: SignalKeyService

    @Inject
    lateinit var conversatonRepo: ConversationRepo

    @Inject
    lateinit var contactsDao: ContactsDao

    fun updateAccount(accountUpdateRequest: AccountUpdateRequest) =
            accountRepo.update(accountUpdateRequest).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

    fun removeAvatar()=accountRepo.removeAvatar().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun verify(accountRequest: AccountRequest) = accountRepo.login(accountRequest).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())!!

    fun sendEventMessage() {
        jabManager.addJobInBackground(SendEventJob())
    }

    fun pushAsyncSignalKeys(): Observable<Response<Void>?> {
        val start = System.currentTimeMillis()
        var signalKeys: SignalKeyRequest? = null
        return Observable.just(App.get()).observeOn(Schedulers.io()).subscribeOn(Schedulers.io()).flatMap {
            if (signalKeys == null) {
                signalKeys = RefreshOneTimePreKeysJob.generateKeys()
            }
            val response = signalKeyService.pushSignalKeys(signalKeys!!).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "key Upload success: ");
            }
            val time = System.currentTimeMillis() - start
            if (time < 2000) {
                Thread.sleep(time)
            }
            Observable.just(response)
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
    }

    fun updateGroup(request: ConversationRequest) {
        jabManager.addJobInBackground(ConversationJob(request, request.conversationId, type = ConversationJob.TYPE_UPDATE))
    }

    fun updateGroupName(conversationId: String, name: String) {
        conversatonRepo.updateGroupName(conversationId, name)
    }

    fun pingServer(callback: () -> Unit, elseCallBack: (e: Exception?) -> Unit): Job {
        return GlobalScope.launch {
            try {
                val response = accountService.ping().execute()

                response.headers()["X-Server-Time"]?.toLong()?.let { serverTime ->
                    if (Math.abs(serverTime / 1000000 - System.currentTimeMillis()) < 600000L) { // 10 minutes
                        withContext(Dispatchers.Main) {
                            callback.invoke()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            elseCallBack.invoke(null)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    elseCallBack.invoke(e)
                }
            }
        }
    }

    fun insertMe(user: User) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            contactsDao.insert(user)
        }
    }



    companion object {
        const val TAG = "AccountViewModel"
    }

}