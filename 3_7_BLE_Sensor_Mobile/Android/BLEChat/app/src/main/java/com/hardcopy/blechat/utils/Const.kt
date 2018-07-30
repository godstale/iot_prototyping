package com.hardcopy.blechat.utils

import java.util.*

object Const {
    // Intent request codes
    val REQUEST_CONNECT_DEVICE = 1
    val REQUEST_ENABLE_BT = 2
    val REQUEST_DISCOVERABLE = 3

    // 비컨 Unique UUID
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * BLE Scanning
     */
    val BLE_SCANNING_TIME = 5 * 1000         // 스캔 지속 시간 (under Android v5.0)
    val BLE_RSSI_UPDATE_INTERVAL_IN_MILLIS = 3 * 1000     // RSSI update queue processing interval

    val BLE_SCAN_FILTERING_MASK = "4d6fc88b-be75-6698-da48-6866a36ec78e".toLowerCase()

    /**
     * BLE connection
     */
    val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    val READ_CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26fa".toLowerCase()
    val WRITE_CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8".toLowerCase()

    val BLE_TIME_INTERVAL_AFTER_DISCONNECT = 250L
    val BLE_TIME_INTERVAL_BEFORE_DISCONNECT = 100L

    /**
     * Beacon broadcasting
     */
    val ADVERTISING_TIMEOUT = 10 * 1000  // 10 sec
    val IBEACON_UUID = "01020304-0506-0708-0910-111213141516"
    val IBEACON_MAJOR = 101
    val IBEACON_MINOR_CONFIRM = 101
    val IBEACON_MINOR_CANCEL = 102
    val IBEACON_TX_POWER = 0xc5

    // 180A Device Information
    val SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb"
    val CHAR_MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb"
    val CHAR_MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb"
    val CHAR_SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb"

    // 1802 Immediate Alert
    val SERVICE_IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb"
    val CHAR_ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb"
}