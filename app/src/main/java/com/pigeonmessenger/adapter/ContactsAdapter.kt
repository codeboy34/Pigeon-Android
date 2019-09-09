package com.pigeonmessenger.adapter

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.widget.inflate
import kotlinx.android.synthetic.main.item_contact_contact.view.*
import kotlinx.android.synthetic.main.item_contact_header.view.*


class ContactsAdapter(val context: Context)
    : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    companion object {
        const val TAG = "ContactsAdapter"
        const val TYPE_HEADER = 0
        const val TYPE_FOOTER = 1
        const val TYPE_FRIEND = 2
        const val TYPE_CONTACT = 3

        const val POS_HEADER = 0
        const val POS_FRIEND = 1
    }

    private var mHeaderView: View? = null
    private var mFooterView: View? = null
    var mContactListener: ContactListener? = null

    private val me: String by lazy {
        Session.getUserId()
    }

    var contacts: List<User> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int {
        return if (mHeaderView == null && mFooterView == null) {
            contacts.size
        } else if (mHeaderView != null && mFooterView == null) {
            contacts.size + 1
        } else if (mHeaderView == null && mFooterView != null) {
            contacts.size + 1
        } else {
            contacts.size + 2
        }
    }

    /*
    override fun getHeaderId(position: Int): Long {
        if (mHeaderView != null && position == POS_HEADER) {
            return -1
        } else if (mFooterView != null && position == itemCount - 1) {
            return -1
        }
        val u = contacts[getPosition(position)]
        return if (u.displayName != null && u.displayName!!.isNotEmpty()) u.displayName!![0].toLong() else -1L
    }*/

    override fun getItemViewType(position: Int): Int {
        if (mHeaderView == null && mFooterView == null) {
            return TYPE_CONTACT
        }
        return when (position) {
            POS_HEADER -> TYPE_HEADER
            itemCount - 1 -> {
                if (mFooterView != null) {
                    return TYPE_FOOTER
                } else {
                    TYPE_CONTACT
                }
            }
            else -> TYPE_CONTACT
        }
    }

    /*
    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val view = parent.inflate(R.layout.item_contact_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
      //  if (friendSize > 0 && position < POS_FRIEND + friendSize && position >= POS_HEADER) {
       //     holder.bind()
       //     return
     //   }
        val contact = contacts[getPosition(position)]
        holder.bind(contact)
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "itemCount${itemCount}: ");
        return if (mHeaderView != null && viewType == TYPE_HEADER) {
            HeadViewHolder(mHeaderView!!)
        } else if (viewType == TYPE_CONTACT) {
            val view = parent.inflate(R.layout.item_contact_contact, false)
            ContactViewHolder(view)
        } else {
            FootViewHolder(mFooterView!!)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "ONBINDVIEW HOLDER: ");
        when (holder) {
            is HeadViewHolder -> {
                holder.bind(mContactListener)
            }
            is FootViewHolder -> {
                holder.bind(mContactListener)
            }
            is ContactViewHolder -> {
                Log.d(TAG, "ContactBIND : ");
                val user: User = contacts[getPosition(position)]
                holder.bind(user, mContactListener)
            }
            else -> {
                Log.d("ContactsAdapter", "WHY I AM AT ELSE: ");
            }
        }
    }

    private fun getPosition(position: Int): Int {
        return if (mHeaderView != null) {
            position - 1
        } else {
            position
        }
    }

    fun setHeader(view: View?) {
        mHeaderView = view
    }

    fun isHeader() = mHeaderView != null

    fun setFooter(view: View) {
        mFooterView = view
    }

    fun removeFooter() {
        mFooterView = null
    }

    fun setContactListener(listener: ContactListener) {
        mContactListener = listener
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.header.text = itemView.context.getString(R.string.contact_item_title)
        }

        fun bind(user: User) {
            itemView.header.text = if (user.displayName != null &&
                    user.displayName!!.isNotEmpty()) user.displayName!![0].toString() else ""
        }
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class HeadViewHolder(itemView: View) : ViewHolder(itemView) {

        fun bind(listener: ContactListener?) {
            if (listener != null) {

            }
        }
    }

    class FootViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(listener: ContactListener?) {
            if (listener != null) {
                // itemView.empty_rl.setOnClickListener { listener.onEmptyRl() }
            }
        }
    }
    /*

             class FriendViewHolder(itemView: View) : ViewHolder(itemView) {
                 fun bind(user: User, listener: ContactListener?) {
                     itemView.normal.text = user.fullName
                     itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
                     //itemView.verified_iv.visibility = if (user.isVerified != null && user.isVerified) VISIBLE else GONE
                 //    if (listener != null) {
                   //      itemView.setOnClickListener { listener.onFriendItem(user) }
                   //  }
                 }
             }*/

    class ContactViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(user: User, listener: ContactListener?) {
            Log.d(TAG, "ContactsViewHolder: bind ");
            itemView.avatar_iv.setUserAvatar(user.userId, user.thumbnail)
            val text = if (user.displayName != null && user.displayName!!.isNotEmpty()) user.displayName else user.userId
            itemView.contact_friend.text = text
            itemView.phone_tv.text = user.userId
            if (listener != null) {
                itemView.setOnClickListener { listener.onContactItem(user) }
            }
        }
    }

    interface ContactListener {
        fun onContactItem(user: User)
    }
}