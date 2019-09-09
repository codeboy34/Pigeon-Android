package com.pigeonmessenger.activities

import android.Manifest
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.pigeonmessenger.R
import com.pigeonmessenger.adapter.ContactsAdapter
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.events.ContactsSyncEvent
import com.pigeonmessenger.extension.openPermissionSetting
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.job.UploadContactsJob
import com.pigeonmessenger.viewmodals.ContactsViewModal
import com.pigeonmessenger.widget.pulltorefresh.RecyclerRefreshLayout
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_contacts.*
import kotlinx.android.synthetic.main.card_title_view.view.*
import kotlinx.android.synthetic.main.contacts_header_edittext.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.toast
import javax.inject.Inject

class ContactsActivity : AppCompatActivity(), ContactsAdapter.ContactListener {

    private var contactList: List<User>? = null

    override fun onContactItem(user: User) {
        ChatRoom.show(this, null, recipientId = user.userId)
    }

    @Inject
    lateinit var jobManager: PigeonJobManager

    companion object {
        const val TAG = "ContactsActivity"
    }

    private val contactsAdapter: ContactsAdapter by lazy {
        ContactsAdapter(this@ContactsActivity)
    }

    private val contactsViewModal: ContactsViewModal by lazy {
        ViewModelProviders.of(this@ContactsActivity).get(ContactsViewModal::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        App.get().appComponent.inject(this)
        contacts_rv.layoutManager = LinearLayoutManager(this)
        titleView.back_iv.setOnClickListener { onBackPressed() }
        // contacts_rv.addItemDecoration(StickyRecyclerHeadersDecoration(contactsAdapter))
        val layoutInflater = LayoutInflater.from(this)
        val header = layoutInflater.inflate(R.layout.contacts_header_edittext, contacts_rv, false)

        header.search_et.addTextChangedListener(ContactsSearchWatcher())
        header.create_group.setOnClickListener {
            GroupActivity.show(this@ContactsActivity)
            finish()
        }
        contacts_rv.adapter = contactsAdapter

        pullToRefresh.setRefreshStyle(RecyclerRefreshLayout.RefreshStyle.PINNED)
        pullToRefresh.setOnRefreshListener {
            RxPermissions(this)
                    .request(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                    .subscribe { granted ->
                        if (granted) {
                            jobManager.addJobInBackground(UploadContactsJob())
                        } else {
                            openPermissionSetting()
                        }
                    }
        }

        UploadContactsJob.isRunning.observe(this, Observer {
            if (it) {
                pullToRefresh.setRefreshing(true)
            } else {
                pullToRefresh.setRefreshing(false)
            }
        })
        contactsAdapter.mContactListener = this
        contactsViewModal.getLiveUsers().observe(this, Observer {
            contactList = it
            if (!it.isNullOrEmpty()) {
                if (!contactsAdapter.isHeader()) contactsAdapter.setHeader(header)
                contactsAdapter.contacts = it
                empty_layout.visibility = GONE
            } else {
                if (contactsAdapter.isHeader()) contactsAdapter.setHeader(null)
                empty_layout.visibility = VISIBLE
            }
        })


    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncEvent(contactsSyncEvent: ContactsSyncEvent) {
        if (contactsSyncEvent.success) {
            toast(String.format(getString(R.string.sync_success), when {
                contactsSyncEvent.newContacts == 0 -> getString(R.string.now_new_contacts)
                contactsSyncEvent.newContacts == 1 -> getString(R.string.one_new_contact)
                else -> String.format(getString(R.string.new_contacts), contactsSyncEvent.newContacts)
            }))
        } else if (contactsSyncEvent.error) {
            when {
                contactsSyncEvent.throwable != null -> com.pigeonmessenger.utils.ErrorHandler.handleError(contactsSyncEvent.throwable!!)
                contactsSyncEvent.errorCode != null -> com.pigeonmessenger.utils.ErrorHandler.handleCode(contactsSyncEvent.errorCode!!)
                else -> toast(getString(R.string.something_went_wrong))
            }
        }
    }

    fun filter(s: String) {
        val us = arrayListOf<User>()
        contactList?.forEach {
            if (it.getName().contains(s, true)) {
                us.add(it)
            }
        }
        contactsAdapter.contacts = us
    }

    inner class ContactsSearchWatcher : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            filter(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }
    }
}
