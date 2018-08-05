package com.hardcopy.mqttchat.utils

import android.content.Context

object Settings {

    @Synchronized
    fun getClientId(c: Context): String {
        val prefs = c.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Const.PREFERENCE_KEY_SET_CLIENT_ID, "")
    }

    @Synchronized
    fun setClientId(c: Context, clientId: String ) {
        val prefs = c.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(Const.PREFERENCE_KEY_SET_CLIENT_ID, clientId)
        editor.apply()
    }

    @Synchronized
    fun getServer(c: Context): String {
        val prefs = c.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Const.PREFERENCE_KEY_SET_SERVER, "")
    }

    @Synchronized
    fun setServer(c: Context, server: String) {
        val prefs = c.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(Const.PREFERENCE_KEY_SET_SERVER, server)
        editor.apply()
    }

    @Synchronized
    fun getPort(c: Context): String {
        val prefs = c.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Const.PREFERENCE_KEY_SET_PORT, "1883")
    }

    @Synchronized
    fun setPort(c: Context, port: String) {
        val prefs = c.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(Const.PREFERENCE_KEY_SET_PORT, port)
        editor.apply()
    }
}