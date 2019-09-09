package com.pigeonmessenger.activities

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.ForwardActivity.Companion.ARGS_MESSAGES
import com.pigeonmessenger.activities.ForwardActivity.Companion.ARGS_SHARE
import com.pigeonmessenger.adapter.ForwardAdapter
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.openPermissionSetting
import com.pigeonmessenger.viewmodals.ConversationItem
import com.pigeonmessenger.viewmodals.MessageViewModal
import com.pigeonmessenger.vo.ForwardCategory
import com.pigeonmessenger.vo.ForwardMessage
import com.tbruyelle.rxpermissions2.RxPermissions
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.card_title_view.view.*
import kotlinx.android.synthetic.main.fragment_forward.*
import org.jetbrains.anko.bundleOf

class ForwardFragment : Fragment() {
    companion object {
        const val TAG = "ForwardFragment"

        fun newInstance(messages: ArrayList<ForwardMessage>, isShare: Boolean = false): ForwardFragment {
            val fragment = ForwardFragment()
            val b = bundleOf(
                    ARGS_MESSAGES to messages,
                    ARGS_SHARE to isShare
            )
            fragment.arguments = b
            return fragment
        }
    }


    private val chatViewModel: MessageViewModal by lazy {
        ViewModelProviders.of(this).get(MessageViewModal::class.java)
    }

    private val adapter by lazy {
        ForwardAdapter()
    }
    var conversations: List<ConversationItem>? = null
    var friends: List<User>? = null

    private val messages: ArrayList<ForwardMessage>? by lazy {
        arguments!!.getParcelableArrayList<ForwardMessage>(ARGS_MESSAGES)
    }

    private val isShare: Boolean by lazy {
        arguments!!.getBoolean(ARGS_SHARE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_forward, container, false)

    private fun setForwardText() {
        if (adapter.selectItem.size > 0) {
            forward_group.visibility = View.VISIBLE
        } else {
            forward_group.visibility = View.GONE
        }
        val str = StringBuffer()
        for (i in adapter.selectItem.size - 1 downTo 0) {
            adapter.selectItem[i].let {
                if (it is ConversationItem) {
                    str.append(it.name())
                    if (i != 0) {
                        str.append(",")
                    }
                } else if (it is User) {
                    str.append(it.getName())
                    if (i != 0) {
                        str.append(",")
                    }
                } else {
                }
            }
        }
        forward_tv.text = str
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (isShare) {
            titleView.title_tv.title_tv.text = getString(R.string.share_file)
        }
        titleView.back_iv.setOnClickListener { activity?.onBackPressed() }
        forward_rv.layoutManager = LinearLayoutManager(requireContext())
        forward_rv.adapter = adapter
        forward_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        forward_bn.setOnClickListener {
            if (adapter.selectItem.size == 1) {
                adapter.selectItem[0].let {
                    if (it is User) {
                        sendSingleMessage(null, it.userId)
                    } else if (it is ConversationItem) {
                        sendSingleMessage(it.conversationId, null)
                    }
                }
            } else {
                sendMessages()
            }
        }
        adapter.setForwardListener(object : ForwardAdapter.ForwardListener {
            override fun onConversationItemClick(item: ConversationItem) {
                if (adapter.selectItem.contains(item)) {
                    adapter.selectItem.remove(item)
                } else {
                    adapter.selectItem.add(item)
                }
                setForwardText()
            }

            override fun onUserItemClick(user: User) {
                if (adapter.selectItem.contains(user)) {
                    adapter.selectItem.remove(user)
                } else {
                    adapter.selectItem.add(user)
                }
                setForwardText()
            }
        })

        chatViewModel.conversationList().observe(this, Observer {
            it?.let {
                conversations = it

                chatViewModel.getFriends().observe(this, Observer { r ->
                    if (r != null) {
                        friends = r
                        adapter.friends = r
                    }
                    adapter.notifyDataSetChanged()
                })
            }
        })
        search_et.addTextChangedListener(mWatcher)
    }

    @SuppressLint("CheckResult")
    private fun sendMessages() {
        if (messages?.find { it.type == ForwardCategory.VIDEO.name || it.type == ForwardCategory.IMAGE.name } != null) {
            RxPermissions(requireActivity())
                    .request(
                            WRITE_EXTERNAL_STORAGE,
                            READ_EXTERNAL_STORAGE)
                    .subscribe({ granted ->
                        if (granted) {
                            chatViewModel.sendForwardMessages(adapter.selectItem, messages)
                            requireActivity().finish()
                            sharePreOperation()
                        } else {
                            requireContext().openPermissionSetting()
                        }
                    }, {
                    })
        } else {
            chatViewModel.sendForwardMessages(adapter.selectItem, messages)
            sharePreOperation()
        }
    }

    @SuppressLint("CheckResult")
    private fun sendSingleMessage(conversationId: String?, userId: String?) {
        if (messages?.find { it.type == ForwardCategory.VIDEO.name || it.type == ForwardCategory.IMAGE.name } != null) {
            RxPermissions(requireActivity())
                    .request(
                            WRITE_EXTERNAL_STORAGE,
                            READ_EXTERNAL_STORAGE)
                    .subscribe({ granted ->
                        if (granted) {
                            sharePreOperation()
                            //      ConversationActivity.show(requireContext(), conversationId, userId, messages = messages)
                        } else {
                            requireContext().openPermissionSetting()
                        }
                    }, {
                    })
        } else {
            sharePreOperation()
            // ConversationActivity.show(requireContext(), conversationId, userId, messages = messages)
        }
    }

    private fun sharePreOperation() {
        if (isShare) {
            startActivity(Intent(context, SplashActivity::class.java))
            activity?.finish()
        } else {
            activity?.finish()
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            adapter.conversations = conversations?.filter {
                it.name().contains(s.toString(), ignoreCase = true)

            }
            adapter.friends = friends?.filter {
                it.getName().contains(s.toString(), ignoreCase = true)
            }
            adapter.showHeader = s.isNullOrEmpty()
            adapter.notifyDataSetChanged()
        }
    }
}
