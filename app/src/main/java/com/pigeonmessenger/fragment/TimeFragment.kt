package com.pigeonmessenger.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.pigeonmessenger.R
import com.pigeonmessenger.activities.SplashActivity
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.extension.putBoolean
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.utils.ErrorHandler
import com.pigeonmessenger.viewmodals.AccountViewModel
import com.pigeonmessenger.widget.shaking
import kotlinx.android.synthetic.main.fragment_time.*
import kotlinx.coroutines.Job

class TimeFragment : BaseFragment() {

    companion object {
        const val TAG: String = "TimeFragment"
        fun newInstance() = TimeFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_time, container, false)


    private val loadingViewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this).get(AccountViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        checkTime()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        continue_tv.setOnClickListener {
            checkTime()
        }
    }

    private var currentJob: Job? = null
    private fun checkTime() {
        if (currentJob == null || currentJob?.isActive == false) {
            everybody_pb.visibility = View.VISIBLE
            continue_tv.visibility = View.INVISIBLE
            currentJob = loadingViewModel.pingServer({
                if (isAdded) {
                    everybody_pb.visibility = View.INVISIBLE
                    continue_tv.visibility = View.VISIBLE
                    defaultSharedPreferences.putBoolean(Constant.Account.PREF_WRONG_TIME, false)
                    SplashActivity.show(requireContext())
                    activity?.finish()
                }
            }, { exception ->
                if (isAdded) {
                    everybody_pb.visibility = View.INVISIBLE
                    continue_tv.visibility = View.VISIBLE
                    if (exception == null) {
                        info.shaking()
                    } else {
                        ErrorHandler.handleError(exception)
                    }
                }
            })
        }
    }
}