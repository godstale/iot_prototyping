/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hardcopy.btchat.bluetooth

import android.content.Context
import com.hardcopy.btchat.ApplicationClass
import com.hardcopy.btchat.utils.Const

/**
 * Remember connection informations for future use
 */
object ConnectionInfo {
    // Device MAC address
    var deviceAddress: String? = null
    // Name of the connected device
    var deviceName: String? = null
        set(name) {
            val context = ApplicationClass.getAppContext()
            val prefs = context?.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE) ?: return
            val editor = prefs.edit()
            editor.putString(Const.PREFERENCE_CONN_INFO_ADDRESS, deviceAddress)
            editor.putString(Const.PREFERENCE_CONN_INFO_NAME, name)
            editor.commit()
            field = name
        }

    init {
        val context = ApplicationClass.getAppContext()
        val prefs = context?.getSharedPreferences(Const.PREFERENCE_NAME, Context.MODE_PRIVATE)
        deviceAddress = prefs?.getString(Const.PREFERENCE_CONN_INFO_ADDRESS, null)
        deviceName = prefs?.getString(Const.PREFERENCE_CONN_INFO_NAME, null)
    }

    /**
     * Reset connection info
     */
    fun resetConnectionInfo() {
        deviceAddress = null
        deviceName = null
    }

}
