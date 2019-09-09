package com.pigeonmessenger.fragment.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.api.request.SettingRequest
import com.pigeonmessenger.extension.addFragment
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.extension.putInt
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.SettingsViewModel
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_account_setting.*
import kotlinx.android.synthetic.main.sub_setting_item.view.*

/**
 * A simple [Fragment] subclass.
 *
 */
class AccountSettingFragment : Fragment() {

    companion object {
        const val TAG = "AccountSettingFragment"

        fun getInstance(): AccountSettingFragment {
            return AccountSettingFragment()
        }
    }

    private var isUpdating = false;

    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProviders.of(this).get(SettingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account_setting, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        invalidateLastSeen()
        invalidateProfileSeen()

        profile_nobody.setOnClickListener {
            val settingRequest = SettingRequest(profile = Constant.Settings.NOBODY)
            updateSetting(settingRequest)
        }
        profile_contacts.setOnClickListener {
            val settingRequest = SettingRequest(profile = Constant.Settings.CONTACTS)
            updateSetting(settingRequest)
        }
        profile_everybody.setOnClickListener {
            val settingRequest = SettingRequest(profile = Constant.Settings.EVERYONE)
            updateSetting(settingRequest)

        }

        seen_nobody.setOnClickListener {
            val settingRequest = SettingRequest(lastSeen = Constant.Settings.NOBODY)
            updateSetting(settingRequest)
        }
        seen_contacts.setOnClickListener {
            val settingRequest = SettingRequest(lastSeen = Constant.Settings.CONTACTS)
            updateSetting(settingRequest)
        }
        seen_everybody.setOnClickListener {
            val settingRequest = SettingRequest(lastSeen = Constant.Settings.EVERYONE)
            updateSetting(settingRequest)
        }

        block_bt.setOnClickListener {
            activity?.addFragment(this, BlockListFragment(),BlockListFragment.TAG)
        }
    }

    private fun preProfileInvalidate(profileSeen: Int) {

        when (profileSeen) {
            Constant.Settings.NOBODY -> {
                profile_nobody.check_iv.visibility = GONE
                profile_contacts.check_iv.visibility = GONE
                profile_everybody.check_iv.visibility = GONE
                profile_nobody.progress.visibility = VISIBLE
            }
            Constant.Settings.CONTACTS -> {
                profile_nobody.check_iv.visibility = GONE
                profile_contacts.check_iv.visibility = GONE
                profile_everybody.check_iv.visibility = GONE
                profile_contacts.progress.visibility = VISIBLE
            }
            Constant.Settings.EVERYONE -> {
                profile_nobody.check_iv.visibility = GONE
                profile_contacts.check_iv.visibility = GONE
                profile_everybody.check_iv.visibility = GONE
                profile_everybody.progress.visibility = VISIBLE
            }
        }
    }

    private fun preSeenInvalidate(seen: Int) {
        when (seen) {
            Constant.Settings.NOBODY -> {
                seen_nobody.check_iv.visibility = GONE
                seen_contacts.check_iv.visibility = GONE
                seen_everybody.check_iv.visibility = GONE
                seen_nobody.progress.visibility = VISIBLE
            }
            Constant.Settings.CONTACTS -> {
                seen_nobody.check_iv.visibility = GONE
                seen_contacts.check_iv.visibility = GONE
                seen_everybody.check_iv.visibility = GONE
                seen_contacts.progress.visibility = VISIBLE
            }
            Constant.Settings.EVERYONE -> {
                seen_nobody.check_iv.visibility = GONE
                seen_contacts.check_iv.visibility = GONE
                seen_everybody.check_iv.visibility = GONE
                seen_everybody.progress.visibility = VISIBLE
            }
        }
    }

