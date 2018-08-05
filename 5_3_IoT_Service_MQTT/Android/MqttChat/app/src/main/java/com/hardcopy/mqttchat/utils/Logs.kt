package com.hardcopy.mqttchat.utils

import android.util.Log


object Logs {

    private val TAG = "MQTT Chat"
    var mIsEnabled = true

    @JvmStatic
    fun v(msg: String) {
        if (mIsEnabled) {
            Log.v(TAG, msg)
        }
    }

    @JvmStatic
    fun v(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.v(tag, msg)
        }
    }

    @JvmStatic
    fun d(msg: String) {
        if (mIsEnabled) {
            Log.d(TAG, msg)
        }
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.d(tag, msg)
        }
    }

    @JvmStatic
    fun e(msg: String) {
        if (mIsEnabled) {
            Log.e(TAG, msg)
        }
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.e(tag, msg)
        }
    }

    @JvmStatic
    fun i(msg: String) {
        if (mIsEnabled) {
            Log.e(TAG, msg)
        }
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.e(tag, msg)
        }
    }
}