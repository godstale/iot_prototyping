package com.hardcopy.blechat.bluetooth

import android.bluetooth.BluetoothDevice

/**
 * BLE device 발견시 호출되는 Listener 인터페이스
 */

interface BleScanListener {
    fun onDeviceFound(device: BluetoothDevice, beacon: Beacon?)
    fun onScanFailed(errorCode: Int)
    fun onScanDone()
}