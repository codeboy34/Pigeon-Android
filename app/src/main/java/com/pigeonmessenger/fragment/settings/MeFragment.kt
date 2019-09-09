package com.pigeonmessenger.fragment.settings


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.AccountUpdateActivity
import com.pigeonmessenger.activities.AvatarPreviewActivity
import com.pigeonmessenger.activities.MeActivity
import com.pigeonmessenger.activities.SetupAccountActivity
import com.pigeonmessenger.api.request.AccountUpdateRequest
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.AccountViewModel
import com.pigeonmessenger.widget.BottomSheet
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.card_title_view.view.*
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.android.synthetic.main.view_update_avatar_bottom.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

class MeFragment : Fragment() {

    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    private var resultUri: Uri? = null

    companion object {
        const val TAG = "MeFragment"
        const val MAX_PHOTO_SIZE = 512

        fun getInstance(): MeFragment {
            return MeFragment()
        }
    }

    private val avatarFile: File by lazy {
        requireContext().avatarFile(Session.getUserId())
    }

    private var avatarTempUri: Uri? = null

    private val accountViewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this).get(AccountViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_me, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        titleView.back_iv.setOnClickListener { activity?.onBackPressed() }
        renderMe()
        name_tv.setOnClickListener {
            AccountUpdateActivity.show(requireContext(), AccountUpdateActivity.PURPOSE_NAME)
        }
        bio_tv.setOnClickListener {
            AccountUpdateActivity.show(requireContext(), AccountUpdateActivity.PURPOSE_BIO)
        }

        add_bio.setOnClickListener {
            AccountUpdateActivity.show(requireContext(), AccountUpdateActivity.PURPOSE_BIO)
        }
        avatar_iv.setOnClickListener {
            showBottom()
        }

        account.setOnClickListener {
            activity?.addFragment(this, AccountSettingFragment.getInstance(), AccountSettingFragment.TAG)
        }
        chat.setOnClickListener {
            activity?.addFragment(this, ChatSettingFragment.getInstance(), ChatSettingFragment.TAG)
        }

        notification.setOnClickListener {
            activity?.addFragment(this, NotificationSettingFragment.getInstance(), NotificationSettingFragment.TAG)
        }


        help.setOnClickListener { }
        about.setOnClickListener { }

    }


    private fun renderMe() {
        mob_tv.text = Session.getPhoneNumberWithCountryCode()
        Session.liveAccount.observe(this, Observer {

            Log.d(MeActivity.TAG, "$it ");
            name_tv.text = it.full_name
            it.avatar?.let { avatar ->
                avatar_iv.loadOwnerAvatar(avatar, it.thumbnail)
            }
            notNullElse(it.bio, {
                bio_tv.visibility = VISIBLE
                add_bio.visibility = GONE
                bio_tv.text = it
            }, {
                bio_tv.visibility = GONE
                add_bio.visibility = VISIBLE
            })
        })
    }

    private fun showBottom() {
        requireActivity().hideKeyboard()
        val builder = BottomSheet.Builder(requireContext())
        val view = View.inflate(ContextThemeWrapper(requireContext(), R.style.Custom), R.layout.view_update_avatar_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()

        val account = Session.getAccount()
        if (account?.avatar == null || account.thumbnail == null) {
            view.remove.visibility = View.GONE
        }

        view.remove.setOnClickListener {
           removeAvatar()
            bottomSheet.dismiss()
        }
        view.edit.setOnClickListener {
            RxPermissions(requireActivity())
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (granted) {
                            avatarTempUri = Uri.fromFile(createTempFile(suffix = ".jpg"))
                            requireActivity().openImage(avatarTempUri!!)
                            //requireActivity().openImage(avatarTempUri!!)
                        } else {
                            requireContext().openPermissionSetting()
                        }
                    }
            bottomSheet.dismiss()
        }
        view.view.setOnClickListener {
            AvatarPreviewActivity.show(requireContext(), Session.getUserId())
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun hideProgress() {
        avatar_iv.isClickable = true
        avatar_pg.stopSpinning()
        avatar_pg.visibility = View.GONE
    }

    private fun showProgress() {
        avatar_pg.spin()
        avatar_iv.isClickable = false
        avatar_pg.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        Log.d(TAG, "OnActivity Result")
        if (resultCode == Activity.RESULT_OK
                && (requestCode == REQUEST_CAMERA || requestCode == REQUEST_IMAGE)) {

            var selectedImageUri: Uri?
            if (data == null || data.action != null && data.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                selectedImageUri = avatarTempUri
            } else {
                selectedImageUri = data.data
                if (selectedImageUri == null) {
                    selectedImageUri = avatarTempUri
                }
            }
            val options = UCrop.Options()
            options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri!!, avatarTempUri!!)
                    .withOptions(options)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(SetupAccountActivity.MAX_PHOTO_SIZE, SetupAccountActivity.MAX_PHOTO_SIZE)
                    .start(requireActivity())
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                resultUri = UCrop.getOutput(data)
                showProgress()
                update()
                // update(Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP), true)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val cropError = UCrop.getError(data)
                toast(cropError.toString())
            }
        }
    }

    fun removeAvatar() {
        showProgress()
        accountViewModel.removeAvatar()
                .autoDisposable(scopeProvider)
                .subscribe({
                    hideProgress()
                    if (it.isSuccessful) {
                        requireContext().avatarFile(Session.getUserId()).delete()
                        val account = Session.getAccount()!!
                        account.thumbnail = null
                        account.avatar = null
                        Session.storeAccount(account)
                        toast("Profile removed.")
                    } else {
                        ErrorHandler.handleCode(it.code())
                    }
                }, {
                    ErrorHandler.handleError(it)
                    hideProgress()
                    Log.d(MeActivity.TAG, "Throwable", it)
                })
    }

    @SuppressLint("CheckResult", "LogNotTimber")
    fun update() = doAsync {
        var thumbnail: String? = null
        var avatarBase64: String? = null
        resultUri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            val imageFile = it.toFile()
            val size = Size(MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
            thumbnail = imageFile.blurThumbnail(size)?.bitmap2String()
            avatarBase64 = Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP)
        }
        this.uiThread {
            accountViewModel.updateAccount(AccountUpdateRequest(avatar = avatarBase64, thumbnailBase64 = thumbnail))
                    .autoDisposable(scopeProvider)
                    .subscribe({
                        hideProgress()
                        if (it.isSuccessful) {
                            hideProgress()
                            toast("Profile updated")
                            val account = Session.getAccount()!!
                            account.thumbnail = thumbnail
                            account.avatar = avatarBase64
                            Session.storeAccount(account)
                            account.avatar?.toBitmap()?.save(avatarFile)
                        } else {
                            ErrorHandler.handleCode(it.code())
                        }
                    }, {
                        hideProgress()
                        ErrorHandler.handleError(it)
                        Log.d(MeActivity.TAG, "Throwable", it)
                    })
        }
    }


}
