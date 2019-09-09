package com.pigeonmessenger.job

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.api.request.ContactRequest
import com.pigeonmessenger.api.response.ContactResponse
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.events.ContactsSyncEvent
import com.pigeonmessenger.extension.formatPhoneNumber
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.runOnUiThread
import java.util.*
import java.util.concurrent.CountDownLatch

class UploadContactsJob : BaseJob(Params(PRIORITY_BACKGROUND).requireNetwork(), "") {


    companion object {
        const val TAG = "UploadContactsJob"

        private val contactsProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER)

        class ContactsLiveData : LiveData<Boolean>() {
            var isRunning: Boolean = false
                set(value) {
                    field = value
                    setValue(value)
                }
        }

        val isRunning: ContactsLiveData = ContactsLiveData()

    }

    override fun cancel() {

    }

    @SuppressLint("CheckResult", "LogNotTimber")
    override fun onRun() {

        val ctx = App.get()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return

        val contactRequestList = localContacts(ctx)

        //TODO just for debugging remove it later//
        contactRequestList.forEach {
            Log.d(TAG, "${it}: ");
        }


        val userList = contactDao.getUsers()
        //insert new contacts
        contactRequestList.filter {
            userList.find { userItem -> userItem.userId == it.phoneNumber } == null
        }.map {
            User(it.displayName, it.phoneNumber, relationship = Relationship.UNREGISTERED_CONTACT.name)
        }.let {
            Log.d(TAG, "INSERT NEW CONTACTS :$it ");
            contactDao.insert(it)
        }

        //delete extra contacts
        userList.filter {
            contactRequestList.find { contactRequest -> contactRequest.phoneNumber == it.userId }
                    ?: it.relationship === Relationship.UNREGISTERED_CONTACT.name
            false
        }.let {
            Log.d(TAG, "DELETE :$it ");
            contactDao.delete(it)
        }

        //update deleted relationship
        userList.filter {
            val request: ContactRequest? = contactRequestList.find { contactRequest -> contactRequest.phoneNumber == it.userId }
            if (request == null) it.relationship === Relationship.REGISTERED_CONTACT.name
            false
        }.forEach {
            Log.d(TAG, "update deleted  relationship :$it ");
            contactsRepo.updateRelationship(it.userId, Relationship.STRANGE.name)
        }


        //update inserted relationship
        userList.filter {
            val request: ContactRequest? = contactRequestList.find { contactRequest -> contactRequest.phoneNumber == it.userId }
            if (request == null) false
            else it.relationship == Relationship.STRANGE.name
        }.forEach {
            Log.d(TAG, "update inserted relationship:$it ");
            contactsRepo.updateRelationship(it.userId, Relationship.REGISTERED_CONTACT.name)
        }

        //update displayName
        userList.forEach {
            val request: ContactRequest? = contactRequestList.find { contactRequest -> contactRequest.phoneNumber == it.userId }
            request?.let { req ->
                if (it.displayName != req.displayName) {
                    contactDao.updateDisplayName(it.userId, req.displayName)
                }
            }
        }
        val response = contactsService.syncContacts(contactRequestList).execute()

        if (response.isSuccessful) {
            val contactResponse: List<ContactResponse> = response.body()!!
            Log.d(TAG, "$contactResponse ");
            val contactSyncEvent = ContactsSyncEvent()
            contactResponse.filter {
                val user: User? = userList.find { user -> user.userId == it.phone_number }
                if (user == null) false
                else user.relationship != Relationship.REGISTERED_CONTACT.name
            }.forEach {
                Log.d(TAG, "$it: ");
                contactSyncEvent.newContacts++
                contactDao.updateContactAndRelationship(it.phone_number, it.full_name, it.bio)
            }

            jobManager.addJobInBackground(RefreshContactsJob())
        }
        setRunning(false)
        EventBus.getDefault().post(ContactsSyncEvent())
    }

    private fun localContacts(ctx: Context): List<ContactRequest> {
        val contactsList = mutableListOf<ContactRequest>()
        val me = Session.registeredPhoneNumber()
        val cursor = ctx.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                contactsProjection,
                null,
                null,
                "${contactsProjection[0]} ASC")

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(cursor.getColumnIndex(contactsProjection[0]))
                val phoneNumber = cursor.getString(cursor.getColumnIndex(contactsProjection[1]))
                phoneNumber.formatPhoneNumber()?.let {
                    val contactRequest = ContactRequest(it, displayName)
                    if (contactsList.find { listContact -> listContact.phoneNumber == it } == null && it != me)
                        contactsList.add(contactRequest)
                }
            }
            cursor.close()
        }
        return contactsList
    }

    @SuppressLint("CheckResult")
    fun getLocalContacts(ctx: Context): List<ContactRequest> {
        var contactsList: List<ContactRequest>? = null
        var countDownLatch = CountDownLatch(1)
        RxContacts.fetch(ctx)
                .toSortedList(Contact::compareTo)
                .subscribe { contacts ->
                    val mutableList = mutableListOf<ContactRequest>()
                    for (item in contacts) {
                        for (p in item.phoneNumbers) {
                            if (p == null) {
                                continue
                            }
                            try {
                                val phoneNum = PhoneNumberUtil.getInstance().parse(p, Locale.getDefault().country)
                                if (!PhoneNumberUtil.getInstance().isValidNumber(phoneNum)) continue
                                var phone = PhoneNumberUtil.getInstance().format(phoneNum, PhoneNumberUtil.PhoneNumberFormat.E164)
                                if (phone != null) {
                                    phone = PhoneNumberUtil.getInstance().parse(phone, "IN").nationalNumber.toString()
                                    mutableList.add(ContactRequest(phone, item.displayName))
                                }
                            } catch (e: Exception) {
                            }
                        }
                    }
                    contactsList = mutableList
                    countDownLatch.countDown()
                }
        countDownLatch.await()
        return contactsList!!
    }

    override fun shouldReRunOnThrowable(p0: Throwable, p1: Int, p2: Int): RetryConstraint {
        Log.d(TAG, "$p0: ");
        setRunning(false)
        return RetryConstraint.CANCEL
    }

    override fun onAdded() {

    }

    fun setRunning(boolean: Boolean) {
        App.get().runOnUiThread {
            isRunning.isRunning = boolean
        }
    }

    override fun onCancel(p0: Int, p1: Throwable?) {
        setRunning(false)
    }

}