    private fun invalidateProfileSeen() {
        val profileSeen = defaultSharedPreferences.getInt(Constant.Settings.PREF_KEY_PROFILE_SEEN, Constant.Settings.EVERYONE)
        when (profileSeen) {
            Constant.Settings.NOBODY -> {
                profile_nobody.check_iv.visibility = VISIBLE
                profile_contacts.check_iv.visibility = GONE
                profile_everybody.check_iv.visibility = GONE
                profile_nobody.progress.visibility = GONE
                profile_contacts.progress.visibility = GONE
                profile_everybody.progress.visibility = GONE

            }
            Constant.Settings.CONTACTS -> {
                profile_nobody.check_iv.visibility = GONE
                profile_contacts.check_iv.visibility = VISIBLE
                profile_everybody.check_iv.visibility = GONE
                profile_nobody.progress.visibility = GONE
                profile_contacts.progress.visibility = GONE
                profile_everybody.progress.visibility = GONE
            }
            Constant.Settings.EVERYONE -> {
                profile_nobody.check_iv.visibility = GONE
                profile_contacts.check_iv.visibility = GONE
                profile_everybody.check_iv.visibility = VISIBLE
                profile_nobody.progress.visibility = GONE
                profile_contacts.progress.visibility = GONE
                profile_everybody.progress.visibility = GONE
            }
        }
    }

    private fun invalidateLastSeen() {
        val seen = defaultSharedPreferences.getInt(Constant.Settings.PREF_KEY_SEEN_STATUS, Constant.Settings.EVERYONE)
        when (seen) {
            Constant.Settings.NOBODY -> {
                seen_nobody.check_iv.visibility = VISIBLE
                seen_contacts.check_iv.visibility = GONE
                seen_everybody.check_iv.visibility = GONE
                seen_nobody.progress.visibility = GONE
                seen_everybody.progress.visibility = GONE
                seen_contacts.progress.visibility = GONE

            }
            Constant.Settings.CONTACTS -> {
                seen_nobody.check_iv.visibility = GONE
                seen_contacts.check_iv.visibility = VISIBLE
                seen_everybody.check_iv.visibility = GONE
                seen_nobody.progress.visibility = GONE
                seen_everybody.progress.visibility = GONE
                seen_contacts.progress.visibility = GONE
            }
            Constant.Settings.EVERYONE -> {
                seen_nobody.check_iv.visibility = GONE
                seen_contacts.check_iv.visibility = GONE
                seen_everybody.check_iv.visibility = VISIBLE
                seen_nobody.progress.visibility = GONE
                seen_everybody.progress.visibility = GONE
                seen_contacts.progress.visibility = GONE
            }
        }
    }

    private fun updateSetting(settingRequest: SettingRequest) {
        if (isUpdating) return
        isUpdating = true
        if (settingRequest.profile != null) {
            val profileSeen = defaultSharedPreferences.getInt(Constant.Settings.PREF_KEY_PROFILE_SEEN, Constant.Settings.EVERYONE)
            if (profileSeen == settingRequest.profile)
                return
            preProfileInvalidate(settingRequest.profile)
        } else {
            val lastSeen = defaultSharedPreferences.getInt(Constant.Settings.PREF_KEY_SEEN_STATUS, Constant.Settings.EVERYONE)
            if (lastSeen == settingRequest.lastSeen)
                return
            preSeenInvalidate(settingRequest.lastSeen!!)
        }

        settingsViewModel.updateSetting(settingRequest).autoDisposable(scopeProvider).subscribe({
            isUpdating = false
            if (it.isSuccessful) {
                settingRequest.profile?.let { p ->
                    defaultSharedPreferences.putInt(Constant.Settings.PREF_KEY_PROFILE_SEEN, p)
                }
                settingRequest.lastSeen?.let { s ->
                    defaultSharedPreferences.putInt(Constant.Settings.PREF_KEY_SEEN_STATUS, s)
                }
                invalidateLastSeen()
                invalidateProfileSeen()
            } else {
                ErrorHandler.handleCode(it.code())
            }
        }, {
            invalidateLastSeen()
            invalidateProfileSeen()
            isUpdating = false
            ErrorHandler.handleError(it)
        })
    }
}
