package com.pigeonmessenger.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.notNullElse
import com.pigeonmessenger.widget.inflate
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_contact_header.view.*
import kotlinx.android.synthetic.main.item_group_friend.view.*


class GroupFriendAdapter : RecyclerView.Adapter<GroupFriendAdapter.FriendViewHolder>(),
    StickyRecyclerHeadersAdapter<GroupFriendAdapter.HeaderViewHolder> {

    private var data: List<User>? = null
    private var mShowHeader: Boolean = false
    private var mListener: GroupFriendListener? = null
    private val mCheckedMap: HashMap<String, Boolean> = HashMap()

    fun setData(data: List<User>?, showHeader: Boolean) {
        this.data = data
        //mShowHeader = showHeader
        //data?.filterNot { mCheckedMap.containsKey(it.identityNumber) }
          //  ?.forEach { mCheckedMap[it.identityNumber] = false }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = notNullElse(data, { it.size }, 0)

    override fun getHeaderId(position: Int): Long {
        if (!mShowHeader) {
            return -1
        }
        return notNullElse(data, {
            val u = it[position]
            if (u.getName() != null) {
                if (u.getName().isEmpty()) ' '.toLong() else u.getName()[0].toLong()
            } else {
                -1L
            }
        }, -1L)
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (data == null || data!!.isEmpty()) {
            return
        }
        val user = data!![position]
        holder.bind(user)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val view = parent.inflate(R.layout.item_contact_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        if (data == null || data!!.isEmpty()) {
            return
        }
        val user = data!![position]
        holder.bind(user, mListener, mCheckedMap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder =
        FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_friend, parent, false))

    fun setGroupFriendListener(listener: GroupFriendListener) {
        mListener = listener
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            user: User,
            listener: GroupFriendListener?,
            checkedMap: HashMap<String, Boolean>) {
            itemView.name.text = user.getName()

            itemView.avatar.setUserAvatar(user.userId,user.thumbnail)
            if (checkedMap.containsKey(user.userId)) {
                itemView.check_iv.visibility = if (checkedMap[user.userId]!!) VISIBLE else INVISIBLE
            }

            itemView.setOnClickListener {
                itemView.check_iv.visibility = if (itemView.check_iv.isVisible) INVISIBLE else VISIBLE
                checkedMap[user.userId] = itemView.check_iv.isVisible
                listener?.onItemClick(user, itemView.check_iv.isVisible)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.header.text = if (user.getName() != null && user.getName().isNotEmpty())
                user.getName()[0].toString() else ""
        }
    }

    interface GroupFriendListener {
        fun onItemClick(user: User, checked: Boolean)
    }
}