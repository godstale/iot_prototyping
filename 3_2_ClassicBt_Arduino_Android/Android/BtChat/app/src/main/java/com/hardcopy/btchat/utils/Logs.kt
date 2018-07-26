package com.hardcopy.btchat.utils

import android.util.Log

object Logs {

    private val TAG = "BTC Template"
    var mIsEnabled = true


    fun v(msg: String) {
        if (mIsEnabled) {
            Log.v(TAG, msg)
        }
    }

    fun v(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.v(tag, msg)
        }
    }

    fun d(msg: String) {
        if (mIsEnabled) {
            Log.d(TAG, msg)
        }
    }

    fun d(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.d(tag, msg)
        }
    }

    fun e(msg: String) {
        if (mIsEnabled) {
            Log.e(TAG, msg)
        }
    }

    fun e(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.e(tag, msg)
        }
    }

    fun i(msg: String) {
        if (mIsEnabled) {
            Log.e(TAG, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (mIsEnabled) {
            Log.e(tag, msg)
        }
    }

}