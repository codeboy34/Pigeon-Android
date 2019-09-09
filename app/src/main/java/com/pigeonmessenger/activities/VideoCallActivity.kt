package com.pigeonmessenger.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pigeonmessenger.R

class VideoCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        ///PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.Builder().createInitializationOptions())
    }
}
