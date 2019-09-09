package com.pigeonmessenger.fragment.settings

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.database.room.entities.ConversationCategory
import com.pigeonmessenger.extension.fileSize
import com.pigeonmessenger.extension.indeterminateProgressDialog
import com.pigeonmessenger.extension.notNullElse
import com.pigeonmessenger.extension.toast
import com.pigeonmessenger.viewmodals.SettingStorageViewModel
import com.pigeonmessenger.vo.ConversationStorageUsage
import com.pigeonmessenger.vo.StorageUsage
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_storage.*
import kotlinx.android.synthetic.main.item_storage_check.view.*
import timber.log.Timber

class SettingStorageFragment : Fragment() {

    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }


    companion object {
        const val TAG = "SettingStorageFragment"

        fun newInstance(): SettingStorageFragment {
            return SettingStorageFragment()
        }
    }

    private val settingStorageViewModel: SettingStorageViewModel by lazy {
        ViewModelProviders.of(this).get(SettingStorageViewModel::class.java)
    }

    private val adapter = StorageAdapter {
        showMenu(it)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_storage, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b_rv.layoutManager = LinearLayoutManager(requireContext())
        b_rv.adapter = adapter
        menuView.adapter = menuAdapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingStorageViewModel.getConversationStorageUsage().observe(this, Observer {
            if (progress.visibility != View.GONE) {
                progress.visibility = View.GONE
            }
            adapter.setData(it)
        })
    }

    private val dialog: Dialog by lazy {
        indeterminateProgressDialog(message = R.string.pb_dialog_message,
                title = R.string.group_adding).apply {
            setCancelable(false)
        }
    }

    private val selectSet: HashSet<StorageUsage> = HashSet()

    private fun showMenu(conversationId: String) {

    }

    private val menuDialog: AlertDialog by lazy {
        AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
                .setView(menuView)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.setting_storage_bn_clear) { dialog, _ ->
                    var sum = 0L
                    var size = 0L
                    selectSet.forEach { sum += it.count; size += it.mediaSize }
                    confirmDialog.setMessage(getString(R.string.setting_storage_clear, sum, size.fileSize()))
                    confirmDialog.show()
                    dialog.dismiss()
                }.create().apply {
                    setOnShowListener {
                        val states = arrayOf(
                                intArrayOf(android.R.attr.state_enabled),
                                intArrayOf(-android.R.attr.state_enabled))
                        val colors = intArrayOf(Color.RED, Color.GRAY)
                        getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ColorStateList(states, colors))
                    }
                    this.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                }
    }

    private val confirmDialog: AlertDialog by lazy {
        AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    clear()
                    dialog.dismiss()
                }.create().apply {
                    setOnShowListener {
                        getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED)
                    }
                }
    }

    private fun clear() {
        dialog.show()
        Observable.just(selectSet)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .map {
                    for (item in selectSet) {
                        settingStorageViewModel.clear(item.conversationId, item.type)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(scopeProvider)
                .subscribe({
                    dialog.dismiss()
                }, {
                    Timber.e(it)
                    dialog.dismiss()
                    toast(R.string.error_unknown)
                })
    }

    private val menuView: RecyclerView by lazy {
        View.inflate(requireContext(), R.layout.view_stotage_list, null) as RecyclerView
    }
    private val menuAdapter: MenuAdapter by lazy {
        MenuAdapter(object : (Boolean, StorageUsage) -> Unit {
            override fun invoke(checked: Boolean, storageUsage: StorageUsage) {
                if (checked) {
                    selectSet.add(storageUsage)
                } else {
                    selectSet.remove(storageUsage)
                }
                menuDialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = selectSet.size > 0
            }
        })
    }

    class MenuAdapter(private val checkAction: (Boolean, StorageUsage) -> Unit) : RecyclerView.Adapter<CheckHolder>() {
        private var storageUsageList: List<StorageUsage>? = null

        fun setData(users: List<StorageUsage>?) {
            this.storageUsageList = users
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckHolder {
            return CheckHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_storage_check, parent, false), checkAction)
        }

        override fun getItemCount(): Int = notNullElse(storageUsageList, { it.size }, 0)

        override fun onBindViewHolder(holder: CheckHolder, position: Int) {
            storageUsageList?.let {
                holder.bind(it[position])
            }
        }
    }

    class StorageAdapter(val action: ((String) -> Unit)) : RecyclerView.Adapter<ItemHolder>() {

        private var conversationStorageUsageList: List<ConversationStorageUsage>? = null

        fun setData(users: List<ConversationStorageUsage>?) {
            this.conversationStorageUsageList = users
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_storage, parent, false))
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (conversationStorageUsageList == null || conversationStorageUsageList!!.isEmpty()) {
                return
            }
            holder.bind(conversationStorageUsageList!![position], action)
        }

        override fun getItemCount(): Int = notNullElse(conversationStorageUsageList, { it.size }, 0)
    }

    class CheckHolder(itemView: View, private val checkAction: (Boolean, StorageUsage) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(storageUsage: StorageUsage) {
            itemView.check_view.setName(when {
                storageUsage.type.endsWith("_IMAGE") -> R.string.conversation_status_pic
                storageUsage.type.endsWith("_DATA") -> R.string.conversation_status_file
                storageUsage.type.endsWith("_VIDEO") -> R.string.conversation_status_video
                storageUsage.type.endsWith("_AUDIO") -> R.string.conversation_status_audio
                else -> R.string.conversation_status_unknown
            })
            itemView.check_view.setSize(storageUsage.mediaSize)
            itemView.check_view.isChecked = true
            itemView.check_view.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, checked ->
                checkAction(checked, storageUsage)
            })
        }
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(conversationStorageUsage: ConversationStorageUsage, action: ((String) -> Unit)) {
            if (conversationStorageUsage.category == ConversationCategory.GROUP.name) {
                // itemView.avatar.setGroup(conversationStorageUsage.groupIconUrl)
                //  itemView.normal.text = conversationStorageUsage.groupName
            } else {
                //  itemView.normal.text = conversationStorageUsage.name
                // itemView.avatar.setInfo(conversationStorageUsage.name, conversationStorageUsage.avatarUrl, conversationStorageUsage.ownerIdentityNumber)
            }
            //itemView.storage_tv.text = conversationStorageUsage.mediaSize.fileSize()
            itemView.setOnClickListener { action(conversationStorageUsage.conversationId) }
        }
    }
}