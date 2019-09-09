package com.pigeonmessenger.manager


import android.annotation.SuppressLint
import android.util.Log
import com.birbit.android.jobqueue.JobManager
import com.github.nkzawa.socketio.client.Ack
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.database.room.daos.FloodMessageDao
import com.pigeonmessenger.database.room.daos.MessageDao
import com.pigeonmessenger.events.SeenStatusEvent
import com.pigeonmessenger.exception.SocketNotConnectedException
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.job.LinkState
import com.pigeonmessenger.job.RefreshUserJob
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.utils.typingExecutor
import com.pigeonmessenger.viewmodals.isGroup
import com.pigeonmessenger.vo.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.runOnUiThread
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SocketManager constructor(
        private var floodMessageDao: FloodMessageDao,
        private var messageDao: MessageDao,
        private var linkState: LinkState,
        private var jobManager: JobManager,
        private var typingManager: TypingManager) {

    private val TAG = "SocketManager"

    private var socket: Socket? = null

    private var isConnected = false
    private var isConnecting = false
    private var gson = Gson()
    var socketObserver: SocketObserver? = null


    @SuppressLint("LogNotTimber")
    fun connect(token: String, phone_number: String) {
        Log.d(TAG, "connect()")
        if (isConnecting || isConnected)
            return

        isConnecting = true

        Log.d(TAG, "connecting...: ")

        val options = IO.Options()

        options.query = "token=$token&user_id=$phone_number"
        //options.transports

        // options.timeout
        options.reconnection = false
        socket = IO.socket(Constant.URL, options)
        socket!!.connect()

        initEvents()
    }


    @SuppressLint("LogNotTimber")
    private fun initEvents() {
        socket!!.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "[++++] Socket Connected [ # ]")
            isConnected = true
            isConnecting = false
            App.get().runOnUiThread {
                linkState.state = LinkState.ONLINE
                jobManager.start()
                socketObserver?.onOpen()
            }
        }

        socket!!.on(Socket.EVENT_ERROR) {
            val error = it[0] as String
            Log.e(TAG, "SOCKET ERROR :  $error")
            isConnecting = false
            isConnected = false
            socket = null
            socketObserver?.onClose()
        }

        socket!!.on(Socket.EVENT_CONNECT_TIMEOUT) {
            Log.d(TAG, "--------------------------------------\n ")
            Log.e(TAG, "SOCKET CONNECTION TIMEOUT")
            Log.d(TAG, "--------------------------------------\n ")
            isConnecting = false
            isConnected = false
            socketObserver?.onClose()
        }

        socket!!.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "--------------------------------------\n\n ")
            Log.d(TAG, "SOCKET DISCONNECTED")
            Log.d(TAG, "--------------------------------------\n\n ")

            App.get().runOnUiThread {
                linkState.state = LinkState.OFFLINE
            }
            isConnected = false
            socket = null
            socketObserver?.onClose()
        }
        socket!!.on(SocketEvents.MESSAGE) {
            val msg: String = it[0] as String
            Log.d(TAG, "[*] Message received : $msg")
            val blazeMessage = gson.fromJson(msg, BlazeMessage::class.java)
            when (blazeMessage.category) {
                BlazeMessageCategory.CHAT_MESSAGE.name -> {
                    val conversationId = blazeMessage.conversationId
                    val blazeMessageParamString = gson.toJson(blazeMessage.messageParam)
                    val floodMessage = FloodMessage(blazeMessage.id, conversationId, blazeMessage.senderId,
                            blazeMessageParamString, blazeMessage.messageParam!!.category, blazeMessage.createdAt)
                    floodMessageDao.insert(floodMessage)
                    val ack = it[1] as Ack
                    typingExecutor.execute {
                        typingManager.onNewChatMessage(conversationId, blazeMessage.senderId)
                    }
                    if (!isGroup(conversationId) && blazeMessage.messageParam.isChatMessage()) {
                        val blazeAckMessage = createAckMessage(blazeMessage.id, blazeMessage.conversationId, Session.getUserId(),
                                blazeMessage.senderId,
                                AckMessage(blazeMessage.messageParam!!.message_id, MessageStatus.DELIVERED.name), nowInUtc())
                        ack.call(gson.toJson(blazeAckMessage))
                    } else {
                        ack.call()
                    }
                }
                BlazeMessageCategory.ACK.name -> {
                    val ackParams: AckMessage = blazeMessage.ack!!
                    messageDao.updateAck(ackParams.messageId, ackParams.status)
                    (it[1] as Ack).call()
                }
                BlazeMessageCategory.ACKS.name -> {
                    val ackList = blazeMessage.acks
                    ackList?.forEach {
                        messageDao.updateAck(it.messageId, it.status)
                    }
                    (it[1] as Ack).call()
                }
                BlazeMessageCategory.CALL.name -> {
                    val blazeMessageParamString = gson.toJson(blazeMessage.messageParam)
                    val floodMessage = FloodMessage(blazeMessage.id, blazeMessage.conversationId, blazeMessage.senderId,
                            blazeMessageParamString, blazeMessage.category, blazeMessage.createdAt)
                    floodMessageDao.insert(floodMessage)
                    val ack = it[1] as Ack
                    ack.call()
                }
                BlazeMessageCategory.SYSTEM.name -> {
                    val conversationId = blazeMessage.conversationId
                    val systemDataString = gson.toJson(blazeMessage.systemData)
                    val floodMessage = FloodMessage(blazeMessage.id, conversationId, blazeMessage.senderId,
                            systemDataString, MessageCategory.SYSTEM_CONVERSATION.name, blazeMessage.createdAt)
                    floodMessageDao.insert(floodMessage)
                    val ack = it[1] as Ack
                    ack.call()
                }

                BlazeMessageCategory.TYPING.name -> {
                    typingExecutor.execute {
                        typingManager.onTypingEvent(blazeMessage.conversationId, blazeMessage.senderId)
                    }
                }

                BlazeMessageCategory.EVENT.name->{
                    jobManager.addJobInBackground(RefreshUserJob(blazeMessage.senderId))
                }
                else -> {
                    if (it[1] != null) {
                        val ack = it[1] as Ack
                        ack.call()
                    }
                }
            }
        }

        socket!!.on("seenStatus") {
            Log.d(TAG, "seenStatus : ");
            val seenStatus = it[0] as JSONObject
            Log.d(TAG, "${seenStatus}: ");
            val seenEvent = Gson().fromJson(seenStatus.toString(), SeenStatusEvent::class.java)
            EventBus.getDefault().post(seenEvent)
        }
    }


    @SuppressLint("LogNotTimber")
    @Throws(SocketNotConnectedException::class)
    fun sendMessage(blazeMessage: BlazeMessage) {
        if (isConnected) {
            val latch = CountDownLatch(1)
            val jsonString = gson.toJson(blazeMessage)
            Log.d(TAG, "sendingMessage $jsonString")
            socket?.emit(SocketEvents.MESSAGE, JSONObject(jsonString), Ack {
                latch.countDown()
            })
            latch.await(25, TimeUnit.SECONDS)
        } else {
            throw SocketNotConnectedException()
        }
    }

    fun changeOnlineStatus(online: Boolean) {
        Log.d(TAG, "changeOnlineStatus: ")
        if (isConnected) {
            if (online) socket?.emit("goOnline") else socket?.emit("goOffline")
        }
    }

    fun registerOnlineStatus(userId: String) {
        if (isConnected) {
            socket?.emit("seenStatus", JSONObject().put("user_id", userId))
            //socket?.on("active-$userId") {

            //}
        }
    }

    fun leaveOnlieStatus(userId: String) {
        if (isConnected) socket?.emit("exitStatus", JSONObject().put("user_id", userId))
    }

    interface SocketObserver {
        fun onOpen()
        fun onClose()
    }

}
