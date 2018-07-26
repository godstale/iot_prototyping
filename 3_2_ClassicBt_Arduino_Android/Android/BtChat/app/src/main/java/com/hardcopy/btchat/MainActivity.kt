package com.hardcopy.btchat

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.hardcopy.btchat.R.layout.activity_main
import com.hardcopy.btchat.bluetooth.BluetoothManager
import com.hardcopy.btchat.utils.Const
import com.hardcopy.btchat.utils.Logs
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset


class MainActivity : Activity() {
    companion object {
        val TAG = "MainActivity"
    }
    private val mBtHandler = BluetoothHandler()
    private val mBluetoothManager: BluetoothManager = BluetoothManager.getInstance()
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
        mBluetoothManager.setHandler(mBtHandler)

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        this.registerReceiver(mReceiver, filter)

        initialize()
    }

    override fun onBackPressed() {
        // Exit dialog
        val alertDiag = AlertDialog.Builder(this)
        alertDiag.setMessage("Exit app?")
        alertDiag.setPositiveButton("OK") { _: DialogInterface, _: Int ->
            // finish app
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible, null))
        } else {
            img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible))
        }
        text_status.text = resources.getString(R.string.bt_state_init)

        btn_scan.setOnClickListener {
            if(mIsConnected)
                disconnect()
            else
                doScan()
        }
        btn_send.setOnClickListener {
            if(edit_chat.text.isNotEmpty()) {
                mBluetoothManager.write(edit_chat.text.toString().toByteArray())
            }
            edit_chat.text.clear()
        }
        btn_discover.setOnClickListener {
            ensureDiscoverable()
        }
    }

    private fun finalizeActivity() {
        Logs.d(TAG, "# Activity - finalizeActivity()")

        // Close bluetooth connection
        mBluetoothManager.stop()
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver)
    }

    private fun finishApp() {
        ActivityCompat.finishAffinity(this);
        System.runFinalizersOnExit(true);
        System.exit(0);
    }

    private fun checkBluetooth(): Boolean {
        return mBluetoothManager.isBluetoothEnabled()
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
    private fun ensureDiscoverable() {
        if (mBluetoothManager.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30)
            startActivityForResult(intent, Const.REQUEST_DISCOVERABLE)

            text_chat.append("\n")
            text_chat.append(resources.getText(R.string.discoverable_desc))
        } else {
            text_chat.append("\n")
            text_chat.append(resources.getText(R.string.discoverable_fail_desc))
        }
    }

    private fun setConnected(connected: Boolean) {
        mIsConnected = connected
        if(connected) {
            btn_send.isEnabled = true
            btn_scan.text = resources.getText(R.string.disconnect)
        } else {
            btn_send.isEnabled = false
            btn_scan.text = resources.getText(R.string.button_scan)
        }
    }

    private fun disconnect() {
        mBluetoothManager.stop()
    }



    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED == action) {
                val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                val prevMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1)
                when(scanMode) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> {
                        btn_scan.isEnabled = false
                        btn_discover.isEnabled = false
                        text_chat.append("\nSCAN_MODE_CONNECTABLE_DISCOVERABLE")
                        text_chat.append("\nMake server socket")

                        mBluetoothManager.start()
                    }
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> {
                        btn_scan.isEnabled = true
                        btn_discover.isEnabled = true
                        text_chat.append("\nSCAN_MODE_CONNECTABLE")
                    }
                    BluetoothAdapter.SCAN_MODE_NONE -> {
                        // Bluetooth is not enabled
                        btn_scan.isEnabled = false
                        btn_discover.isEnabled = false
                        text_chat.append("\nBluetooth is not enabled!!")
                    }
                }
            }
        }
    }

    inner class BluetoothHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BluetoothManager.MESSAGE_READ -> {
                    if (msg.obj != null) {
                        val currentTime = System.currentTimeMillis()
                        if(mPrevUpdateTime + 1000 < currentTime) {
                            text_chat.append("\n")
                            mPrevUpdateTime = currentTime
                        }
                        if(text_chat.text.length > 10000) {
                            text_chat.text = ""
                        }
                        text_chat.append((msg.obj as ByteArray).toString(Charset.defaultCharset()))
                    }
                }
                BluetoothManager.MESSAGE_STATE_CHANGE -> {
                    when(msg.arg1) {
                        BluetoothManager.STATE_NONE -> {    // we're doing nothing
                            text_status.text = resources.getString(R.string.bt_title) + ": " + resources.getString(R.string.bt_state_init)
                            img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible))
                            setConnected(false)
                        }
                        BluetoothManager.STATE_LISTEN -> {  // now listening for incoming connections
                            text_status.text = resources.getString(R.string.bt_title) + ": " + resources.getString(R.string.bt_state_wait)
                            img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible))
                            setConnected(false)
                        }
                        BluetoothManager.STATE_CONNECTING -> {  // connecting to remote
                            text_status.text = resources.getString(R.string.bt_title) + ": " + resources.getString(R.string.bt_state_connect)
                            img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_away))
                        }
                        BluetoothManager.STATE_CONNECTED -> {   // now connected to a remote device
                            text_status.text = resources.getString(R.string.bt_state_connected)
                            img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_online))
                            setConnected(true)
                        }
                    }
                }
                BluetoothManager.MESSAGE_DEVICE_NAME -> {
                    if(msg.data != null) {
                        val deviceName = msg.data.getString(BluetoothManager.MSG_DEVICE_NAME)
                        val deviceAddr = msg.data.getString(BluetoothManager.MSG_DEVICE_ADDRESS)
                        text_status.append(" to ")
                        text_status.append(deviceName)
                        text_status.append(", ")
                        text_status.append(deviceAddr)
                    }
                }
            }

            super.handleMessage(msg)
        }
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
                    // Attempt to connect to the device
                    mBluetoothManager.connect(address)
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
}
