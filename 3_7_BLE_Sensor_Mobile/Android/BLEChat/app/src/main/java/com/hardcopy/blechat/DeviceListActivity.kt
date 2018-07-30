package com.hardcopy.blechat

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.hardcopy.blechat.bluetooth.Beacon
import com.hardcopy.blechat.bluetooth.BleScanListener
import com.hardcopy.blechat.bluetooth.BleScanner
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

        // Return Intent extra
        var EXTRA_DEVICE_ADDRESS = "device_address"
        var EXTRA_DEVICE_OBJECT = "device_object"
    }

    // Member fields
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

        BleScanner.setScanListener(mScanListener)

        initialize()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        BleScanner.scheduleLeScan(false, 0)
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
        val pairedDevices = BleScanner.getAdapter().bondedDevices

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            text_paired_devices.visibility = View.VISIBLE
            for (device in pairedDevices) {
                if(device.type == BluetoothDevice.DEVICE_TYPE_LE
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
            mNewDevicesArrayAdapter.clear()
            scan()
            showScanButton(false)
        }
    }

    private fun showScanButton(isShow: Boolean) {
        runOnUiThread {
            if(isShow)
                btn_scan.visibility = View.VISIBLE
            else
                btn_scan.visibility = View.GONE
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun scan() {
        Logs.d(TAG, "# Scan BLE devices...")

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true)
        setTitle(resources.getString(R.string.scanning))

        // Turn on sub-title for new devices
        text_new_devices.visibility = View.VISIBLE

        // Clear cache
        mDeviceList.clear()

        // If we're already discovering, stop it
        BleScanner.scheduleLeScan(true, 10*1000)
    }

    // The on-click listener for all devices in the ListViews
    private val mDeviceClickListener = AdapterView.OnItemClickListener { av, v, arg2, arg3 ->
        // Cancel discovery because it's costly and we're about to connect
        BleScanner.scheduleLeScan(false, 0)

        // Get the device MAC address, which is the last 17 chars in the View
        val info = (v as TextView).text.toString()
        if (info.length > 16) {
            val address = info.substring(info.length - 17)
            Log.d(TAG, "User selected device : $address")

            // Create the result Intent and include the MAC address
            val intent = Intent()
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address)

            for(cached_device in mDeviceList) {
                if(cached_device.address.equals(address, true)) {
                    intent.putExtra(EXTRA_DEVICE_OBJECT, cached_device)
                    break
                }
            }

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private val mScanListener = object: BleScanListener {
        override fun onDeviceFound(device: BluetoothDevice, beacon: Beacon?) {
            if(device.bondState != BluetoothDevice.BOND_BONDED) {
                for(cached_device in mDeviceList) {
                    if(cached_device.address.equals(device.address))
                        return
                }
                Logs.d("# Device found... ${device.name}, ${device.address}, state=${device.bondState}")
                val sb = StringBuilder()
                if(beacon != null) {
                    if(device.name != null && device.name.isNotEmpty())
                        sb.append(device.name).append("\n")
                    if(beacon.proximityUUID.isNotEmpty())
                        sb.append(beacon.proximityUUID).append("\n")
                    sb.append(device.address)
                } else {
                    if(device.name != null && device.name.isNotEmpty())
                        sb.append(device.name).append("\n")
                    else
                        sb.append("No name\n")
                    sb.append(device.address)
                }
                mNewDevicesArrayAdapter.add(sb.toString())
                mDeviceList.add(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            showScanButton(true)
        }

        override fun onScanDone() {
            showScanButton(true)
        }
    }
}
