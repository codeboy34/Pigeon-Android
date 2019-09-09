package com.pigeonmessenger.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.amin.SlideAnimationUtil
import com.pigeonmessenger.api.request.AccountRequest
import com.pigeonmessenger.crypto.*
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.vibrate
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.AccountViewModel
import com.pigeonmessenger.vo.toUser
import com.pigeonmessenger.widget.Keyboard
import com.pigeonmessenger.widget.VerificationCodeView
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fragment_mobile.*
import kotlinx.android.synthetic.main.layout_verificaion.*
import org.jetbrains.anko.textColor
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity(), VerificationCodeView.OnCodeEnteredListener {

    companion object {
        private const val TAG = "LoginActivity"
        fun show(context: Context) = context.startActivity(Intent(context, LoginActivity::class.java))
    }


    var keyboard: Keyboard? = null
    private val KEYS = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<<")
    private var visibleLayout: VisibleLayout = VisibleLayout.MOBILE

    var verificationId: String? = null
    var credential: PhoneAuthCredential? = null
    var autoSingIn = false
    var mResendToken: PhoneAuthProvider.ForceResendingToken? = null


    private val mKeyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            vibrate(longArrayOf(0, 30))
            val editable = mobile_et.text
            if (position == 11 && editable.isNotEmpty()) {
                mobile_et.text = editable.subSequence(0, editable.length - 1) as Editable?
            } else {
                mobile_et.text = editable.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            vibrate(longArrayOf(0, 30))
            val editable = mobile_et.text
            if (position == 11 && editable.isNotEmpty()) {
                mobile_et.text.clear()
            } else {
                mobile_et.text = editable.append(value)
            }
        }
    }

    private val accountViewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this).get(AccountViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContentView(R.layout.activity_login)

        mobile_et.showSoftInputOnFocus = false
        mobile_et.addTextChangedListener(mMobileEtTextWatcher)
        mobile_et.requestFocus()
        mobile_keyboard!!.setKeyboardKeys(KEYS)
        mobile_keyboard!!.setOnClickKeyboardListener(mKeyboardListener)
        mobile_keyboard.animate().translationY(0f).start()

        keyboard_verification.setKeyboardKeys(KEYS)
        keyboard_verification.setOnClickKeyboardListener(mVerificationListener)
        keyboard_verification.animate().translationY(0f).start()
        pin_verification_view.setOnCodeEnteredListener(this)

        mobile_fab.setOnClickListener {
            if (mobile_et.text.isNotEmpty()) {
                sendVerificationCode()
            }
        }
        otp_back_iv.setOnClickListener { onBackPressed() }
        back_iv.setOnClickListener { onBackPressed() }

        verification_left_bottom_tv.setOnClickListener {
            Toast.makeText(this, "Resending OTP", Toast.LENGTH_SHORT).show()
            resendVerificationCode()
            startCountDown()
        }

    }

    private fun slideToVerification() {
        cover.visibility = View.GONE
        mobile_progress.hide()
        layout_verification.visibility = View.VISIBLE

        SlideAnimationUtil.slideOutToLeft(this, layout_mobile)
        SlideAnimationUtil.slideInFromRight(this, layout_verification)

        layout_mobile.visibility = View.GONE
        visibleLayout = VisibleLayout.OTP
        pin_verification_title_tv.text = getString(R.string.landing_validation_title, mobile_et.text.toString())
        startCountDown()
    }


    @SuppressLint("RestrictedApi")
    private fun slideToMobile() {
        SlideAnimationUtil.slideOutToRight(this, layout_verification)
        layout_verification.visibility = View.GONE
        SlideAnimationUtil.slideInFromLeft(this, layout_mobile)
        layout_mobile.visibility = View.VISIBLE
        visibleLayout = VisibleLayout.MOBILE
        verification_fab.visibility = View.GONE
        pin_verification_view.clear()
        mobile_fab.visibility = View.VISIBLE
        verification_progress.visibility = View.GONE
        mobile_progress.hide()
    }


    private fun handleBackPress(): Boolean {
        if (visibleLayout == VisibleLayout.OTP) {
            mCountDownTimer!!.cancel()
            pin_verification_view.clear()
            slideToMobile()
            return true
        }
        return false
    }

    private var mCountDownTimer: CountDownTimer? = null

    private fun startCountDown() {
        mCountDownTimer?.cancel()
        mCountDownTimer = object : CountDownTimer(60000, 1000) {

            override fun onTick(l: Long) {
                if (verification_left_bottom_tv != null)
                    verification_left_bottom_tv.text = getString(R.string.landing_resend_code_disable, l / 1000)
            }

            override fun onFinish() {
                resetCountDown()
            }
        }
        mCountDownTimer?.start()
        verification_left_bottom_tv.isEnabled = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(R.color.colorGray).let { verification_left_bottom_tv.setTextColor(it) }
        }
    }


    private fun resetCountDown() {
        if (verification_left_bottom_tv != null) {
            verification_left_bottom_tv.setText(R.string.landing_resend_code_enable)
            verification_left_bottom_tv.isEnabled = true
            verification_left_bottom_tv.textColor = ContextCompat.getColor(this, R.color.pigeonActionColor)
        }
    }

    override fun onBackPressed() {
        if (!handleBackPress())
            super.onBackPressed()
    }


    private fun sendVerificationCode() {
        cover.visibility = View.VISIBLE
        var phoneNumber = mobile_et.text.toString()
        mobile_progress.visibility = View.VISIBLE
        mobile_progress.show()
        phoneNumber = "+91$phoneNumber"

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber, // Phone number to verify
                60, // Timeout duration
                TimeUnit.SECONDS, // Unit of timeout
                this, // Activity (for callback binding)
                mCallbacks)

    }


    @SuppressLint("RestrictedApi")
    override fun onCodeEntered(code: String) {
        if (code.length == 6) {
            verification_fab.visibility = View.VISIBLE
            verification_progress.visibility = View.VISIBLE
            verification_progress.show()
            verifyOTP()
        }
    }

    private fun verifyOTP() {
        if (!autoSingIn)
            credential = PhoneAuthProvider.getCredential(verificationId!!, pin_verification_view.code())
        autoSingIn = false
        signInWithCredential(credential!!)

    }

    private fun resendVerificationCode() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobile_et.text.toString(), // Phone number to verify
                60, // Timeout duration
                TimeUnit.SECONDS, // Unit of timeout
                this, // Activity (for callback binding)
                mCallbacks, // OnVerificationStateChangedCallbacks
                mResendToken)             // ForceResendingToken from callbacks
    }

    @SuppressLint("RestrictedApi")
    override fun onCodeDeleted() {
        verification_fab.visibility = View.GONE
        verification_progress.visibility = View.GONE
    }

    private val mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            this@LoginActivity.credential = credential
            autoSingIn = true
            if (credential.smsCode == null) {
                signInWithCredential(credential)
            } else {
                pin_verification_view.clear()
                for (code in credential.smsCode!!.toCharArray()) {
                    pin_verification_view.append(code.toString())
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e(TAG, "Verification Failed ", e)
            handleException(e)
        }

        override fun onCodeSent(verificationId: String?, token: PhoneAuthProvider.ForceResendingToken?) {
            this@LoginActivity.verificationId = verificationId
            mResendToken = token
            if (visibleLayout == VisibleLayout.MOBILE) slideToVerification()
        }
    }

    private fun handleException(e: Throwable) {
        if(verification_progress!=null) verification_progress.hide()
        if(mobile_progress!=null) mobile_progress.hide()
        if (e is FirebaseAuthInvalidCredentialsException)
            pin_verification_tip_tv.text = getString(R.string.enter_valid_otp)
        else ErrorHandler.handleError(e)
    }


    private fun hideProgress() {
        cover.visibility = View.GONE
        if (visibleLayout != VisibleLayout.MOBILE) verification_progress.hide()
        else mobile_progress.hide()
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        cover.visibility = View.VISIBLE
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                loginPigeon()
            }
        }.addOnFailureListener {
            Log.d(TAG, " ", it);
            hideProgress()
            ErrorHandler.handleError(it)
        }
    }


    @SuppressLint("CheckResult")
    private fun loginPigeon() {
        SignalProtocol.initSignal(this)
        val registrationId = CryptoPreference.getLocalRegistrationId(this)

        val phoneNumber = mobile_et.text.toString()
        val accountReq = AccountRequest(phoneNumber, registrationId)
        accountViewModel.verify(accountReq).subscribe({
            if (it.isSuccessful) {
                val account = it.body()
                if (account?.userId != null && account.full_name != null) {
                    Session.storeAccount(account)
                    accountViewModel.insertMe(account.toUser())
                } else {
                    accountViewModel.insertMe(User(null, mobile_et.text.toString(), relationship = Relationship.ME.name))
                }
                Session.setPhoneNumber(mobile_et.text.toString())
                hideProgress()
                startNextActivity()
            } else {
                Log.d(TAG, "Error code: ${it.code()}");
                ErrorHandler.handleCode(it.code())
                hideProgress()
            }
        }, {
            Log.d(TAG, "verifyErrror ", it);
            hideProgress()
            handleException(it)
        })
    }

    private
    val mMobileEtTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            handleEditView(s.toString())
        }
    }


    @SuppressLint("RestrictedApi")
    private fun handleEditView(toString: String) {
        if (!toString.isNotEmpty() || toString.length != 10) {
            mobile_fab.visibility = View.GONE
        } else mobile_fab.visibility = View.VISIBLE
    }

    private val mVerificationListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin_verification_view?.delete()
            } else {
                pin_verification_view?.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin_verification_view?.clear()
            } else {
                pin_verification_view?.append(value)
            }
        }
    }

    enum class VisibleLayout {
        OTP, MOBILE
    }

    private fun startNextActivity() {
        val intent = Intent(this,SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}
