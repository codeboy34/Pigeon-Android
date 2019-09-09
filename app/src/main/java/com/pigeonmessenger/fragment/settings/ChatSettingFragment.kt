package com.pigeonmessenger.fragment.settings


import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.extension.putBoolean
import com.pigeonmessenger.extension.replaceFragment
import com.pigeonmessenger.viewmodals.SettingsViewModel
import kotlinx.android.synthetic.main.card_title_view.view.*
import kotlinx.android.synthetic.main.fragment_chat_setting.*
import kotlinx.android.synthetic.main.sub_setting_item.view.*
import org.jetbrains.anko.toast

val PREF_DOWN_IMG_MOB = "pref_down_img_mob"
val PREF_DOWN_AUDIO_MOB = "pref_down_audio_mob"
val PREF_DOWN_FILE_MOB = "pref_down_file_mob"

val PREF_DOWN_IMG_WIFI = "pref_down_img_wifi"
val PREF_DOWN_AUDIO_WIFI = "pref_down_audio_wifi"
val PREF_DOWN_FILE_WIFI = "pref_down_file_wifi"
val PREF_IS_ENTER_SEND = "pref_is_key_send"


class ChatSettingFragment : Fragment() {


    companion object {
        const val TAG = "ChatSettingFragment"

        fun getInstance(): ChatSettingFragment {
            return ChatSettingFragment()
        }
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProviders.of(this).get(SettingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_chat_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleView.back_iv.setOnClickListener { activity?.onBackPressed() }
        validate()
        validateEnterKey()
        img_down_mb.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_DOWN_IMG_MOB, !img_down_mb.check_iv.isVisible)
            validate()
        }
        audio_down_mb.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_DOWN_AUDIO_MOB, !audio_down_mb.check_iv.isVisible)
            validate()
        }
        file_down_mb.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_DOWN_FILE_MOB, !file_down_mb.check_iv.isVisible)
            validate()
        }
        img_down_wifi.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_DOWN_IMG_WIFI, !img_down_wifi.check_iv.isVisible)
            validate()
        }
        audio_down_wifi.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_DOWN_AUDIO_WIFI, !audio_down_wifi.check_iv.isVisible)
            validate()
        }
        file_down_wifi.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_DOWN_FILE_WIFI, !file_down_wifi.check_iv.isVisible)
            validate()
        }

        backup.setOnClickListener {
            activity?.replaceFragment(BackUpFragment.newInstance(), R.id.container, BackUpFragment.TAG)
        }


        checkBox.setOnClickListener {
            defaultSharedPreferences.putBoolean(PREF_IS_ENTER_SEND, !checkBox.isChecked)
            validateEnterKey()
        }

        clear_chat.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.PigeonAlertDialogTheme)
                    .setMessage(R.string.confirm_all_clear)
                    .setPositiveButton("CLEAR") { dialog, _ ->
                        settingsViewModel.clearAllChat()
                        requireContext().toast("Cleared")
                        dialog.dismiss()
                    }
                    .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }.create().show()
        }

        delete_chat.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.PigeonAlertDialogTheme)
                    .setMessage(R.string.confirm_all_delete)
                    .setPositiveButton("DELETE") { dialog, _ ->
                        settingsViewModel.clearAllChat()
                        requireContext().toast("Deleted")
                        dialog.dismiss()
                    }
                    .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }.create().show()
        }
    }

    private fun validateEnterKey() {
        val enterKey = defaultSharedPreferences.getBoolean(PREF_IS_ENTER_SEND, false)
        checkBox.isChecked = !enterKey
    }

    private fun validate() {
        val imgMob = defaultSharedPreferences.getBoolean(PREF_DOWN_IMG_MOB, false)
        val audioMob = defaultSharedPreferences.getBoolean(PREF_DOWN_AUDIO_MOB, false)
        val fileMob = defaultSharedPreferences.getBoolean(PREF_DOWN_FILE_MOB, false)


        img_down_mb.check_iv.visibility = if (imgMob) VISIBLE else GONE
        audio_down_mb.check_iv.visibility = if (audioMob) VISIBLE else GONE
        file_down_mb.check_iv.visibility = if (fileMob) VISIBLE else GONE


        val imgWifi = defaultSharedPreferences.getBoolean(PREF_DOWN_IMG_WIFI, false)
        val audioWifi = defaultSharedPreferences.getBoolean(PREF_DOWN_AUDIO_WIFI, false)
        val fileWifi = defaultSharedPreferences.getBoolean(PREF_DOWN_FILE_WIFI, false)

        img_down_wifi.check_iv.visibility = if (imgWifi) VISIBLE else GONE
        audio_down_wifi.check_iv.visibility = if (audioWifi) VISIBLE else GONE
        file_down_wifi.check_iv.visibility = if (fileWifi) VISIBLE else GONE
    }

}
