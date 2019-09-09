package com.pigeonmessenger.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Size
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.api.request.ConversationRequest
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.database.room.entities.ConversationStatus
import com.pigeonmessenger.database.room.entities.isMute
import com.pigeonmessenger.events.ConversationEvent
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.job.ConversationJob
import com.pigeonmessenger.job.GroupIconDownloadJob
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.viewmodals.GroupViewModel
import com.pigeonmessenger.widget.BottomSheet
import com.pigeonmessenger.widget.PbDialog
import com.pigeonmessenger.widget.sweetalert.SweetAlertDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import com.yalantis.ucrop.UCrop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.card_title_view.view.*
import kotlinx.android.synthetic.main.item_mute_time.view.*
import kotlinx.android.synthetic.main.item_setting.view.*
import kotlinx.android.synthetic.main.mute_bottom_layout.view.*
import kotlinx.android.synthetic.main.update_icon_bottom.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File
import javax.inject.Inject

class GroupInfo : AppCompatActivity() {

    @Inject
    lateinit var jobManager: PigeonJobManager

    val conversationId: String by lazy {
        intent.getStringExtra(CONVERSATON_ID)
    }

    private var disposable: Disposable? = null

    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProviders.of(this).get(GroupViewModel::class.java)
    }

    private var conversation: Conversation? = null

    private var resultUri: Uri? = null

    private val iconFile: File by lazy {
        this.getGroupAvatarPath(conversationId)
    }

    private var tempUri: Uri? = null //TODO think over it ..It is creating temp file

    private var isMute: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)
        titleView.back_iv.setOnClickListener { onBackPressed() }

        App.get().appComponent.inject(this)
        groupViewModel.getConversationById(conversationId).observe(this, Observer {
            conversation = it
            name_tv.text = it.name
            avatar_iv.loadAvatar(avatarFile(conversationId),it.groupIconThumbnail,R.drawable.ic_groupme)
            val iconUrl = getGroupAvatarPath(conversationId)

            if (!iconUrl.exists() || iconUrl.length() <= 0) {
                jobManager.addJobInBackground(GroupIconDownloadJob(conversationId))
            }

            if (it.status == ConversationStatus.QUIT.ordinal) {
                mute.visibility = GONE
                leave_group.visibility = GONE
                not_in.visibility = VISIBLE
            }

            mute.title.text = if (it.isMute()) {
                isMute = true
                getString(R.string.unmute)

            } else {
                isMute = false
                getString(R.string.sub_mute)
            }

            doAsync {
                val user = groupViewModel.findUser(it.ownerId!!)
                this.uiThread { ui ->
                    user?.let { user ->
                        group_desc.text = String.format(getString(R.string.created_by),
                                user.getName(), it.createdAt.timeAgoDate(this@GroupInfo))
                    }
                }
            }
        })

        edit_iv.setOnClickListener {
            AccountUpdateActivity.show(this, AccountUpdateActivity.PURPOSE_GROUP_NAME, conversationId, conversation!!.name)
        }
        group_members.setOnClickListener {
            GroupActivity.show(this@GroupInfo, GroupActivity.INFO, conversationId)
        }

        shared_media.setOnClickListener {
            SharedMediaActivity.show(this, conversationId)
        }
        leave_group.setOnClickListener {
            SweetAlertDialog(this)
                    .setTitleText(getString(R.string.leave_dialog_title))
                    .setContentText(getString(R.string.group_info_leave_tip))
                    .setConfirmText(getString(R.string.confirm).toUpperCase())
                    .setCancelText(getString(R.string.cancel).toUpperCase())
                    .setCancelClickListener { it.dismissWithAnimation() }
                    .setConfirmClickListener {
                        showPb(R.string.leaving)
                        groupViewModel.exitGroup(conversationId)
                        it.dismissWithAnimation()
                    }
                    .show()
        }

        clear_chat.setOnClickListener {
            showClear()
        }

        avatar_iv.setOnClickListener {
            showBottom()
        }

        mute.setOnClickListener {
            if (isMute) groupViewModel.mute(conversationId, null)
            else showMuteBottom()
        }

        if (disposable == null) {
            disposable = RxBus.listen(ConversationEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it.type == ConversationJob.TYPE_EXIT)
                            dialog?.dismiss()
                        else {
                            if (it.isSuccess && resultUri != null) {
                            }
                            avatar_iv.visibility = VISIBLE
                            icon_pv.visibility = GONE
                        }
                    }
        }
    }

    private fun showBottom() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(ContextThemeWrapper(this, R.style.Custom), R.layout.update_icon_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()

        if (!iconFile.exists() && iconFile.length() <= 0) {
            view.view.visibility = View.GONE
        }

        view.edit.setOnClickListener {
            RxPermissions(this)
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (granted) {
                            tempUri = Uri.fromFile(createTempFile(suffix = ".jpg"))
                            openImage(tempUri!!)
                        } else {
                            openPermissionSetting()
                        }
                    }
            bottomSheet.dismiss()
        }
        view.view.setOnClickListener {
            AvatarPreviewActivity.show(this, conversationId = conversationId)
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }


    private var dialog: PbDialog? = null
    private fun showPb(message: Int?) {
        if (dialog == null) {
            dialog = progressDialog()
        }
        dialog?.addMessage(this, message)
        dialog?.show(supportFragmentManager, "fuck")
    }

    private fun showClear() {
        SweetAlertDialog(this)
                .setTitleText(getString(R.string.dialog_clear_chat_title))
                .setConfirmText(getString(R.string.clear).toUpperCase())
                .setCancelText(getString(R.string.cancel).toUpperCase())
                .setContentText(getString(R.string.sure_clear_chat))
                .setCancelClickListener { it.dismissWithAnimation() }
                .setConfirmClickListener {
                    groupViewModel.clearConversationById(conversationId)
                    toast(R.string.cleared)
                    it.dismissWithAnimation()
                }
                .show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE) {
            var selectedImageUri: Uri?
            if (data == null || data.action != null &&
                    data.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE) {
                selectedImageUri = tempUri
            } else {
                selectedImageUri = data.data
                if (selectedImageUri == null) {
                    selectedImageUri = tempUri
                }
            }
            val options = UCrop.Options()
            options.setToolbarColor(ContextCompat.getColor(this, R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(this, R.color.black))
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri!!, tempUri!!)
                    .withOptions(options)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(MeActivity.MAX_PHOTO_SIZE, MeActivity.MAX_PHOTO_SIZE)
                    .start(this)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                resultUri = UCrop.getOutput(data)
                // photo_rl.setGroup(resultUri)
                updateIcon()
                //new_group_avatar.loadCircleImage(resultUri.toString(), R.drawable.ic_photo_camera)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                toast("crop failed")
            }
        }
    }

    private fun updateIcon() {
        if (resultUri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, resultUri)
            val imageFile = resultUri!!.toFile()
            val size = Size(SetupAccountActivity.MAX_PHOTO_SIZE, SetupAccountActivity.MAX_PHOTO_SIZE)
            val thumbnail = imageFile.blurThumbnail(size)?.bitmap2String()
            val groupIcon = Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP)
            icon_pv.visibility = VISIBLE
            icon_pv.spin()
            groupViewModel.updateGroup(conversationId, ConversationRequest(conversationId, iconBase64 = groupIcon, thumbnail = thumbnail))
        }
    }


    private fun showMuteBottom() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(ContextThemeWrapper(this, R.style.Custom), R.layout.mute_bottom_layout, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()

        view.eight_hours.time_tv.text = getString(R.string.eight_hours)
        view.one_week.time_tv.text = getString(R.string.one_weeks)
        view.one_year.time_tv.text = getString(R.string.one_years)
        view.forever.time_tv.text = getString(R.string.forevers)

        makeActive(view, R.id.eight_hours)
        view.eight_hours.setOnClickListener { makeActive(view, R.id.eight_hours) }
        view.one_week.setOnClickListener { makeActive(view, R.id.one_week) }
        view.one_year.setOnClickListener { makeActive(view, R.id.one_year) }
        view.forever.setOnClickListener { makeActive(view, R.id.forever) }

        view.mute_button.setOnClickListener {
            when {
                view.eight_hours.isActive() -> groupViewModel.mute(conversationId, Constant.MUTE_8_HOURS.muteUntil())
                view.one_week.isActive() -> groupViewModel.mute(conversationId, Constant.MUTE_1_WEEK.muteUntil())
                view.one_year.isActive() -> groupViewModel.mute(conversationId, Constant.MUTE_1_YEAR.muteUntil())
                view.forever.isActive() -> groupViewModel.mute(conversationId, Constant.MUTE_FOREVER.muteUntil())
            }
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun makeActive(view: View, id: Int) {
        when (id) {
            R.id.eight_hours -> {
                view.eight_hours.makeActive()
                view.one_week.unActive()
                view.one_year.unActive()
                view.forever.unActive()
            }
            R.id.one_week -> {
                view.eight_hours.unActive()
                view.one_week.makeActive()
                view.one_year.unActive()
                view.forever.unActive()
            }
            R.id.one_year -> {
                view.eight_hours.unActive()
                view.one_week.unActive()
                view.one_year.makeActive()
                view.forever.unActive()
            }
            R.id.forever -> {
                view.eight_hours.unActive()
                view.one_week.unActive()
                view.one_year.unActive()
                view.forever.makeActive()
            }
        }
    }


    companion object {
        val CONVERSATON_ID = "conversationId"

        fun show(context: Context, conversationId: String) {
            context.startActivity(Intent(context, GroupInfo::class.java).apply {
                this.putExtra(CONVERSATON_ID, conversationId)
            })
        }
    }
}
