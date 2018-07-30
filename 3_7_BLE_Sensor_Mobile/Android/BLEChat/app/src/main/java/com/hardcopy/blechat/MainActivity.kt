package com.hardcopy.blechat

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.*
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.hardcopy.blechat.R.layout.activity_main
import com.hardcopy.blechat.bluetooth.BleConnector
import com.hardcopy.blechat.bluetooth.BleGattListener
import com.hardcopy.blechat.bluetooth.BleManager
import com.hardcopy.blechat.utils.Const
import com.hardcopy.btchat.utils.Logs
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = "MainActivity"
    }

    private var mBleConnector: BleConnector? = null

    private val mCharacteristicList = ArrayList<BluetoothGattCharacteristic>()
    private var mDefaultWriteChar: BluetoothGattCharacteristic? = null
    private var mDefaultReadChar: BluetoothGattCharacteristic? = null

    private var mPrevUpdateTime = 0L
    private var mIsConnected = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_main)

        Logs.d(TAG, "# MainActivity - onCreate()")

        if(!checkBluetooth()) {
            btn_scan.isEnabled = false
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, Const.REQUEST_ENABLE_BT)
        } else {
            btn_scan.isEnabled = true
        }
        
        initialize()
    }

    override fun onBackPressed() {
        // Exit dialog
        val alertDiag = AlertDialog.Builder(this)
        alertDiag.setMessage("Exit app?")
        alertDiag.setPositiveButton("OK") { _: DialogInterface, _: Int ->
            // finish app
            finalizeActivity()
            finishApp()
        }
        alertDiag.setNegativeButton("Cancel") { _: DialogInterface, _: Int -> }

        val alert = alertDiag.create()
        alert.setTitle("Warning")
        alert.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        finalizeActivity()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        finalizeActivity()
    }



    private fun initialize() {
        // initialize UI
        setConnected(false)

        btn_scan.setOnClickListener {
            if(mIsConnected)
                disconnect()
            else
                doScan()
        }
        btn_beacon.setOnClickListener {
            startBeacon()
        }
        btn_send.setOnClickListener {
            val text = edit_chat.text.toString()
            if(text.isNotEmpty()) {
                if(text.startsWith("@")) {
                    val commands = text.split(" ", ignoreCase = false, limit = 2)
                    if(commands[0] == "@read") {
                        if(commands.size > 1) {
                            val index = commands[1].toIntOrNull()
                            if(index != null && mCharacteristicList.size > index) {
                                mDefaultReadChar = mCharacteristicList[index]
                                mBleConnector?.readData(mDefaultReadChar!!)
                                addText("@ set default read char [$index]\n")
                            } else {
                                addText("@ command error")
                            }
                        } else if(mDefaultReadChar != null) {
                            mBleConnector?.readData(mDefaultReadChar!!)
                        }
                    } else if(commands[0] == "@write") {
                        if(commands.size > 1) {
                            val index = commands[1].toIntOrNull()
                            if(index != null && mCharacteristicList.size > index) {
                                mDefaultWriteChar = mCharacteristicList[index]
                                addText("@ set default write char [$index]")
                            } else {
                                addText("@ command error")
                            }
                        } else {
                            addText("@ command error")
                        }
                    } else if(commands[0] == "@noti") {
                        if(commands.size > 1) {
                            val index = commands[1].toIntOrNull()
                            if(index != null && mCharacteristicList.size > index) {
                                val notiChar = mCharacteristicList[index]
                                mBleConnector?.enableNotification(notiChar.uuid.toString())
                                addText("@ enable notification of char num = $index")
                            } else {
                                addText("@ command error")
                            }
                        } else {
                            addText("@ command error")
                        }
                    }
                } else {
                    if(mDefaultWriteChar != null) {
                        mBleConnector?.sendData(mDefaultWriteChar!!, edit_chat.text.toString().toByteArray())
                    }
                }
            }
            edit_chat.text.clear()
        }
    }

    private fun finalizeActivity() {
        Logs.d(TAG, "# Activity - finalizeActivity()")

        // Close bluetooth connection
        disconnect()
    }

    private fun finishApp() {
        ActivityCompat.finishAffinity(this);
        System.runFinalizersOnExit(true);
        System.exit(0);
    }

    private fun checkBluetooth(): Boolean {
        return BleManager.isBluetoothEnabled()
    }

    /**
     * Launch the DeviceListActivity to see devices and do scan
     */
    private fun doScan() {
        val intent = Intent(this, DeviceListActivity::class.java)
        startActivityForResult(intent, Const.REQUEST_CONNECT_DEVICE)
    }

    /**
     * Ensure this device is discoverable by others
     */
    private fun startBeacon() {
        // TODO:
    }

    private fun setConnected(connected: Boolean) {
        mIsConnected = connected

        runOnUiThread {
            if(connected) {
                text_status.text = resources.getString(R.string.bt_state_connected)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_online, null))
                } else {
                    img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_online))
                }

                btn_send.isEnabled = true
                btn_scan.text = resources.getText(R.string.disconnect)
                btn_beacon.isEnabled = false
            } else {
                text_status.text = resources.getString(R.string.bt_state_init)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible, null))
                } else {
                    img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible))
                }

                btn_send.isEnabled = false
                btn_scan.text = resources.getText(R.string.button_scan)
                btn_beacon.isEnabled = true
            }
        }
    }

    private fun addText(text: String) {
        runOnUiThread {
            val currentTime = System.currentTimeMillis()
            if(mPrevUpdateTime + 1000 < currentTime) {
                text_chat.append("\n")
                mPrevUpdateTime = currentTime
            }
            if(text_chat.text.length > 10000) {
                text_chat.text = ""
            }
            text_chat.append(text)
            scroll_chat.post { scroll_chat.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun connect(device: BluetoothDevice) {
        mBleConnector = BleConnector()
        mBleConnector?.setConnectionListener(mBleGattListener)
        mBleConnector?.connect(this, device)
    }

    private fun disconnect() {
        mBleConnector?.disconnect()
        mCharacteristicList.clear()
    }

    /**
     * Receives result from external activity
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Logs.d(TAG, "onActivityResult $resultCode")

        when (requestCode) {
            Const.REQUEST_CONNECT_DEVICE -> {
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    val address = data?.extras?.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
                    val device = data?.extras?.getParcelable<BluetoothDevice>(DeviceListActivity.EXTRA_DEVICE_OBJECT)
                    // Attempt to connect to the device
                    if(device != null) {
                        connect(device)
                        addText("connecting to $address")
                    }
                }
            }
            Const.REQUEST_ENABLE_BT -> {
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a BT session
                    btn_scan.isEnabled = true
                } else {
                    // User did not enable Bluetooth or an error occured
                    Logs.e(TAG, "BT is not enabled")
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show()
                    btn_scan.isEnabled = false
                }
            }
            Const.REQUEST_DISCOVERABLE -> {
                // resultCode is always false
            }
        }    // End of switch(requestCode)
    }

    private val mBleGattListener = object: BleGattListener {
        // BLE 연결 후 service discovery 까지 완료
        override fun onConnected(gatt: BluetoothGatt) {
            mCharacteristicList.clear()
            val sb = StringBuilder()
            var i: Int = 0
            for (service in gatt.services) {
                sb.append("# Service: ").append(service.uuid.toString()).append("\n")
                for (characteristic in service.characteristics) {
                    sb.append("#   Characteristic[").append(i)
                            .append("]: ").append(characteristic.uuid.toString()).append("\n      ")
                    if (mBleConnector!!.isWritableCharacteristic(characteristic)) {
                        sb.append("write, ")
                    }
                    if (mBleConnector!!.isReadableCharacteristic(characteristic)) {
                        sb.append("read, ")
                    }
                    if (mBleConnector!!.isNotificationCharacteristic(characteristic)) {
                        sb.append("notify, ")
                        for (descriptor in characteristic.descriptors) {
                            sb.append("descriptor = ").append(descriptor.uuid).append(", ")
                        }
                    }
                    sb.append("\n")
                    i++
                    mCharacteristicList.add(characteristic)
                }
            }
            sb.append("\n\n")

            addText(sb.toString())
            setConnected(true)

            // 자동으로 notify 활성화가 필요한 경우 여기서 작업
            // mBleConnector?.enableNotification(NOTIFY_CHARACTERISTIC_UUID)
        }

        override fun onConnectionFail() {
            setConnected(false)
            addText(resources.getString(R.string.bt_state_connect_failed))
        }

        override fun onDisconnected() {
            setConnected(false)
            addText(resources.getString(R.string.bt_state_disconnected))
            mCharacteristicList.clear()
        }

        override fun onWriteSuccess(characteristic: BluetoothGattCharacteristic) {}

        override fun onWriteFailure(characteristic: BluetoothGattCharacteristic) {
            addText(resources.getString(R.string.bt_cmd_sending_error))
        }

        override fun onRead(characteristic: BluetoothGattCharacteristic) {
            addText(characteristic.value.toString(Charset.defaultCharset()))
        }

        override fun onNotify(characteristic: BluetoothGattCharacteristic) {
            addText(characteristic.value.toString(Charset.defaultCharset()))
        }

    }
}
