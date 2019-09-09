package com.pigeonmessenger.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.Session
import com.pigeonmessenger.api.request.AccountUpdateRequest
import com.pigeonmessenger.api.request.ConversationRequest
import com.pigeonmessenger.events.ConversationEvent
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.AccountViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_account_update.*
import kotlinx.android.synthetic.main.card_title_view.view.*
import android.text.InputFilter



class AccountUpdateActivity : AppCompatActivity() {

    private val purpose: Int by lazy {
        intent.getIntExtra(ARG_PURPOSE, 0)
    }


    private val accountViewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this).get(AccountViewModel::class.java)
    }

    private val groupId: String by lazy {
        intent.getStringExtra(ARG_GROUP_ID)
    }

    private var etLimit  = 0
    private var oldData: String? = ""

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_update)

        titleView.back_iv.setOnClickListener { onBackPressed() }

        val account = Session.getAccount()


        when {
            purpose == PURPOSE_NAME -> {
                titleView.title_tv.text = getString(R.string.name)
                update_et.hint = getString(R.string.name)
                update_et.setText(account?.full_name)
                oldData = account?.full_name
                etLimit = USERNAME_LIMIT
            }
            PURPOSE_BIO == purpose -> {
                update_et.hint = getString(R.string.bio)
                titleView.title_tv.text = getString(R.string.bio)
                account!!.bio?.let {
                    oldData = account.bio
                    update_et.setText(account.bio)
                }
                etLimit = BIO_LIMIT
            }
            PURPOSE_GROUP_NAME == purpose -> {
                titleView.title_tv.text = getString(R.string.name)
                update_et.hint = getString(R.string.name)
                update_et.setText(intent.getStringExtra(GROUP_NAME))
                oldData = intent.getStringExtra(GROUP_NAME)
                etLimit = GROUPNAME_LIMIT
            }
        }
        update_et.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(etLimit))
        update_et.addTextChangedListener(TextWatchListener())
        update_fab.setOnClickListener { update() }
        updateCounter()
        if (disposable == null) {
            disposable = RxBus.listen(ConversationEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it.isSuccess) {
                            accountViewModel.updateGroupName(groupId, update_et.text.toString())
                        }
                        finish()
                    }
        }

    }

    @SuppressLint("CheckResult")
    private fun update() {
        update_progress.show()
        if (purpose == PURPOSE_NAME || purpose == PURPOSE_BIO) {
            val accountRequest = if (purpose == PURPOSE_NAME) {
                AccountUpdateRequest(fullName = update_et.text.toString())
            } else {
                AccountUpdateRequest(bio = update_et.text.toString())
            }
            accountViewModel.updateAccount(accountRequest)
                    .subscribe({
                        update_progress.hide()
                        if (it.isSuccessful) {
                            val account = Session.getAccount()
                            if (purpose == PURPOSE_NAME) account!!.full_name = update_et.text.toString()
                            else account!!.bio = update_et.text.toString()
                            Session.storeAccount(account)
                            finish()
                        } else {
                            ErrorHandler.handleCode(it.code())
                        }
                    }, {
                        update_progress.hide()
                        ErrorHandler.handleError(it)
                    })
        } else {
            updateGroupName()
        }
    }

    private fun updateGroupName() {
        val request = ConversationRequest(groupId, name = update_et.text.toString())
        accountViewModel.updateGroup(request)
    }

    inner class TextWatchListener : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.isNullOrEmpty()) {

                update_fab.visibility = GONE
            } else if (oldData == s) {
                if (update_fab.isVisible) update_fab.visibility = GONE
            } else if (!update_fab.isVisible) update_fab.visibility = VISIBLE
            updateCounter()
        }
    }

    private fun updateCounter(){
        counter_tv.text = String.format(getString(R.string.account_counter,update_et.text.length,etLimit))
    }

    companion object {
        const val PURPOSE_NAME = 0
        const val PURPOSE_BIO = 1
        const val PURPOSE_GROUP_NAME = 3
        const val ARG_PURPOSE = "arg_purpose"
        private const val ARG_GROUP_ID = "conversation_id"
        private const val GROUP_NAME = "group_name"
        private const val CONVERSATION_ID = "conversation"

        private const val USERNAME_LIMIT = 50;
        private const val BIO_LIMIT = 200;
        private const val GROUPNAME_LIMIT = 50


        fun show(context: Context, purpose: Int,
                 groupId: String? = null, groupName: String? = null) {
            val intent = Intent(context, AccountUpdateActivity::class.java)
            intent.putExtra(ARG_PURPOSE, purpose)
            intent.putExtra(ARG_GROUP_ID, groupId)
            intent.putExtra(GROUP_NAME, groupName)
            context.startActivity(intent)
        }
    }
}
