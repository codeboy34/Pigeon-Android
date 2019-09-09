package com.pigeonmessenger.utils

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.events.ProgressEvent
import com.pigeonmessenger.utils.video.MixinPlayer
import com.pigeonmessenger.vo.MessageItem
import com.pigeonmessenger.widget.CircleProgress.Companion.STATUS_ERROR
import com.pigeonmessenger.widget.CircleProgress.Companion.STATUS_PAUSE
import com.pigeonmessenger.widget.CircleProgress.Companion.STATUS_PLAY
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

import java.util.concurrent.TimeUnit

class AudioPlayer private constructor() {
    companion object {
        @Synchronized
        fun get(): AudioPlayer {
            if (instance == null) {
                instance = AudioPlayer()
            }
            return instance as AudioPlayer
        }

        private var instance: AudioPlayer? = null

        fun release() {
            instance?.let {
                it.player.release()
                it.stopTimber()
            }
            instance = null
        }

        fun pause() {
            instance?.pause()
        }
    }

    private val player: MixinPlayer = MixinPlayer(true).also {
        it.setCycle(false)
        it.setOnVideoPlayerListener(object : MixinPlayer.VideoPlayerListenerWrapper() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
                    stopTimber()
                    status = STATUS_ERROR
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                status = STATUS_PAUSE
                stopTimber()
                it.stop()
                RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
            }
        })
    }

    private var id: String? = null
    private var url: String? = null
    private var status = STATUS_PAUSE

    fun play(messageItem: MessageItem) {
        if (id != messageItem.id) {
            id = messageItem.id
            url = messageItem.mediaUrl
            url?.let {
                player.loadAudio(it)
            }
        } else if (status == STATUS_ERROR) {
            player.loadAudio(url!!)
        }
        status = STATUS_PLAY
        player.start()
        if (id != null) {
            RxBus.publish(ProgressEvent(id!!, -1f, STATUS_PLAY))
        }
        startTimer()
    }

    fun pause() {
        status = STATUS_PAUSE
        player.pause()
        if (id != null) {
            RxBus.publish(ProgressEvent(id!!, -1f, STATUS_PAUSE))
        }
        stopTimber()
    }

    fun isPlay(id: String): Boolean {
        return status == STATUS_PLAY && this.id == id
    }

    fun isLoaded(id: String): Boolean {
        return this.id == id && status != STATUS_ERROR
    }

    var timerDisposable: Disposable? = null
    var progress = 0f
    private fun startTimer() {
        if (timerDisposable == null) {

            timerDisposable =   io.reactivex.Observable.interval(0, 100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
                progress = player.getCurrentPos().toFloat() / player.duration()
                RxBus.publish(ProgressEvent(id!!, progress))
            }
        }
    }

    private fun stopTimber() {

        timerDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        timerDisposable = null
    }
}