package com.pigeonmessenger.fragment


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.database.room.daos.MessageDao
import com.pigeonmessenger.extension.loadImageMark
import com.pigeonmessenger.vo.MediaMinimal
import com.pigeonmessenger.vo.MessageCategory
import com.pigeonmessenger.widget.gallery.internal.ui.widget.MediaGridInset
import kotlinx.android.synthetic.main.fragment_photos_shared.*
import kotlinx.android.synthetic.main.shared_photo_item.view.*
import javax.inject.Inject

class PhotosSharedFragment : Fragment() {

    companion object {
        private const val TAG = "PhotosSharedFragment"
        private const val CONVERSATION_ID = "conversation_id"

        fun getInstance(conversationId: String):PhotosSharedFragment {
            return PhotosSharedFragment().apply {
                val bundle = Bundle()
                bundle.putString(CONVERSATION_ID, conversationId)
                this.arguments = bundle
            }
        }
    }

    @Inject
    lateinit var messagesDao: MessageDao


    private val conversationId: String by lazy {
        arguments!!.getString(CONVERSATION_ID)
    }

    private val adapter: PhotosAdapter by lazy {
        PhotosAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_photos_shared, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "conversationsdfjkl jflkdsjf $conversationId: ");
        App.get().appComponent.inject(this)

        photos_rv.layoutManager = GridLayoutManager(requireContext(), 3)
        val spacing = resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
        photos_rv.addItemDecoration(MediaGridInset(3, spacing, false))
        photos_rv.adapter = adapter

        messagesDao.getSharedMedia(conversationId, MessageCategory.SIGNAL_IMAGE.name).observe(this, Observer {
            adapter.photoList = it
        })
    }

    class PhotosAdapter : RecyclerView.Adapter<PhotoViewHolder>() {
        var photoList: List<MediaMinimal> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.shared_item_view, parent, false)
            return PhotoViewHolder(view)
        }

        override fun getItemCount(): Int = photoList.size

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            holder.bind(photoList[position])
        }
    }

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(mediaMinimal: MediaMinimal) {
            itemView.image_view.loadImageMark(mediaMinimal.mediaUrl,mediaMinimal.mediaThumbnail,-1)
        }
    }


}
