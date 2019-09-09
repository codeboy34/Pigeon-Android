package com.pigeonmessenger.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.replaceFragment
import com.pigeonmessenger.fragment.GroupFragment
import com.pigeonmessenger.fragment.GroupFragment.Companion.ARGS_CONVERSATION_ID
import com.pigeonmessenger.fragment.GroupInfoFragment
import com.pigeonmessenger.fragment.NewGroupFragment

class GroupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val type = intent.getIntExtra(ARGS_TYPE, 0)
        if (type == CREATE) {
            val fragment = GroupFragment.newInstance()
            replaceFragment(fragment, R.id.container, GroupFragment.TAG)
        } else if (type == INFO) {
          val f = GroupInfoFragment.newInstance(intent.getStringExtra(ARGS_CONVERSATION_ID))
           replaceFragment(f, R.id.container, GroupInfoFragment.TAG)
        }
    }

    companion object {
        private const val ARGS_TYPE = "args_type"
        const val ARGS_EXPAND = "args_expand"
        const val CREATE = 0
        const val INFO = 1

        fun show(context: Context, type: Int = CREATE, conversationId: String? = null, expand: Boolean = false) {
            context.startActivity(Intent(context, GroupActivity::class.java).apply {
                putExtra(ARGS_TYPE, type)
                putExtra(ARGS_EXPAND, expand)
                conversationId?.let {
                    putExtra(ARGS_CONVERSATION_ID, it)
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val newGroupFragment = supportFragmentManager.findFragmentByTag(NewGroupFragment.TAG)
        newGroupFragment?.onActivityResult(requestCode, resultCode, data)
    }
}
