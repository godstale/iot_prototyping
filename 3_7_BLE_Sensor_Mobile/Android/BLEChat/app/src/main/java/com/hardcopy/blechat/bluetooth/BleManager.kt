package com.hardcopy.blechat.bluetooth

import android.bluetooth.BluetoothAdapter
import android.os.Build

object BleManager {
    // Debugging
    private const val TAG = "BleManager"

    // Member fields
    private val mBtAdapter = BluetoothAdapter.getDefaultAdapter()


    init {
    }


    fun isBluetoothEnabled(): Boolean {
        return mBtAdapter.isEnabled
    }

    fun getAdapter(): BluetoothAdapter {
        return mBtAdapter
    }


}
