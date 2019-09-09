package com.pigeonmessenger.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.pigeonmessenger.R
import com.pigeonmessenger.fragment.FileSharedFragment
import com.pigeonmessenger.fragment.PhotosSharedFragment
import com.pigeonmessenger.fragment.VideoSharedFragment
import kotlinx.android.synthetic.main.activity_shared_media.*


class SharedMediaActivity : AppCompatActivity() {

    private val conversationId: String by lazy {
        intent.getStringExtra(CONVERSATION_ID)
    }

    private val TAG = "SharedMediaActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_media)

        Log.d(TAG, "CONVERSATIONiD ${intent.getStringExtra(CONVERSATION_ID)}: ");

        viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> PhotosSharedFragment.getInstance(intent.getStringExtra(CONVERSATION_ID))
                    1 -> VideoSharedFragment()
                    2 -> FileSharedFragment()
                    else -> throw Exception("Out of bound")
                }
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when (position) {
                    0 -> getString(R.string.Photos)
                    1 -> getString(R.string.Videos)
                    2 -> getString(R.string.Files)
                    else -> null
                }
            }

            override fun getCount() = 3

        }

        tabLayout.setupWithViewPager(viewPager)
        viewPager.currentItem = 0
    }


    companion object {
        private const val CONVERSATION_ID = "conversationId"

        fun show(context: Context,conversationId: String){
            val intent = Intent(context,SharedMediaActivity::class.java)
            intent.putExtra(CONVERSATION_ID,conversationId)
            context.startActivity(intent)
        }
    }
}
