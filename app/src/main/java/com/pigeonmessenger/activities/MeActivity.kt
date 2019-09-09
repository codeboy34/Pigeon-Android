package com.pigeonmessenger.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.fragment.settings.MeFragment

class MeActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MeActivity"
        const val MAX_PHOTO_SIZE = 512
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_me)
        replaceFragment(MeFragment.getInstance(), R.id.container, MeFragment.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.findFragmentByTag(MeFragment.TAG)?.run {
            this.onActivityResult(requestCode, resultCode, data)
        }

    }

}
