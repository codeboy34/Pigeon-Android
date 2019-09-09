package com.pigeonmessenger.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.User

class TestContactsAdapter (private val context:Context)
    : RecyclerView.Adapter<ContactsAdapter.ViewHolder>()
{
    var contacts : List<User> = emptyList()
    set(value) {
        field =value
        notifyDataSetChanged()
    }
    var mContactListener: ContactsAdapter.ContactListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_contact_contact, parent,false)
        return ContactsAdapter.ContactViewHolder(view)
    }

    override fun getItemCount() = contacts.size

    override fun onBindViewHolder(holder: ContactsAdapter.ViewHolder, position: Int) {
       (holder as ContactsAdapter.ContactViewHolder).bind(contacts[position],mContactListener)
    }
}