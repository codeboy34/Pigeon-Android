package com.pigeonmessenger.utils.backup

import androidx.lifecycle.LiveData
import com.pigeonmessenger.activities.App
import org.jetbrains.anko.runOnUiThread

class BackupLiveData : LiveData<Boolean>() {
    var ing: Boolean = false
        private set(value) {
            App.get().runOnUiThread {
                if (field != value) {
                    field = value
                    setValue(value)
                }
            }
        }
    var result: Result? = null
        private set

    fun setResult(ing: Boolean, result: Result?) {
        this.result = result
        this.ing = ing
    }

    fun start() {
        setResult(true, null)
    }
}
