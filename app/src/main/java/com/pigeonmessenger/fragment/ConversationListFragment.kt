package com.pigeonmessenger.fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager

import com.pigeonmessenger.R
import com.pigeonmessenger.activities.ChatRoom
import com.pigeonmessenger.activities.ContactsActivity
import com.pigeonmessenger.adapter.ConversationListAdapter
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.viewmodals.ConversationItem
import com.pigeonmessenger.viewmodals.MessageViewModal
import com.pigeonmessenger.widget.BottomSheet
import kotlinx.android.synthetic.main.fragment_conversation_list.*
import kotlinx.android.synthetic.main.view_conversation_bottom.view.*
import kotlinx.android.synthetic.main.view_empty.*

class ConversationListFragment : Fragment() , ConversationListAdapter.OnItemClickListener{

    private val conversationListAdapter: ConversationListAdapter by lazy {
        ConversationListAdapter()
    }

    private val conversationViewModal by lazy {
        ViewModelProviders.of(this).get(MessageViewModal::class.java)
    }

    companion object {
        fun newInstance() = ConversationListFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_conversation_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        conversation_rv.layoutManager = LinearLayoutManager(requireContext())
        conversation_rv.adapter = conversationListAdapter
        val recyclerViewState = conversation_rv.layoutManager?.onSaveInstanceState()
        conversation_rv.layoutManager?.onRestoreInstanceState(recyclerViewState)

       conversationViewModal.conversationList().observe(this, Observer {
            if (it.isNullOrEmpty()) {
                empty_view.visibility = View.VISIBLE
            } else {
                empty_view.visibility = View.GONE
                conversationListAdapter.setConversationList(it)
            }
        })

        start_bn.setOnClickListener {
            startActivity(Intent(requireContext(), ContactsActivity::class.java))
        }
        conversationListAdapter.onItemClickListener = this

    }

    override fun longClick(conversation: ConversationItem): Boolean {
        showBottomSheet(conversation.conversationId, conversation.pinTime != null)
        return true
    }

    override fun click(position: Int, conversation: ConversationItem) {
        val recipientId = if (!conversation.isGroup()) conversation.ownerId else null
        ChatRoom.show(requireContext(), conversation.conversationId, recipientId )
    }



    @SuppressLint("InflateParams")
    fun showBottomSheet(conversationId: String, hasPin: Boolean) {
        val builder = BottomSheet.Builder(requireContext())
        val view = View.inflate(ContextThemeWrapper(requireContext(), R.style.Custom), R.layout.view_conversation_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.delete_tv.setOnClickListener {
            conversationViewModal.deleteConversation(conversationId)
            bottomSheet.dismiss()
        }
        view.cancel_tv.setOnClickListener { bottomSheet.dismiss() }
        if (hasPin) {
            view.pin_tv.setText(R.string.conversation_pin_clear)
            view.pin_tv.setOnClickListener {
                conversationViewModal.updateConversationPinTimeById(conversationId, null)
                bottomSheet.dismiss()
            }
        } else {
            view.pin_tv.setText(R.string.conversation_pin)
            view.pin_tv.setOnClickListener {
                conversationViewModal.updateConversationPinTimeById(conversationId, nowInUtc())
                bottomSheet.dismiss()
            }
        }

        bottomSheet.show()
    }



}
