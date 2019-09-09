package com.pigeonmessenger.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.extension.putLong
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.job.RefreshOneTimePreKeysJob
import com.pigeonmessenger.job.RotateSignedPreKeyJob
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.utils.Constant.Account.PREF_RESTORE
import com.pigeonmessenger.widget.NoUnderLineSpan
import kotlinx.android.synthetic.main.activity_splash.*
import javax.inject.Inject

class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var jobManager: PigeonJobManager

    companion object {
        private const val TAG = "SplashActivity"

        fun getSingleIntent(context: Context): Intent {
            return Intent(context, SplashActivity::class.java).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }

        fun show(context: Context) {
            context.startActivity(Intent(context, SplashActivity::class.java))
        }
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        App.get().appComponent.inject(this)
        if (FirebaseAuth.getInstance().currentUser == null) {
            initUI()
            return
        //    LoginActivity.show(this)
          //  finish()
        }

        val mPhoneNumber = Session.registeredPhoneNumber()
        if (mPhoneNumber == null) {
            initUI()
            return
        }

        val account = Session.getAccount()
        if (account?.full_name == null) {
            SetupAccountActivity.show(this)
            finish()
            return
        }

        if (!defaultSharedPreferences.getBoolean(InitializeActivity.IS_LOADED, false)) {
            InitializeActivity.show(this)
            finish()
            return
        }

        if (defaultSharedPreferences.getBoolean(PREF_RESTORE, true)) {
            RestoreActivity.show(this)
            finish()
            return
        }


        if (defaultSharedPreferences.getBoolean(Constant.Account.PREF_WRONG_TIME, false)) {
            //TimeFragment.newInstance()
            // finish()
            return
        }

        jobManager.addJobInBackground(RefreshOneTimePreKeysJob())
        rotateSignalPreKey()
        startHomeActivity()
    }

    private fun initUI() {
        setContentView(R.layout.activity_splash)
        val policy: String = getString(R.string.landing_privacy_policy)
        val termsService: String = getString(R.string.landing_terms_service)
        val policyWrapper = getString(R.string.landing_introduction, policy, termsService)
        val colorPrimary = ContextCompat.getColor(this, R.color.pigeonActionColor)
        val policyUrl = getString(R.string.privacy_url)
        val termsUrl = getString(R.string.terms_url)
        introduction_tv.text = highlightLinkText(
                policyWrapper,
                colorPrimary,
                arrayOf(policy, termsService),
                arrayOf(policyUrl, termsUrl))
        introduction_tv.movementMethod = LinkMovementMethod.getInstance()

        agree_tv.setOnClickListener {
            LoginActivity.show(this)
        }
    }

    private fun highlightLinkText(
            source: String,
            color: Int,
            texts: Array<String>,
            links: Array<String>
    ): SpannableString {
        if (texts.size != links.size) {
            throw IllegalArgumentException("texts's length should equals with links")
        }
        val sp = SpannableString(source)
        for (i in texts.indices) {
            val text = texts[i]
            val link = links[i]
            val start = source.indexOf(text)
            sp.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(link) }.also { startActivity(it) }
                        }
                    },
                    start,
                    start + text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sp.setSpan(NoUnderLineSpan(link), start, start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(ForegroundColorSpan(color), start, start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sp
    }

    private fun rotateSignalPreKey() {
        val cur = System.currentTimeMillis()
        val last = defaultSharedPreferences.getLong(RotateSignedPreKeyJob.ROTATE_SIGNED_PRE_KEY, 0)
        if (last == 0.toLong()) {
            defaultSharedPreferences.putLong(RotateSignedPreKeyJob.ROTATE_SIGNED_PRE_KEY, cur)
        }
        if (cur - last > Constant.INTERVAL_48_HOURS) {
            jobManager.addJobInBackground(RotateSignedPreKeyJob())
            defaultSharedPreferences.putLong(RotateSignedPreKeyJob.ROTATE_SIGNED_PRE_KEY, cur)
        }
    }

    private fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }


}
