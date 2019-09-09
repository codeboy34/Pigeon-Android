package com.pigeonmessenger.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.extension.loadOwnerAvatar
import com.pigeonmessenger.fragment.ConversationListFragment
import com.pigeonmessenger.fragment.SearchFragment
import com.pigeonmessenger.services.NetworkService
import com.pigeonmessenger.widget.MaterialSearchView
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.android.synthetic.main.activity_home.*

import kotlinx.android.synthetic.main.view_search.*



class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        startConnectionMonitorService()
        initView()
        EmojiManager.install(GoogleEmojiProvider())
        navigateToMessage()
        renderAvatarIcon()
    }

    private fun renderAvatarIcon() {
        Session.liveAccount.observe(this, Observer {
            avatar_iv.loadOwnerAvatar(it.avatar,it.thumbnail)
        })
    }

    private fun startConnectionMonitorService() {
        startService(Intent(this, NetworkService::class.java))
    }


    private fun initView() {
        search_bar.setOnLeftClickListener(View.OnClickListener {
            startActivity(Intent(this, MeActivity::class.java))
        })

        search_bar.setOnRightClickListener(View.OnClickListener {
            startActivity(Intent(this@HomeActivity, ContactsActivity::class.java))
        })

        search_bar.setOnBackClickListener(View.OnClickListener {
            hideSearch()
            search_bar.closeSearch()
        })

        search_bar.mOnQueryTextListener = object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                SearchFragment.getInstance().setQueryText(newText)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                SearchFragment.getInstance().setQueryText(query)
                return true
            }
        }

        search_bar.setSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewClosed() {
                hideSearch()
            }

            override fun onSearchViewOpened() {
                showSearch()
            }
        })
        root_view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && search_bar.isOpen) {
                search_bar.closeSearch()
                true
            } else {
                false
            }
        }
    }

    override fun onBackPressed() {
        if (search_bar.isOpen) {
            hideSearch()
            search_bar.closeSearch()
        } else {
            super.onBackPressed()
        }
    }


    fun showSearch() {
        val searchFragment = SearchFragment.getInstance()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(R.id.frag_container, searchFragment)
                .commitAllowingStateLoss()
    }

    fun hideSearch() {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .remove(SearchFragment.getInstance())
                .commitAllowingStateLoss()
    }

    private fun navigateToMessage() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.frag_container,  ConversationListFragment.newInstance())
                .commitAllowingStateLoss()
    }


}
