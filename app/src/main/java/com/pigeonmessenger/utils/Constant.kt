package com.pigeonmessenger.utils

object Constant {

    val HOST = "pigeon.net.in"
    val PORT = "8000"
    val PROTOCOL = "http://"
    val URL = "$PROTOCOL$HOST:$PORT"


    const val SLEEP_MILLIS: Long = 1000
    const val INTERVAL_24_HOURS: Long = 1000 * 60 * 60 * 24
    const val INTERVAL_48_HOURS: Long = 1000 * 60 * 60 * 48
    const val INTERVAL_10_MINS: Long = 1000 * 60 * 10

    const val BATCH_SIZE = 100
    const val ARGS_USER = "args_user"



    object Account{
        const val PREF_SESSION = "pref_session"
        const val PREF_PIN_TOKEN = "pref_pin_token"
        const val PREF_NAME_ACCOUNT = "pref_name_account"
        const val PREF_REG_PHONE_NUMBER  ="pref_reg_phone_number"
        const val PREF_NAME_TOKEN = "pref_name_token"
        const val PREF_PIN_CHECK = "pref_pin_check"
        const val PREF_PIN_INTERVAL = "pref_pin_interval"
        const val PREF_PIN_ITERATOR = "pref_pin_iterator"
        const val PREF_CAMERA_TIP = "pref_camera_tip"
        const val PREF_LOGOUT_COMPLETE = "pref_logout_complete"
        const val PREF_BIOMETRICS = "pref_biometrics"
        const val PREF_WRONG_TIME = "pref_wrong_time"
        const val PREF_RESTORE = "pref_restore"
    }

    const val PAGE_SIZE = 20

    object Settings{

        //settings
        const val PREF_KEY_SEEN_STATUS = "seen_status"
        const val PREF_KEY_PROFILE_SEEN = "profile_seen"

        const val NOBODY = 0
        const val EVERYONE= 1
        const val CONTACTS = 2

    }

    object DataBase {
        const val DB_NAME = "pigeon.db"
        const val MINI_VERSION = 15
        const val CURRENT_VERSION = 20
    }

    object BackUp {
        const val BACKUP_PERIOD = "backup_period"
        const val BACKUP_LAST_TIME = "backup_last_time"
    }


    const val MUTE_8_HOURS :Long = 8 * 60 * 60
    const val MUTE_1_WEEK :Long= 7 * 24 * 60 * 60
    const val MUTE_1_YEAR :Long= 365 * 24 * 60 * 60
    const val MUTE_FOREVER :Long= 365 * 4 * 24 * 60 * 60

}
