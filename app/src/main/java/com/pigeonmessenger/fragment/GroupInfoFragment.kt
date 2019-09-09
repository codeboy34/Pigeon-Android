package com.pigeonmessenger.fragment

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import android.text.Editable
import android.text.TextWatcher
import android.util.ArrayMap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pigeonmessenger.R
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.activities.ChatRoom
import com.pigeonmessenger.activities.HomeActivity
import com.pigeonmessenger.activities.UserProfileActivity
import com.pigeonmessenger.adapter.GroupInfoAdapter
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.database.room.entities.generateConversationId
import com.pigeonmessenger.database.room.entities.isGroup
import com.pigeonmessenger.events.ConversationEvent
import com.pigeonmessenger.extension.addFragment
import com.pigeonmessenger.extension.addMessage
import com.pigeonmessenger.extension.progressDialog
import com.pigeonmessenger.fragment.GroupFragment.Companion.ARGS_CONVERSATION_ID
import com.pigeonmessenger.fragment.GroupFragment.Companion.MAX_USER
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_ADD
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_DELETE
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_EXIT
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_MAKE_ADMIN
import com.pigeonmessenger.job.ConversationJob.Companion.TYPE_REMOVE
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.viewmodals.GroupViewModel
import com.pigeonmessenger.vo.Participant
import com.pigeonmessenger.vo.ParticipantRole
import com.pigeonmessenger.widget.PbDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_group_info.*
import kotlinx.android.synthetic.main.view_group_info_header.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import javax.inject.Inject

class GroupInfoFragment : Fragment() {
    companion object {
        const val TAG = "GroupInfoFragment"

        fun newInstance(conversationId: String): GroupInfoFragment {
            val fragment = GroupInfoFragment()
            val b = Bundle().apply {
                putString(ARGS_CONVERSATION_ID, conversationId)
            }
            fragment.arguments = b
            return fragment
        }
    }


