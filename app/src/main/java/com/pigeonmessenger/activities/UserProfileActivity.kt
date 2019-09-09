package com.pigeonmessenger.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.job.LinkState
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.ContactsViewModal
import com.pigeonmessenger.vo.CallState
import com.pigeonmessenger.webrtc.CallService
import com.pigeonmessenger.widget.BottomSheet
import com.pigeonmessenger.widget.PbDialog
import com.pigeonmessenger.widget.sweetalert.SweetAlertDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.card_title_view.view.*
import kotlinx.android.synthetic.main.item_mute_time.view.*
import kotlinx.android.synthetic.main.item_setting.view.*
import kotlinx.android.synthetic.main.mute_bottom_layout.view.*
import kotlinx.android.synthetic.main.user_profile_test.*
import org.jetbrains.anko.toast
import javax.inject.Inject

class UserProfileActivity : AppCompatActivity() {
    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    @Inject
    lateinit var linkState: LinkState


    @Inject
    lateinit var callState: CallState

    private var isMute = false
    private var pbDialog: PbDialog? = null
    private var isBlocked = false

    private val contactsViewModel: ContactsViewModal by lazy {
        ViewModelProviders.of(this).get(ContactsViewModal::class.java)
    }
    private val conversationId: String by lazy {

        intent.getStringExtra(CONVERSATION_ID)
    }

    private val userId: String by lazy {
        intent.getStringExtra(CONTACT_ID)
    }
    private var conversationUser: User? = null

    companion object {
        const val CONTACT_ID = "contact_id"
        const val CONVERSATION_ID = "conversation_id"

        fun show(context: Context, contactId: String, conversationId: String) {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra(CONTACT_ID, contactId)
            intent.putExtra(CONVERSATION_ID, conversationId)
            context.startActivity(intent)
        }

        private const val TAG = "UserProfileActivity"

    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_profile_test)
        App.get().appComponent.inject(this)

        titleView.back_iv.setOnClickListener { onBackPressed() }

        mobile_tv.text = "+91$userId"

        contactsViewModel.getContact(userId).observe(this, Observer {
            conversationUser = it
            name_tv.text = it.getName()
            notNullElse(it.bio, {
                bio_tv.text = it
            }, {
                bio_tv.visibility = GONE
            })
            avatar_iv.loadAvatar(avatarFile(userId), it.thumbnail, R.drawable.avatar_contact)

            if (!applicationContext.avatarFile(userId).exists() && it.thumbnail != null) {
                Log.d(TAG, "file not exist but thumbnail availlable...")
                contactsViewModel.fetchAvatar(it.userId)
            }

            mute.title.text = if (it.isMute()) {
                isMute = true
                getString(R.string.unmute)
            } else {
                isMute = false
                getString(R.string.sub_mute)
            }

            block.title.text = if (it.relationship == Relationship.BLOCKING.name) {
                isBlocked = true
                getString(R.string.unblock)
            } else {
                isBlocked = false
                getString(R.string.block)
            }

        })

        avatar_iv.setOnClickListener {
            if(avatarFile(userId).exists() && avatarFile(userId).length()>0)
            AvatarPreviewActivity.show(this, userId)
        }
        clear_chat.setOnClickListener { showClear() }

        shared_media.setOnClickListener {
            Log.d("UserProfileActivity", " ConversationId ${conversationId} ");
            SharedMediaActivity.show(this, conversationId)
        }

        titleView.back_iv.setOnClickListener {
            onBackPressed()
        }

        block.setOnClickListener {
            if (isBlocked)
                showUnblock()
            else showBlock()
        }

        mute.setOnClickListener {
            if (isMute) contactsViewModel.mute(userId, null)
            else showMuteBottom()
        }

        chat.setOnClickListener {
            val intent = Intent(this, ChatRoom::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ChatRoom.show(this, intent, userId)
            finish()
        }

        audioCall.setOnClickListener {
            if (!callState.isIdle()) {
                if (callState.user?.userId == userId && callState.callType == CallState.CallType.AUDIO) {
                    CallActivity.show(this, conversationUser)
                } else {
                    toast(getString(R.string.chat_call_warning_call))
                }
            } else {
                RxPermissions(this)
                        .request(Manifest.permission.RECORD_AUDIO)
                        .subscribe({ granted ->
                            if (granted) {
                                callVoice(false)
                            } else {
                                openPermissionSetting()
                            }
                        }, {
                        })
            }
        }

        videoCall.setOnClickListener {
            if (!callState.isIdle()) {
                if (callState.user?.userId == userId && callState.callType == CallState.CallType.VIDEO) {
                    CallActivity.show(this, conversationUser)
                } else {
                    toast(getString(R.string.chat_call_warning_call))
                }
            } else {
                RxPermissions(this)
                        .request(Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA)
                        .subscribe({ granted ->
                            if (granted) {
                                callVoice(true)
                            } else {
                                openPermissionSetting()
                            }
                        }, {
                        })
            }
        }
    }

