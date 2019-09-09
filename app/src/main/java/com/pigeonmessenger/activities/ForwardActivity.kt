package com.pigeonmessenger.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.extension.replaceFragment
import com.pigeonmessenger.utils.ShareHelper
import com.pigeonmessenger.vo.ForwardCategory
import com.pigeonmessenger.vo.ForwardMessage
import org.jetbrains.anko.toast

class ForwardActivity : AppCompatActivity() {
    companion object {
        var ARGS_MESSAGES = "args_messages"
        var ARGS_SHARE = "args_share"

        fun show(context: Context, messages: ArrayList<ForwardMessage>, isShare: Boolean = false) {
            val intent = Intent(context, ForwardActivity::class.java).apply {
                putParcelableArrayListExtra(ARGS_MESSAGES, messages)
                putExtra(ARGS_SHARE, isShare)
            }
            context.startActivity(intent)
        }

        fun show(context: Context, link: String?) {
            val intent = Intent(context, ForwardActivity::class.java).apply {
                val list = ArrayList<ForwardMessage>().apply {
                    add(ForwardMessage(ForwardCategory.TEXT.name, content = link))
                }
                putParcelableArrayListExtra(ARGS_MESSAGES, list)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val list = intent.getParcelableArrayListExtra<ForwardMessage>(ARGS_MESSAGES)
        if (list != null && list.isNotEmpty()) {
            val f = ForwardFragment.newInstance(list, intent.getBooleanExtra(ARGS_SHARE, false))
            replaceFragment(f, R.id.container, ForwardFragment.TAG)
        } else {
            if (Session.getAccount() == null) {
                toast(R.string.not_logged_in)
                finish()
                return
            }
            val forwardMessageList = ShareHelper.get().generateForwardMessageList(intent)
            if (forwardMessageList != null && forwardMessageList.isNotEmpty()) {
                replaceFragment(
                    ForwardFragment.newInstance(forwardMessageList, true),
                    R.id.container,
                    ForwardFragment.TAG
                )
            } else {
                toast(R.string.error_share)
                finish()
            }
        }
    }
}
