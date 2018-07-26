package com.hardcopy.btchat

import android.app.Application
import android.content.Context


/**
 * Created by suhyb_wion on 2017-02-03.
 */

class ApplicationClass : Application() {
    companion object {
        lateinit var context: Context

        fun getAppContext(): Context {
            return context
        }
    }

    override fun onCreate() {
        super.onCreate()

        context = this
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}