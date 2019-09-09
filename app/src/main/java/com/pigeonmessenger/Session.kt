package com.pigeonmessenger

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.extension.clear
import com.pigeonmessenger.extension.putString
import com.pigeonmessenger.extension.sharedPreferences
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.vo.Account

class Session {
    companion object {
        class LiveAccount : LiveData<Account>() {
            var self: Account? = null
                set(value) {
                    if (value != null) {
                        field = value
                        setValue(value)
                    }
        }
        }

        private var userId: String? = null
        private var self: Account? = null
            set(value) {
                field = value
                liveAccount.self = value
            }

        var liveAccount: LiveAccount = LiveAccount().apply {
            this.self = getAccount()
        }

        fun storeAccount(account: Account) {
            self = account
            val preference = App.get().sharedPreferences(Constant.Account.PREF_SESSION)
            preference.putString(Constant.Account.PREF_NAME_ACCOUNT, Gson().toJson(account))
        }

        fun getAccount(): Account? = if (self != null) {
            self
        } else {
            val preference = App.get().sharedPreferences(Constant.Account.PREF_SESSION)
            val json = preference.getString(Constant.Account.PREF_NAME_ACCOUNT, "")
            if (!json.isNullOrBlank()) {
                Gson().fromJson<Account>(json, object : TypeToken<Account>() {}.type)
            } else {
                null
            }
        }

        fun clearAccount() {
            self = null
            val preference = App.get().sharedPreferences(Constant.Account.PREF_SESSION)
            preference.clear()
        }

        fun getPhoneNumberWithCountryCode(): String {
            return FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
        }

        fun getUserId(): String {
            if (userId == null) userId = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!.replace("+91", "")
            return userId!!
        }

        fun setPhoneNumber(phoneNumber: String) {
            val preference = App.get().sharedPreferences(Constant.Account.PREF_SESSION)
            preference.putString(Constant.Account.PREF_REG_PHONE_NUMBER, phoneNumber)
        }

        fun registeredPhoneNumber(): String? {
            val preference = App.get().sharedPreferences(Constant.Account.PREF_SESSION)
            return preference.getString(Constant.Account.PREF_REG_PHONE_NUMBER, null)
        }

    }

}