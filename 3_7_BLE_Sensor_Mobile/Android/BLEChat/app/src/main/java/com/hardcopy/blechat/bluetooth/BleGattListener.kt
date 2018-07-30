package com.hardcopy.blechat.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

interface BleGattListener {
    fun onConnected(gatt: BluetoothGatt)
    fun onConnectionFail()
    fun onDisconnected()

    fun onWriteSuccess(characteristic: BluetoothGattCharacteristic)
    fun onWriteFailure(characteristic: BluetoothGattCharacteristic)

    fun onRead(characteristic: BluetoothGattCharacteristic)
    fun onNotify(characteristic: BluetoothGattCharacteristic)
}