package com.pigeonmessenger.fragment.settings


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.*
import kotlinx.android.synthetic.main.card_title_view.view.*
import kotlinx.android.synthetic.main.fragment_notification_setting.*

val PREF_KEY_MESSAGE_NOTI_URI = "pref_message_notification_uri"
val PREF_KEY_GROUP_NOTI_URI = "pref_group_notification_uri"
val PREF_KEY_CALL_NOTI_URI = "pref_call_notification_uri"

val PREF_KEY_MESSAGE_VIBRATE = "pref_key_message_vibrate"
val PREF_KEY_GROUP_VIBRAE = "pref_key_group_vibrate"
val PREF_KEY_CALL_VIBRATE = "perf_key_call_vibrate"
val PREF_CONVERSATION_TONE = "conversation_tone"

class NotificationSettingFragment : Fragment() {

    private val REQ_CODE_MESSAGE = 1
    private val REQ_CODE_GROUP = 2
    private val REQ_CODE_CALL = 3

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notification_setting, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleView.back_iv.setOnClickListener { activity?.onBackPressed() }
        val conversationTone =  defaultSharedPreferences.getBoolean(PREF_CONVERSATION_TONE,false)
        checkBox.isChecked  = conversationTone
        invalidateNotificationTone()
        invalidateVibrate()

        checkBox.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_CONVERSATION_TONE,!checkBox.isChecked)
        }
        message_noti.setOnClickListener {
            val messageNoti = defaultSharedPreferences.getString(PREF_KEY_MESSAGE_NOTI_URI, null)
            pickRingtone(REQ_CODE_MESSAGE, messageNoti)
        }

        group_noti.setOnClickListener {
            val groupNoti = defaultSharedPreferences.getString(PREF_KEY_GROUP_NOTI_URI, null)
            pickRingtone(REQ_CODE_GROUP, groupNoti)
        }

        call_noti.setOnClickListener {
            val callNoti = defaultSharedPreferences.getString(PREF_KEY_CALL_NOTI_URI, null)
            pickRingtone(REQ_CODE_CALL, callNoti)
        }

        message_vib.setOnClickListener {
            showVibrateDialog(PREF_KEY_MESSAGE_VIBRATE)
        }

        group_vib.setOnClickListener {
            showVibrateDialog(PREF_KEY_GROUP_VIBRAE)
        }
        call_vib.setOnClickListener {
            showVibrateDialog(PREF_KEY_CALL_VIBRATE)
        }
    }

    private fun pickRingtone(requestCode: Int, existingUri: String?) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
        notNullElse(existingUri, {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUri))
        }, {
            //intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri)null)
        })

        this.startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                val key = when (requestCode) {
                    REQ_CODE_MESSAGE -> PREF_KEY_MESSAGE_NOTI_URI
                    REQ_CODE_GROUP -> PREF_KEY_GROUP_NOTI_URI
                    REQ_CODE_CALL -> PREF_KEY_CALL_NOTI_URI
                    else -> null
                }
                defaultSharedPreferences.putString(key!!, uri.toString())
                invalidateNotificationTone()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun invalidateNotificationTone() {
        val messageNoti = defaultSharedPreferences.getString(PREF_KEY_MESSAGE_NOTI_URI, null)
        val groupNoti = defaultSharedPreferences.getString(PREF_KEY_GROUP_NOTI_URI, null)
        val callNoti = defaultSharedPreferences.getString(PREF_KEY_CALL_NOTI_URI, null)

        messageNoti?.let { msg_noti_tv.text = getString(R.string.notification_) + Uri.parse(it).lastPathSegment }
        groupNoti?.let { group_noti_tv.text = getString(R.string.notification_) + Uri.parse(it).lastPathSegment }
        callNoti?.let { call_noti_tv.text = getString(R.string.notification_) + Uri.parse(it).lastPathSegment }

    }

    private fun invalidateVibrate() {
        val messageVib = defaultSharedPreferences.getInt(PREF_KEY_MESSAGE_VIBRATE, 1)
        val groupVib = defaultSharedPreferences.getInt(PREF_KEY_GROUP_VIBRAE, 1)
        val callVib = defaultSharedPreferences.getInt(PREF_KEY_CALL_VIBRATE, 1)

        msg_vib_tv.text = options[messageVib]
        group_vib_tv.text = options[groupVib]
        call_vib_tv.text = options[callVib]
    }

    private val options by lazy {
        requireContext().resources.getStringArray(R.array.vibrate_dialog_list)
    }

    private fun showVibrateDialog(key: String) {
        val builder = AlertDialog.Builder(requireContext(),R.style.PigeonDialogTheme)
        builder.setTitle(R.string.vibrate)

        val checkedItem = defaultSharedPreferences.getInt(key, 1)
        builder.setSingleChoiceItems(options, checkedItem) { dialog, which ->
            defaultSharedPreferences.putInt(key, which)
            invalidateVibrate()
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel) { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
    }

    companion object {
        const val TAG = "NotificationSettingFragment"

        fun getInstance(): NotificationSettingFragment {
            return NotificationSettingFragment()
        }
    }


}
