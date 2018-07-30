package com.hardcopy.blechat.bluetooth

import android.bluetooth.*
import android.content.Context
import com.hardcopy.blechat.utils.Const
import com.hardcopy.btchat.utils.Logs


/**
 * Created by wion on 2016. 10. 1..
 */

class BleConnector {
    private val TAG = javaClass.simpleName

    private var mBluetoothDevice: BluetoothDevice? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    private var mGattListener: BleGattListener? = null
    private var mWriteCharacteristics = HashMap<String, BluetoothGattCharacteristic>()
    private var mReadCharacteristics = HashMap<String, BluetoothGattCharacteristic>()
    private var mNotiCharacteristic = HashMap<String, BluetoothGattCharacteristic>()

    /**
     * =============================
     * Gatt CallBack
     * =============================
     */
    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Logs.i(TAG, "onConnectionStateChange")
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Logs.i(TAG, "STATE_CONNECTED")
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_CONNECTING -> Logs.i(TAG, "STATE_CONNECTING")
                BluetoothGatt.STATE_DISCONNECTED -> {
                    Logs.i(TAG, "STATE_DISCONNECTED")
                    mGattListener?.onDisconnected()
                }
                BluetoothGatt.STATE_DISCONNECTING -> Logs.i(TAG, "STATE_DISCONNECTING")
                else -> {
                    mGattListener?.onConnectionFail()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Logs.i(TAG, "onServicesDiscovered")
            if (BluetoothGatt.GATT_SUCCESS == status) {
                Logs.i(TAG, "GATT_SUCCESS")
                discoverService(gatt)
            } else {
                Logs.i(TAG, "GATT_FAIL")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Logs.i(TAG, "onCharacteristicWrite")
            if (Const.SERVICE_UUID.equals(characteristic.service.uuid.toString())) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mGattListener?.onWriteSuccess(characteristic)
                } else {
                    mGattListener?.onWriteFailure(characteristic)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Logs.i(TAG, "onCharacteristicChanged")
            mGattListener?.onNotify(characteristic)     // data is in characteristic.value
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Logs.i(TAG, "onCharacteristicRead")
            super.onCharacteristicRead(gatt, characteristic, status)
            if (BluetoothGatt.GATT_SUCCESS == status) {
                mGattListener?.onRead(characteristic)     // data is in characteristic.value
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Logs.i(TAG, "onDescriptorRead")
            super.onDescriptorRead(gatt, descriptor, status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Logs.i(TAG, "onDescriptorWrite=$status")
            //gatt.writeCharacteristic(mWrite)
            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
            Logs.i(TAG, "onReliableWriteCompleted")
            super.onReliableWriteCompleted(gatt, status)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Logs.i(TAG, "onReadRemoteRssi")
            super.onReadRemoteRssi(gatt, rssi, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Logs.i(TAG, "onMtuChanged")
            super.onMtuChanged(gatt, mtu, status)
        }
    }

    /**
     * @brief : Service initialize
     */
    private fun discoverService(gatt: BluetoothGatt) {
        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (isWritableCharacteristic(characteristic)) {
                    mWriteCharacteristics.put(characteristic.uuid.toString(), characteristic)
                }
                if (isReadableCharacteristic(characteristic)) {
                    mReadCharacteristics.put(characteristic.uuid.toString(), characteristic)
                }
                if (isNotificationCharacteristic(characteristic)) {
                    mNotiCharacteristic.put(characteristic.uuid.toString(), characteristic)
                    for (descriptor in characteristic.descriptors) {
                        // do nothing
                    }
                }
            }
        }
        Logs.i(TAG, "Service discovery finished...")
        mGattListener?.onConnected(gatt)
    }

    /**
     * @brief :Connect to BLE device
     * @param device
     */
    fun connect(context: Context, device: BluetoothDevice) {
        Logs.d(TAG, "connect BLE device")
        mBluetoothDevice = device
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback)
        mBluetoothGatt?.connect()
    }

