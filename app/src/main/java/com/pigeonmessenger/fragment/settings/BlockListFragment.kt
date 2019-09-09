package com.pigeonmessenger.fragment.settings


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.Relationship
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.addMessage
import com.pigeonmessenger.extension.progressDialog
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.ContactsViewModal
import com.pigeonmessenger.widget.PbDialog
import com.pigeonmessenger.widget.inflate
import com.pigeonmessenger.widget.sweetalert.SweetAlertDialog
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_block_list.*
import kotlinx.android.synthetic.main.item_block_user.view.*
import kotlinx.android.synthetic.main.view_title.view.*

class BlockListFragment : Fragment() {
    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    private var pbDialog: PbDialog? = null

    private val contactsViewModel: ContactsViewModal by lazy {
        ViewModelProviders.of(this).get(ContactsViewModal::class.java)
    }

    private val blockListAdapter: BlockListAdapter by lazy {
        BlockListAdapter()
    }

    companion object {
        const val TAG = "BlockListFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_block_list, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        block_rv.layoutManager = LinearLayoutManager(requireContext())
        titleView.back_iv.setOnClickListener { activity?.onBackPressed() }
        block_rv.adapter = blockListAdapter
        blockListAdapter.blockUserClickListener = object : BlockUserClickListener {
            override fun onUserClick(userId: String) {
                showUnblock(userId)
            }
        }
        contactsViewModel.blockList().observe(this, Observer {
            Log.d(TAG, "$it")
            if (it.isNullOrEmpty()) {
                empty_tv.visibility = VISIBLE
            } else {
                empty_tv.visibility = GONE
                blockListAdapter.blockList = it
            }
        })
    }


    private fun showUnblock(userId: String) {
        SweetAlertDialog(requireContext())
                .setTitleText(getString(R.string.dialog_unblock_title))
                .setContentText(getString(R.string.sure_unblock))
                .setConfirmText(getString(R.string.unblock).toUpperCase())
                .setCancelText(getString(R.string.cancel).toUpperCase())
                .setConfirmClickListener {
                    unblock(userId)
                    it.dismissWithAnimation()
                }.setCancelClickListener {
                    it.dismissWithAnimation()
                }
                .show()
    }

    private fun unblock(userId: String) {
        if (pbDialog == null) pbDialog = progressDialog()

        pbDialog?.addMessage(requireContext(), R.string.unblocking)
        pbDialog?.show(childFragmentManager, "")
        contactsViewModel.unblock(userId).autoDisposable(scopeProvider).subscribe({
            if (it.isSuccessful) contactsViewModel.updateBlockRelationship(userId, Relationship.STRANGE.name)
            else ErrorHandler.handleCode(it.code())
            pbDialog?.dismiss()
        }, {
            pbDialog?.dismiss()
            ErrorHandler.handleError(it)
        })
    }


    class BlockListAdapter : RecyclerView.Adapter<BlockUserItem>() {
        var blockUserClickListener: BlockUserClickListener? = null;

        var blockList: List<User> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockUserItem =
                BlockUserItem(parent.inflate(R.layout.item_block_user, false))

        override fun getItemCount() = blockList.size

        override fun onBindViewHolder(holder: BlockUserItem, position: Int) {
            holder.bind(blockList[position])
            holder.itemView.setOnClickListener {
                blockUserClickListener?.onUserClick(blockList[position].userId)
            }
        }
    }

    class BlockUserItem(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(user: User) {
            itemView.name_tv.text = user.getName()
        }
    }

    interface BlockUserClickListener {
        fun onUserClick(userId: String)
    }
}
