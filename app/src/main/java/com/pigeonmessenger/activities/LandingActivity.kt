package com.pigeonmessenger.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.setLinkSpan
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_landing.*

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        terms_tv.apply {
            movementMethod = LinkMovementMethod.getInstance()

            val msgString = getString(R.string.accept_terms_and_condition)
            val privacyPolicy = getString(R.string.privacy_policy)
            val termsOfService = getString(R.string.terms_of_service)
            val termsUrl = getString(R.string.terms_url)
            val privacyUrl = getString(R.string.privacy_url)

            val message = SpannableString(String.format(msgString,privacyPolicy,termsOfService)).apply {
                setLinkSpan(privacyPolicy, privacyUrl)
                setLinkSpan(termsOfService, termsUrl)
            }
            text = message
        }

        get_started.setOnClickListener {
            RxPermissions(this)
                    .request(Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS ,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA)
                    .subscribe {
                        startActivity(Intent(this,LoginActivity::class.java))
                    }
        }
    }

    companion object {
        fun show(context:Context){
            context.startActivity(Intent(context,LandingActivity::class.java))
        }
    }
}