    /**
     * Enable notification
     * @param uuid
     */
    fun enableNotification(uuid: String) {
        if(mNotiCharacteristic.containsKey(uuid)) {
            val notiChar = mNotiCharacteristic[uuid]
            if(notiChar != null) {
                mBluetoothGatt?.setCharacteristicNotification(notiChar, true) ?: return
                for (descriptor in notiChar.descriptors) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    mBluetoothGatt?.writeDescriptor(descriptor)
                }
            }
        }
    }

    /**
     * Set listener
     * @param gattListener      BleGattListener
     */
    fun setConnectionListener(gattListener: BleGattListener) {
        this.mGattListener = gattListener
    }

    /**
     * @brief :BLEGATT Disonnect
     */
    fun disconnect() {
        if (mBluetoothGatt != null) {
            Thread(Runnable {
                try {
                    //Thread.sleep(Const.BLE_TIME_INTERVAL_BEFORE_DISCONNECT)
                    mBluetoothGatt?.disconnect()
                    Thread.sleep(Const.BLE_TIME_INTERVAL_AFTER_DISCONNECT)
                    mBluetoothGatt?.close()
                    mBluetoothGatt = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }).start()
        }
    }

    /**
     * Disconnect
     * @return
     */
    fun isConnected(): Boolean {
        return if (mBluetoothDevice != null && mBluetoothGatt != null)
            mBluetoothGatt?.getConnectionState(mBluetoothDevice) == BluetoothGatt.STATE_CONNECTED
        else
            false
    }

    /**
     * @brief : BLE Data Send
     * @param data
     * @param listener
     */
    fun sendData(uuid: String, data: ByteArray) {
        if (data.isEmpty())
            return
        if (isConnected()) {
            if(mWriteCharacteristics.containsKey(uuid)) {
                val writeChar = mWriteCharacteristics[uuid]
                if(writeChar != null) {
                    writeChar.value = data
                    mBluetoothGatt?.writeCharacteristic(writeChar)
                }
            }
        } else {
            mGattListener?.onConnectionFail()
        }
    }

    /**
     * @brief BLE Data Send
     */
    fun sendData(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        characteristic.value = data
        mBluetoothGatt?.writeCharacteristic(characteristic)
    }

    /**
     * @brief Read request
     */
    fun readData(uuid: String) {
        if (isConnected()) {
            if(mWriteCharacteristics.containsKey(uuid)) {
                val readChar = mWriteCharacteristics[uuid]
                if(readChar != null) {
                    mBluetoothGatt?.readCharacteristic(readChar)
                }
            }
        } else {
            mGattListener?.onConnectionFail()
        }
    }

    /**
     * @brief Read request
     */
    fun readData(characteristic: BluetoothGattCharacteristic) {
        mBluetoothGatt?.readCharacteristic(characteristic)
    }

    /**
     * Writable characteristic 인지 확인
     *
     * @param chr
     * @return
     */
    fun isWritableCharacteristic(chr: BluetoothGattCharacteristic?): Boolean {
        if (chr == null) return false

        val charaProp = chr.properties
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE or (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
            Logs.d("# Found writable characteristic")
            return true
        } else {
            Logs.d("# Not writable characteristic")
            return false
        }
    }

    /**
     * Readable characteristic 인지 확인
     *
     * @param chr
     * @return
     */
    fun isReadableCharacteristic(chr: BluetoothGattCharacteristic?): Boolean {
        if (chr == null) return false

        val charaProp = chr.properties
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
            Logs.d("# Found readable characteristic")
            return true
        } else {
            Logs.d("# Not readable characteristic")
            return false
        }
    }

    /**
     * Notification characteristic 인지 확인
     * @param chr
     * @return
     */
    fun isNotificationCharacteristic(chr: BluetoothGattCharacteristic?): Boolean {
        if (chr == null) return false

        val charaProp = chr.properties
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            Logs.d("# Found notification characteristic")
            return true
        } else {
            Logs.d("# Not notification characteristic")
            return false
        }
    }

}
