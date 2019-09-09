package com.pigeonmessenger.fragment

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.ChatRoom
import com.pigeonmessenger.activities.HomeActivity
import com.pigeonmessenger.activities.MeActivity.Companion.MAX_PHOTO_SIZE
import com.pigeonmessenger.activities.SetupAccountActivity
import com.pigeonmessenger.database.room.entities.ConversationStatus
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.viewmodals.GroupViewModel
import com.pigeonmessenger.vo.toUser
import com.pigeonmessenger.widget.PbDialog
import com.pigeonmessenger.widget.hideKeyboard
import com.pigeonmessenger.widget.showKeyboard
import com.tbruyelle.rxpermissions2.RxPermissions
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_group.view.*
import kotlinx.android.synthetic.main.fragment_new_group.*
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import java.util.*

class NewGroupFragment : Fragment() {
    companion object {
        const val TAG = "NewGroupFragment"
        private const val ARGS_USERS = "args_users"

        fun newInstance(users: ArrayList<User>): NewGroupFragment {
            val fragment = NewGroupFragment()
            fragment.apply {

            }
            fragment.withArgs {
                putParcelableArrayList(ARGS_USERS, users)
            }
            return fragment
        }
    }

    private val conversationId by lazy { "group-${UUID.randomUUID()}" }

    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProviders.of(this).get(GroupViewModel::class.java)
    }
    private val sender: User by lazy { Session.getAccount()!!.toUser() }

    private val imageUri: Uri by lazy {
        Uri.fromFile(context?.getGroupAvatarPath(conversationId))
    }
    private var resultUri: Uri? = null
    private val adapter = NewGroupAdapter()
    private var dialog: Dialog? = null
    private var progressDialog: PbDialog? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? =
            inflater.inflate(R.layout.fragment_new_group, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val users: List<User> = arguments!!.getParcelableArrayList(ARGS_USERS)!!
        left_ib.setOnClickListener {
            name_desc_et.hideKeyboard()
            activity?.onBackPressed()
        }

        right_animator.displayedChild = 1

        right_animator.setOnClickListener {
            createGroup()
        }

        photo_rl.setGroup(conversationId, null)
        enableCreate(false)
        photo_rl.setOnClickListener {
            RxPermissions(activity!!)
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (granted) {
                            activity?.openImage(imageUri)
                        } else {
                            context?.openPermissionSetting()
                        }
                    }
        }

        Log.d(TAG, ":$users ");
        adapter.users = users
        user_rv.layoutManager = LinearLayoutManager(requireContext())
        user_rv.adapter = adapter
        //  user_rv.addItemDecoration(SpaceItemDecoration())
        name_desc_et.addTextChangedListener(mWatcher)
        name_desc_et.showKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE) {
            var selectedImageUri: Uri?
            if (data == null || data.action != null &&
                    data.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE) {
                selectedImageUri = imageUri
            } else {
                selectedImageUri = data.data
                if (selectedImageUri == null) {
                    selectedImageUri = imageUri
                }
            }
            val options = UCrop.Options()
            options.setToolbarColor(ContextCompat.getColor(context!!, R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(context!!, R.color.black))
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri, imageUri)
                    .withOptions(options)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
                    .start(activity!!)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                resultUri = UCrop.getOutput(data)
                photo_rl.setGroup(resultUri)
                Log.d(TAG, "ResultUri $resultUri: ");
                //new_group_avatar.loadCircleImage(resultUri.toString(), R.drawable.ic_photo_camera)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                context?.toast("crop failed")
            }
        }
    }

    private fun createGroup() {
        var thumbnail: String? = null
        var iconUrl: String? = null
        var groupIcon: String? = null

        if (resultUri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, resultUri)
            val imageFile = resultUri!!.toFile()
            val size = Size(SetupAccountActivity.MAX_PHOTO_SIZE, SetupAccountActivity.MAX_PHOTO_SIZE)
            thumbnail = imageFile.blurThumbnail(size)?.bitmap2String()
            groupIcon = Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP)
            iconUrl = resultUri.toString()
        }

        val conversation = groupViewModel.createGroupConversation(conversationId, name_desc_et.text.toString(),
                notice_desc_et.text.toString(), thumbnail, groupIcon, iconUrl, adapter.users!!, sender)

        val liveData = groupViewModel.getConversationStatusById(conversation.conversationId)
        liveData.observe(this, Observer { c ->
            if (c != null) {
                when {
                    c.status == ConversationStatus.START.ordinal -> {

                        if (progressDialog == null) {
                            progressDialog = requireContext().progressDialog()
                            progressDialog?.addMessage(requireContext(), R.string.creating)
                            progressDialog?.show(childFragmentManager, "")
                        }
                    }
                    c.status == ConversationStatus.SUCCESS.ordinal -> {
                        liveData.removeObservers(this)
                        name_desc_et.hideKeyboard()
                        dialog?.dismiss()
                        ChatRoom.show(context!!, conversation.conversationId, null)
                        requireActivity().finish()
                    }
                    c.status == ConversationStatus.FAILURE.ordinal -> {
                        name_desc_et.hideKeyboard()
                        dialog?.dismiss()
                        startActivity(Intent(context, HomeActivity::class.java))
                        requireActivity().finish()
                    }
                }
            }
        })
    }

    private fun enableCreate(enable: Boolean) {
        if (enable) {
            title_view.right_tv.textColor = ContextCompat.getColor(requireContext(), R.color.pigeonActionColor)
            title_view.right_animator.isEnabled = true
        } else {
            title_view.right_tv.textColor = ContextCompat.getColor(requireContext(), R.color.text_gray)
            title_view.right_animator.isEnabled = false
        }
    }

    class NewGroupAdapter : RecyclerView.Adapter<ItemHolder>() {
        var users: List<User>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
                ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_normal, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (users == null || users!!.isEmpty()) {
                return
            }
            holder.bind(users!![position])
        }

        override fun getItemCount(): Int = notNullElse(users, { it.size }, 0)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.avatar.setUserAvatar(user.userId,user.thumbnail)
            itemView.normal.text = user.getName()
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrEmpty()) {
                enableCreate(true)
            } else {
                enableCreate(false)
            }
        }
    }
}