package com.pigeonmessenger.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import com.pigeonmessenger.R
import com.pigeonmessenger.extension.defaultSharedPreferences
import com.pigeonmessenger.extension.fileSize
import com.pigeonmessenger.extension.openPermissionSetting
import com.pigeonmessenger.extension.putBoolean
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.utils.backup.BackupNotification
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import com.pigeonmessenger.utils.backup.Result
import com.pigeonmessenger.utils.backup.restore
import  com.pigeonmessenger.utils.backup.findBackup
import kotlinx.android.synthetic.main.activity_restore.*

class RestoreActivity : BaseActivity() {

    @Inject
    lateinit var jobManager: PigeonJobManager

    @SuppressLint("MissingPermission", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.get().appComponent.inject(this)
        defaultSharedPreferences.putBoolean(Constant.Account.PREF_RESTORE, true)

        RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (!granted) {
                        openPermissionSetting()
                    } else {
                        findBackup()
                    }
                }, {
                    defaultSharedPreferences.putBoolean(Constant.Account.PREF_RESTORE, false)
                    SplashActivity.show(this)
                    finish()
                })
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun findBackup() {
        GlobalScope.launch {
            val file = findBackup(this@RestoreActivity, coroutineContext)
            withContext(Dispatchers.Main) {
                if (file == null) {
                    defaultSharedPreferences.putBoolean(Constant.Account.PREF_RESTORE, false)
                    SplashActivity.show(this@RestoreActivity)
                    finish()
                   // showErrorAlert(Result.NOT_FOUND)
                } else {
                    initUI(file)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun showErrorAlert(error: Result) {
        AlertDialog.Builder(this, R.style.MixinAlertDialogTheme)
            .setMessage(when (error) {
                Result.FAILURE -> {
                    R.string.restore_failure
                }
                Result.NOT_FOUND -> {
                    R.string.restore_not_found
                }
                else -> {
                    R.string.restore_not_support
                }
            })
            .setNegativeButton(R.string.restore_retry) { dialog, _ ->
                findBackup()
                dialog.dismiss()
            }
            .setPositiveButton(R.string.restore_skip) { dialog, _ ->
                dialog.dismiss()
                defaultSharedPreferences.putBoolean(Constant.Account.PREF_RESTORE, false)
               SplashActivity.show(this)
            }.create().run {
                this.setCanceledOnTouchOutside(false)
                this.show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun initUI(data: File) {
        setContentView(R.layout.activity_restore)
        restore_time.text = data.lastModified().run {
            val now = Date().time
            val createTime = data.lastModified()
            DateUtils.getRelativeTimeSpanString(createTime, now, when {
                ((now - createTime) < 60000L) -> DateUtils.SECOND_IN_MILLIS
                ((now - createTime) < 3600000L) -> DateUtils.MINUTE_IN_MILLIS
                ((now - createTime) < 86400000L) -> DateUtils.HOUR_IN_MILLIS
                else -> DateUtils.DAY_IN_MILLIS
            })
        }
        restore_restore.setOnClickListener {
            RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (!granted) {
                        openPermissionSetting()
                    } else {
                        showProgress()
                        BackupNotification.show(false)
                        restore(this) { result ->
                            BackupNotification.cancel()
                            if (result == Result.SUCCESS) {
                                SplashActivity.show(this)
                                defaultSharedPreferences.putBoolean(Constant.Account.PREF_RESTORE, false)
                                finish()
                            } else {
                                hideProgress()
                            }
                        }
                    }
                }, {
                    BackupNotification.cancel()
                    hideProgress()
                })
        }
        restore_size.text = getString(R.string.restore_size, data.length().fileSize())
        restore_skip.setOnClickListener {
            SplashActivity.show(this)
            defaultSharedPreferences.putBoolean(Constant.Account.PREF_RESTORE, false)
            finish()
        }
    }

    private fun showProgress() {
        restore_progress.visibility = View.VISIBLE
        restore_restore.visibility = View.GONE
        restore_skip.visibility = View.GONE
    }

    private fun hideProgress() {
        restore_progress.visibility = View.GONE
        restore_restore.visibility = View.VISIBLE
        restore_skip.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
    }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, RestoreActivity::class.java))
        }
    }
}