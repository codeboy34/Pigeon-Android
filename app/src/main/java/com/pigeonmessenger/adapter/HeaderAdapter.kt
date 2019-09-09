package com.pigeonmessenger.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.pigeonmessenger.extension.notNullElse

abstract class HeaderAdapter<T> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1
    }

    var headerView: View? = null
    open var data: List<T>? = null

    var onItemListener: OnItemListener? = null

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && headerView != null) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int = notNullElse(data, {
        if (headerView != null) it.size + 1 else it.size }, 0)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            getHeaderViewHolder()
        } else {
            getNormalViewHolder(parent.context, parent)
        }
    }

    protected fun getPos(position: Int): Int {
        return if (headerView != null) {
            position - 1
        } else {
            position
        }
    }

    open fun getHeaderViewHolder() = HeadHolder(headerView!!)
    abstract fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder

    open class NormalHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    open class HeadHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface OnItemListener {
        fun <T> onNormalItemClick(item: T)
    }
}