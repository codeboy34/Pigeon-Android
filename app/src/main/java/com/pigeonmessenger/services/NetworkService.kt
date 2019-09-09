package com.pigeonmessenger.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.room.InvalidationTracker
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.database.room.daos.FloodMessageDao
import com.pigeonmessenger.database.room.dbs.MessageRoomDatabase
import com.pigeonmessenger.extension.findFloodMessageDeferred
import com.pigeonmessenger.job.*
import com.pigeonmessenger.manager.FirebaseTokenManager
import com.pigeonmessenger.manager.SocketManager
import com.pigeonmessenger.vo.CallState
import com.pigeonmessenger.vo.OnlineStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import java.util.concurrent.Executors
import javax.inject.Inject


class NetworkService : Service(), NetworkEventProvider.Listener, SocketManager.SocketObserver {

    override fun onOpen() {
        jobManager.addJobInBackground(RefreshContactsJob())
        if (App.isAppRunning) onSeenEvent(OnlineStatus.ONLINE)
    }

    override fun onClose() {
        if (App.isAppRunning) onSeenEvent(OnlineStatus.OFFLINE)
        doConnect()
    }

    companion object {
        private const val TAG = "NetworkService"
        fun stopService(ctx: Context) {
            val intent = Intent(ctx, NetworkService::class.java)
            ctx.stopService(intent)
        }
    }

    @Inject
    lateinit var jobManager: PigeonJobManager

    @Inject
    lateinit var socketManager: SocketManager

    @Inject
    lateinit var jobNetworkUtil: JobNetworkUtil

    @Inject
    lateinit var floodMessageDao: FloodMessageDao

    @Inject
    lateinit var messageDatabase: MessageRoomDatabase

    @Inject
    lateinit var linkState: LinkState

    private var floodJob: Job? = null

    @Inject
    lateinit var callState: CallState

    private var onlineStatus: OnlineStatus = OnlineStatus.OFFLINE

    private val messageDecrypt by lazy { DecryptMessage() }

    private val callDecrypt by lazy {
        DecryptCallMessage(callState)
    }

    private val floodThread by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    override fun onNetworkChange(networkStatus: Int) {
        if (networkStatus != NetworkUtil.DISCONNECTED) {
            doConnect()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSeenEvent(status: OnlineStatus) {
        Log.d(TAG, "onSeenEvent: ${status} " );
        if (onlineStatus != status) {
            if (LinkState.isOnline(linkState.state)) {
                socketManager.changeOnlineStatus(status == OnlineStatus.ONLINE)
                onlineStatus = status
            } else if (status == OnlineStatus.OFFLINE) onlineStatus = OnlineStatus.OFFLINE
        }
    }


    private fun doConnect() {
        Log.d(TAG, "doConnect: ");

        if (jobNetworkUtil.isInternetConnected(applicationContext))
            doAsync {
                val token = FirebaseTokenManager.getInstance().token
                val userId = Session.getUserId()
                socketManager.connect(token, userId)
            }
    }


    override fun onCreate() {
        super.onCreate()
        App.get().appComponent.inject(this)
        jobNetworkUtil.setListener(this)
        socketManager.socketObserver = this
        startFloodJob()
        doConnect()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private val floodObserver =
            object : InvalidationTracker.Observer("flood_messages") {
                override fun onInvalidated(tables: MutableSet<String>) {
                    runFloodJob()
                }
            }


    private fun startFloodJob() {
        messageDatabase.invalidationTracker.addObserver(floodObserver)
    }

    @Synchronized
    private fun runFloodJob() {
        if (floodJob?.isActive == true) {
            return
        }
        floodJob = GlobalScope.launch(floodThread) {
            floodJobBlock()
        }
    }


    private suspend fun floodJobBlock() {
        floodMessageDao.findFloodMessageDeferred().await()?.let { list ->
            try {
                list.forEach { message ->
                    if (message.category == "CALL")
                        callDecrypt.onRun(message)
                    else messageDecrypt.onRun(message)
                    floodMessageDao.delete(message)
                }
            } catch (e: Exception) {
                runFloodJob()
            } finally {
                floodJob = null
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


}