    @Inject
    lateinit var jobManager: PigeonJobManager

    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProviders.of(this).get(GroupViewModel::class.java)
    }


    private val adapter: GroupInfoAdapter = GroupInfoAdapter()

    private val conversationId: String by lazy {
        arguments!!.getString(ARGS_CONVERSATION_ID)
    }
    private var self: User? = null
    private var participantsMap: ArrayMap<String, Participant> = ArrayMap()
    private var users = arrayListOf<User>()
    private var disposable: Disposable? = null
    private lateinit var header: View
    private var dialog: PbDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            LayoutInflater.from(context).inflate(R.layout.fragment_group_info, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        App.get().appComponent.inject(this)
        group_info_rv.layoutManager = LinearLayoutManager(requireContext())
        group_info_rv.adapter = adapter
        header = LayoutInflater.from(context).inflate(R.layout.view_group_info_header, group_info_rv, false)
        adapter.headerView = header
        adapter.setGroupInfoListener(object : GroupInfoAdapter.GroupInfoListener {
            override fun onAdd() {
                modifyMember(true)
            }

            override fun onClick(name: View, user: User) {
                val choices = mutableListOf<String>()
                choices.add(getString(R.string.group_pop_menu_message, user.getName()))
                choices.add(getString(R.string.group_pop_menu_view, user.getName()))
                var role: String? = null
                self?.let {
                    val p = participantsMap[it.userId]
                    p?.let { role = p.role }
                }
                if (role == ParticipantRole.OWNER.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole == ParticipantRole.ADMIN.name) {
                        choices.add(getString(R.string.group_pop_menu_remove, user.getName()))
                    } else {
                        choices.add(getString(R.string.group_pop_menu_remove, user.getName()))
                        choices.add(getString(R.string.group_pop_menu_make_admin))
                    }
                } else if (role == ParticipantRole.ADMIN.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole != ParticipantRole.OWNER.name && userRole != ParticipantRole.ADMIN.name) {
                        choices.add(getString(R.string.group_pop_menu_remove, user.getName()))
                    }
                }
                AlertDialog.Builder(context!!)
                        .setItems(choices.toTypedArray()) { _, which ->
                            when (which) {
                                0 -> {
                                    openChat(user)
                                }
                                1 -> {
                                    viewUser(user.userId)
                                }
                                2 -> {
                                    showConfirmDialog(getString(R.string.group_info_remove_tip,
                                            user.getName(), adapter.getConversation()?.name), TYPE_REMOVE, user = user)
                                }
                                3 -> {
                                    showPb(R.string.adding_role)
                                    groupViewModel.makeAdmin(conversationId, user)
                                }
                            }
                        }.show()
            }

            override fun onLongClick(name: View, user: User): Boolean {
                val popMenu = PopupMenu(activity!!, name)
                val c = adapter.getConversation()
                if (c == null || !c.isGroup()) {
                    return false
                }
                var role: String? = null
                self?.let {
                    val p = participantsMap[it.userId]
                    p?.let { role = p.role }
                }
                if (role == ParticipantRole.OWNER.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole == ParticipantRole.ADMIN.name) {
                        popMenu.menuInflater.inflate(R.menu.group_item_admin, popMenu.menu)
                    } else {
                        popMenu.menuInflater.inflate(R.menu.group_item_owner, popMenu.menu)
                    }
                    popMenu.menu.findItem(R.id.remove).title = getString(R.string.group_pop_menu_remove, user.getName())
                } else if (role == ParticipantRole.ADMIN.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole == ParticipantRole.OWNER.name || userRole == ParticipantRole.ADMIN.name) {
                        popMenu.menuInflater.inflate(R.menu.group_item_simple, popMenu.menu)
                    } else {
                        popMenu.menuInflater.inflate(R.menu.group_item_admin, popMenu.menu)
                        popMenu.menu.findItem(R.id.remove).title =
                                getString(R.string.group_pop_menu_remove, user.getName())
                    }
                } else {
                    popMenu.menuInflater.inflate(R.menu.group_item_simple, popMenu.menu)
                }
                popMenu.menu.findItem(R.id.message).title = getString(R.string.group_pop_menu_message, user.getName())
                popMenu.menu.findItem(R.id.view).title = getString(R.string.group_pop_menu_view, user.getName())
                popMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.message -> {
                            openChat(user)
                        }
                        R.id.view -> {
                            viewUser(user.userId)
                        }
                        R.id.remove -> {
                            showConfirmDialog(getString(R.string.group_info_remove_tip,
                                    user.getName(), adapter.getConversation()?.name),
                                    TYPE_REMOVE, user = user)
                        }
                        R.id.admin -> {
                            showPb(R.string.adding_role)
                            groupViewModel.makeAdmin(conversationId, user)
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                popMenu.show()
                return true
            }
        })


        groupViewModel.getGroupParticipantsLiveData(conversationId).observe(this, Observer { u ->
            u?.let {
                var role: String? = null
                self?.let {
                    val p = participantsMap[it.userId]
                    p?.let { role = p.role }
                }
                users.clear()
                users.addAll(u)

                header.add_rl.visibility = if (it.isEmpty() || it.size >= MAX_USER || role == null ||
                        (role != ParticipantRole.OWNER.name && role != ParticipantRole.ADMIN.name))
                    GONE else VISIBLE

                doAsync {
                    val participants = groupViewModel.getRealParticipants(conversationId)
                    Log.d(TAG, "participants for conversation ${participants} ");
                    participantsMap.clear()
                    for (item in it) {
                        participants.forEach {
                            if (item.userId == it.userId) {
                                participantsMap[item.userId] = it
                                return@forEach
                            }
                        }
                    }
                    adapter.participantsMap = participantsMap

                    uiThread {
                        val s = search_et.text.toString()
                        if (s.isNotBlank()) {
                            filter(s)
                        } else {
                            adapter.data = u
                        }
                    }
                }
            }
        })

        groupViewModel.getConversationById(conversationId).observe(this, Observer {
            it?.let {
                adapter.setConversation(it)
            }
        })

        self = User(userId = Session.getUserId())
        adapter.self = self
        //  groupViewModel.findSelf().observe(this, Observer {
        //     self = it
        //     adapter.self = it
        /// })

        if (disposable == null) {
            disposable = RxBus.listen(ConversationEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it.type == TYPE_MAKE_ADMIN || it.type == TYPE_REMOVE || it.type == TYPE_EXIT) {
                            dialog?.dismiss()
                        }
                    }
        }

        search_et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                filter(s.toString())
            }
        })

        //jobManager.addJobInBackground(RefreshConversationJob(conversationId))
    }

    private fun filter(s: String) {
        val us = arrayListOf<User>()
        users.forEach {
            if (it.getName().contains(s, true)) {
                us.add(it)
            }
        }
        adapter.data = us
    }

    private fun openChat(user: User) {
        val intent = Intent(requireContext(), ChatRoom::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ChatRoom.show(requireContext(), intent, user.userId)
        requireActivity().finish()
    }

    private fun viewUser(userId: String) {
        UserProfileActivity.show(requireContext(), userId, generateConversationId(Session.getUserId(), userId))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable?.dispose()
        disposable = null
        dialog?.dismiss()
    }

    private fun showConfirmDialog(message: String, type: Int, user: User? = null) {

        AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
                .setMessage(message)
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(R.string.confirm) { dialog, _ ->
                    showPb(R.string.removing)
                    when (type) {
                        TYPE_REMOVE -> {
                            groupViewModel.modifyGroupMembers(conversationId, listOf(user!!), TYPE_REMOVE)
                        }
                        TYPE_EXIT -> {
                            groupViewModel.exitGroup(conversationId)
                        }
                        TYPE_DELETE -> {
                            groupViewModel.deleteMessageByConversationId(conversationId)
                            startActivity(Intent(context, HomeActivity::class.java))
                        }
                    }
                    dialog.dismiss()
                }.show()
    }

    private fun showPb(message: Int?) {
        if (dialog == null) {
            dialog = progressDialog()
        }
        dialog?.addMessage(requireContext(), message)

        dialog?.show(childFragmentManager, "fuck")
    }


    private fun modifyMember(isAdd: Boolean) {
        val list = arrayListOf<User>()
        adapter.data.let {
            list += it!!
        }
        activity?.addFragment(this@GroupInfoFragment,
                GroupFragment.newInstance(if (isAdd) TYPE_ADD else TYPE_REMOVE, list, conversationId), GroupFragment.TAG)
    }
}