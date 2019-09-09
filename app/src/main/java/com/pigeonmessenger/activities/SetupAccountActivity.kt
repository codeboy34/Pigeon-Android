package com.pigeonmessenger.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.api.request.AccountUpdateRequest
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.AccountViewModel
import com.pigeonmessenger.vo.Account
import com.pigeonmessenger.vo.toUser
import com.pigeonmessenger.widget.BottomSheet
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_setup_account.*
import kotlinx.android.synthetic.main.view_setupname_bottom.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

class SetupAccountActivity : AppCompatActivity() {

    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }


    companion object {
        const val TAG = "SetupAccountActivity"
        const val MAX_PHOTO_SIZE = 512

        fun show(context: Context) {
            context.startActivity(Intent(context, SetupAccountActivity::class.java))
        }
    }

    private val accountViewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this).get(AccountViewModel::class.java)
    }

    private val imageUri: Uri by lazy {
        Uri.fromFile(getImagePath().createImageTemp())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_account)

        name_et.requestFocus()
        val dummyIv = findViewById<ImageView>(R.id.dummy_iv)

        dummyIv.setOnClickListener {
            showBottom()
        }
        circular_iv.setOnClickListener { showBottom() }

        name_et.addTextChangedListener(TextWatchListener())

        name_fab.setOnClickListener {
            name_progress.show()
            cover.visibility = View.VISIBLE
            update()
        }
    }

    @SuppressLint("CheckResult", "LogNotTimber")
    fun update() = doAsync {
        var thumbnail: String? = null
        var avatarBase64: String? = null
        resultUri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            val imageFile = it.toFile()
            val size = Size(MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
            thumbnail = imageFile.blurThumbnail(size)?.bitmap2String()
            avatarBase64 = Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP)
        }
        this.uiThread {
            val fullName = name_et.text.toString()
            if (fullName.isNullOrBlank()) {
                name_fab.visibility = View.GONE
                toast("Please enter a valid name")
                return@uiThread
            }
            accountViewModel.updateAccount(AccountUpdateRequest(fullName, avatar = avatarBase64, thumbnailBase64 = thumbnail))
                    .autoDisposable(scopeProvider)
                    .subscribe({
                        hideProgress()
                        if (it.isSuccessful) {
                            name_fab.visibility = View.INVISIBLE
                            val account = Account(Session.registeredPhoneNumber())
                            account.full_name = fullName
                            account.thumbnail = thumbnail
                            account.avatar = avatarBase64
                            Session.storeAccount(account)
                            accountViewModel.insertMe(account.toUser())
                            SplashActivity.show(this@SetupAccountActivity)
                            finish()
                        } else {
                            ErrorHandler.handleCode(it.code())
                        }
                    }, {
                        hideProgress()
                        ErrorHandler.handleError(it)
                        Log.d(TAG, "Throwable", it)
                    })
        }
    }

    private fun hideProgress() {
        cover.visibility = View.GONE
        name_progress.hide()
    }

    private var resultUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK
                && (requestCode == REQUEST_CAMERA || requestCode == REQUEST_IMAGE)) {
            var selectedImageUri: Uri?
            if (data == null || data.action != null && data.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                selectedImageUri = imageUri
            } else {
                selectedImageUri = data.data
                if (selectedImageUri == null) {
                    selectedImageUri = imageUri
                }
            }
            val options = UCrop.Options()
            options.setToolbarColor(ContextCompat.getColor(this, R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(this, R.color.black))
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri, imageUri)
                    .withOptions(options)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
                    .start(this)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                resultUri = UCrop.getOutput(data)
                showAvatar()
                // update(Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP), true)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val cropError = UCrop.getError(data)
                toast(cropError.toString())
            }
        }
    }


    private fun showAvatar() {
        if (resultUri != null) {
            dummy_iv.visibility = View.GONE
            circular_iv_wp.visibility = View.VISIBLE
            circular_iv.loadImage(resultUri)
        } else {
            circular_iv_wp.visibility = View.GONE
            dummy_iv.visibility = View.VISIBLE
        }
    }

    private fun showBottom() {

        hideKeyboard()
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(ContextThemeWrapper(this, R.style.Custom), R.layout.view_setupname_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()

        if (resultUri == null)
            view.remove.visibility = View.GONE
        view.remove.setOnClickListener {
            resultUri = null
            showAvatar()
            bottomSheet.dismiss()
        }
        view.gallery.setOnClickListener {
            RxPermissions(this@SetupAccountActivity)
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (granted) {
                            openImage()
                        } else {
                            openPermissionSetting()
                        }
                    }
            bottomSheet.dismiss()
        }
        view.camera.setOnClickListener {
            RxPermissions(this@SetupAccountActivity)
                    .request(Manifest.permission.CAMERA)
                    .subscribe({ granted ->
                        if (granted) {
                            Log.d(TAG, "Permission granted: ");
                            imageUri.let {
                                Log.d(TAG, "imgUri not null ");
                                openCamera(it)
                            }
                        } else {
                            Log.d(TAG, "Permission not granted: ");
                            openPermissionSetting()
                        }
                    }, {
                        Log.e(TAG, "err ", it);
                    })
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()

    }

    inner class TextWatchListener : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.isNullOrEmpty()) {
                name_fab.visibility = View.GONE
            } else {
                name_fab.visibility = View.VISIBLE
            }
        }

    }
}