    private fun callVoice(videoCall: Boolean) {
        if (LinkState.isOnline(linkState.state)) {
            if (videoCall)
                callState.callType = CallState.CallType.VIDEO
            else callState.callType = CallState.CallType.AUDIO

            CallService.outgoing(this, conversationUser!!, conversationId)
        } else {
            toast(R.string.error_no_connection)
        }
    }


    private fun showClear() {
        SweetAlertDialog(this)
                .setTitleText(getString(R.string.dialog_clear_chat_title))
                .setConfirmText(getString(R.string.clear).toUpperCase())
                .setCancelText(getString(R.string.cancel).toUpperCase())
                .setContentText(getString(R.string.sure_clear_chat))
                .setCancelClickListener { it.dismissWithAnimation() }
                .setConfirmClickListener {
                    contactsViewModel.clearConversationById(conversationId)
                    toast(R.string.cleared)
                    it.dismissWithAnimation()
                }
                .show()
    }

    private fun showBlock() {
        SweetAlertDialog(this)
                .setTitleText(getString(R.string.dialog_block_title))
                .setContentText(getString(R.string.sure_block))
                .setConfirmText(getString(R.string.block).toUpperCase())
                .setCancelText(getString(R.string.cancel).toUpperCase())
                .setConfirmClickListener {
                    block()
                    it.dismissWithAnimation()
                }.setCancelClickListener {
                    it.dismissWithAnimation()
                }
                .show()
    }

    private fun showUnblock() {
        SweetAlertDialog(this)
                .setTitleText(getString(R.string.dialog_unblock_title))
                .setContentText(getString(R.string.sure_unblock))
                .setConfirmText(getString(R.string.unblock).toUpperCase())
                .setCancelText(getString(R.string.cancel).toUpperCase())
                .setConfirmClickListener {
                    unblock()
                    it.dismissWithAnimation()
                }.setCancelClickListener {
                    it.dismissWithAnimation()
                }
                .show()
    }

    private fun unblock() {
        if (pbDialog == null) pbDialog = progressDialog()

        pbDialog?.addMessage(this, R.string.unblocking)
        pbDialog?.show(supportFragmentManager, "")
        contactsViewModel.unblock(userId).autoDisposable(scopeProvider).subscribe({
            if (it.isSuccessful) contactsViewModel.updateBlockRelationship(userId, Relationship.STRANGE.name)
            else ErrorHandler.handleCode(it.code())
            pbDialog?.dismiss()
        }, {
            pbDialog?.dismiss()
            ErrorHandler.handleError(it)
        })
    }

    @SuppressLint("CheckResult")
    fun block() {
        if (pbDialog == null) pbDialog = progressDialog()

        pbDialog?.addMessage(this, R.string.blocking)
        pbDialog?.show(supportFragmentManager, "")
        contactsViewModel.block(userId).autoDisposable(scopeProvider).subscribe({
            if (it.isSuccessful) contactsViewModel.updateBlockRelationship(userId, Relationship.BLOCKING.name)
            else {
                Log.e(TAG, "BLOCK ERROR CODE ${it.code()}")
                ErrorHandler.handleCode(it.code())
            }
            pbDialog?.dismiss()
        }, {
            Log.e(TAG, "BLOCKING ERROR", it)
            pbDialog?.dismiss()
            ErrorHandler.handleError(it)
        })
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
                view.eight_hours.isActive() -> contactsViewModel.mute(userId, Constant.MUTE_8_HOURS.muteUntil())
                view.one_week.isActive() -> contactsViewModel.mute(userId, Constant.MUTE_1_WEEK.muteUntil())
                view.one_year.isActive() -> contactsViewModel.mute(userId, Constant.MUTE_1_YEAR.muteUntil())
                view.forever.isActive() -> contactsViewModel.mute(userId, Constant.MUTE_FOREVER.muteUntil())
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


}
