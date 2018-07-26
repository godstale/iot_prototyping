package com.hardcopy.btchat

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.hardcopy.btchat.bluetooth.BluetoothManager
import com.hardcopy.btchat.utils.Logs
import kotlinx.android.synthetic.main.activity_device_list.*


/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
class DeviceListActivity : Activity() {
    companion object {
        // Debugging
        private val TAG = "DeviceListActivity"
        private val D = true

        // Return Intent extra
        var EXTRA_DEVICE_ADDRESS = "device_address"
    }

    // Member fields
    private val mBluetoothManager: BluetoothManager = BluetoothManager.getInstance()
    private lateinit var mPairedDevicesArrayAdapter: ArrayAdapter<String>
    private lateinit var mNewDevicesArrayAdapter: ArrayAdapter<String>
    private val mDeviceList = ArrayList<BluetoothDevice>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the window
        setContentView(R.layout.activity_device_list)

        // Set result CANCELED in case the user backs out
        val intent = Intent()
        setResult(Activity.RESULT_CANCELED, intent)

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        initialize()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        mBluetoothManager.cancelDiscovery()

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver)
    }

    private fun initialize() {
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = ArrayAdapter(this, R.layout.adapter_device_name)
        mNewDevicesArrayAdapter = ArrayAdapter(this, R.layout.adapter_device_name)

        // Find and set up the ListView for paired devices
        list_paired_devices.adapter = mPairedDevicesArrayAdapter
        list_paired_devices.onItemClickListener = mDeviceClickListener

        // Find and set up the ListView for newly discovered devices
        list_new_devices.adapter = mNewDevicesArrayAdapter
        list_new_devices.onItemClickListener = mDeviceClickListener

        // Get a set of currently paired devices
        val pairedDevices = mBluetoothManager.getBondedDevices()

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            text_paired_devices.visibility = View.VISIBLE
            for (device in pairedDevices) {
                if(device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC
                        || device.type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                    mPairedDevicesArrayAdapter.add(device.name + "\n" + device.address)
                }
            }
        } else {
            val noDevices = resources.getText(R.string.none_paired).toString()
            mPairedDevicesArrayAdapter.add(noDevices)
        }

        // Initialize the button to perform device discovery
        btn_scan.setOnClickListener { v ->
            doDiscovery()
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
        Logs.d(TAG, "doDiscovery()")

        // Update view
        showScanButton(false)
        mNewDevicesArrayAdapter.clear()
        // Clear cache
        mDeviceList.clear()

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true)
        setTitle(R.string.scanning)

        // Turn on sub-title for new devices
        text_new_devices.visibility = View.VISIBLE

        // If we're already discovering, stop it
        if (mBluetoothManager.isDiscovering()) {
            mBluetoothManager.cancelDiscovery()
        }

        // Request discover from BluetoothAdapter
        mBluetoothManager.startDiscovery()
    }

    private fun showScanButton(isShow: Boolean) {
        runOnUiThread {
            if(isShow)
                btn_scan.visibility = View.VISIBLE
            else
                btn_scan.visibility = View.GONE
        }
    }

    // The on-click listener for all devices in the ListViews
    private val mDeviceClickListener = AdapterView.OnItemClickListener { av, v, arg2, arg3 ->
        // Cancel discovery because it's costly and we're about to connect
        mBluetoothManager.cancelDiscovery()

        // Get the device MAC address, which is the last 17 chars in the View
        val info = (v as TextView).text.toString()
        if (info.length > 16) {
            val address = info.substring(info.length - 17)
            Log.d(TAG, "User selected device : $address")

            // Create the result Intent and include the MAC address
            val intent = Intent()
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address)

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    for(cached_device in mDeviceList) {
                        if(cached_device.address.equals(device.address))
                            return
                    }
                    Logs.d("# Device found... ${device.name}, ${device.address}, state=${device.bondState}")
                    mNewDevicesArrayAdapter.add(device.name + "\n" + device.address)
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                setProgressBarIndeterminateVisibility(false)
                setTitle(R.string.select_device)
                if (mNewDevicesArrayAdapter.count == 0) {
                    val noDevices = resources.getText(R.string.none_found).toString()
                    mNewDevicesArrayAdapter.add(noDevices)
                }
                showScanButton(true)
            }
        }
    }
}
