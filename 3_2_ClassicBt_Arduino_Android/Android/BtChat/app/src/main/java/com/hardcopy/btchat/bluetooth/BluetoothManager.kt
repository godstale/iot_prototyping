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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
class BluetoothManager {
    companion object {
        // Debugging
        private const val TAG = "BluetoothManager"

        // Constants that indicate the current connection state
        const val STATE_NONE = 0       // we're doing nothing
        const val STATE_LISTEN = 1     // now listening for incoming connections
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3  // now connected to a remote device

        // Message types sent from the BluetoothManager to Handler
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_TOAST = 5

        // Data field of scan result
        const val MSG_DEVICE_NAME = "device_name"
        const val MSG_DEVICE_ADDRESS = "device_address"

        // Name for the SDP record when creating server socket
        private const val NAME = "BluetoothManager"

        // Unique UUID for this application
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private const val RECONNECT_DELAY_MAX = (60 * 60 * 1000).toLong()

        private var mInstance: BluetoothManager = BluetoothManager()
        fun getInstance(): BluetoothManager {
            return mInstance
        }
    }

    // Member fields
    private val mAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mHandler: Handler? = null

    private var mReconnectDelay: Long = (15 * 1000).toLong()
    private var mConnectTimer: Timer? = null
    private var mIsServiceStopped: Boolean = false

    /**
     * Return the current connection state.  */
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    // Give the new state to the Handler so the UI Activity can update
    var state: Int = STATE_NONE
        set(new_state) {
            Log.d(TAG, "setState() $field -> $new_state")
            field = new_state

            if (new_state == STATE_CONNECTED)
                cancelRetryConnect()
            mHandler?.obtainMessage(MESSAGE_STATE_CHANGE, new_state, -1)?.sendToTarget()
        }

    init {
        state = STATE_NONE
    }


    fun getAdapter(): BluetoothAdapter {
        return mAdapter
    }

    fun setHandler(handler: Handler) {
        mHandler = handler
    }

    fun isBluetoothEnabled(): Boolean {
        return mAdapter.isEnabled
    }

    fun getScanMode(): Int {
        return mAdapter.scanMode
    }

    fun startDiscovery() {
        mAdapter.startDiscovery()
    }

    fun cancelDiscovery() {
        mAdapter.cancelDiscovery()
    }

    fun isDiscovering(): Boolean {
        return mAdapter.isDiscovering
    }

    fun getBondedDevices(): Set<BluetoothDevice> {
        return mAdapter.bondedDevices
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()  */
    @Synchronized
    fun start() {
        Log.d(TAG, "Starting BluetoothManager...")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = AcceptThread()
            mAcceptThread?.start()
        }
        state = STATE_LISTEN
        mIsServiceStopped = false
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param address  The BluetoothDevice address to connect
     */
    fun connect(address: String?) {
        address ?: return

        connect(mAdapter.getRemoteDevice(address))
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    @Synchronized
    fun connect(device: BluetoothDevice?) {
        Log.d(TAG, "Connecting to: $device")
        device ?: return

        if (state == STATE_CONNECTED)
            return

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device)
        mConnectThread?.start()
        state = STATE_CONNECTING
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket, device: BluetoothDevice) {
        Log.d(TAG, "connected")

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread?.start()

        // Change state
        state = STATE_CONNECTED

        // Send the name of the connected device back to the UI Activity
        val msg = mHandler?.obtainMessage(MESSAGE_DEVICE_NAME)
        if(msg != null && device.address != null && device.name != null) {
            val bundle = Bundle()
            bundle.putString(MSG_DEVICE_ADDRESS, device.address)
            bundle.putString(MSG_DEVICE_NAME, device.name)
            msg.data = bundle
            mHandler?.sendMessage(msg)
        }
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }
        state = STATE_NONE

        mIsServiceStopped = true
        cancelRetryConnect()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray) {
        // Create temporary object
        var r: ConnectedThread? = null
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (state != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r?.write(out)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        Log.d(TAG, "BluetoothManager :: connectionFailed()")
        state = STATE_LISTEN

        // Reserve re-connect timer
        reserveRetryConnect()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        Log.d(TAG, "BluetoothManager :: connectionLost()")
        state = STATE_LISTEN

        // Reserve re-connect timer
        reserveRetryConnect()
    }

    /**
     * Automatically retry bluetooth connection.
     */
    private fun reserveRetryConnect() {
        if (mIsServiceStopped)
            return

        mReconnectDelay *= 2
        if (mReconnectDelay > RECONNECT_DELAY_MAX)
            mReconnectDelay = RECONNECT_DELAY_MAX

        if (mConnectTimer != null) {
            try {
                mConnectTimer!!.cancel()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        }
        mConnectTimer = Timer()
        mConnectTimer!!.schedule(ConnectTimerTask(), mReconnectDelay)
    }

    private fun cancelRetryConnect() {
        if (mConnectTimer != null) {
            try {
                mConnectTimer!!.cancel()
                mConnectTimer!!.purge()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

            mConnectTimer = null
            mReconnectDelay = (15 * 1000).toLong()
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private inner class AcceptThread : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "listen() failed" + e.toString())
            }

            mmServerSocket = tmp
        }

        override fun run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this)
            var socket: BluetoothSocket? = null

            // Listen to the server socket if we're not connected
            while (this@BluetoothManager.state != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    if (mmServerSocket != null) {
                        this@BluetoothManager.state = STATE_CONNECTING
                        socket = mmServerSocket.accept()
                    }
                } catch (e: IOException) {
                    this@BluetoothManager.state = STATE_NONE
                    Log.e(TAG, "accept() failed", e)
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    when (this@BluetoothManager.state) {
                        STATE_LISTEN, STATE_CONNECTING ->
                            // Situation normal. Start the connected thread.
                            connected(socket, socket!!.remoteDevice)
                        STATE_NONE, STATE_CONNECTED ->
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket!!.close()
                            } catch (e: IOException) {
                                Log.e(TAG, "Could not close unwanted socket", e)
                            }

                        else -> {
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread")
        }

        fun cancel() {
            Log.d(TAG, "cancel " + this)
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of server failed" + e.toString())
            }
            this@BluetoothManager.state = STATE_NONE
        }
    }    // End of class AcceptThread


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?

        init {
            var tmp: BluetoothSocket? = null

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "create() failed", e)
            }

            mmSocket = tmp
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread")
            name = "ConnectThread"

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
            } catch (e: IOException) {
                connectionFailed()
                // Close the socket
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2)
                }

                // Start the service over to restart listening mode
                this@BluetoothManager.start()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this@BluetoothManager) {
                mConnectThread = null
            }

            // Start the connected thread
            connected(mmSocket, mmDevice)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }

        }
    }    // End of class ConnectThread

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            Log.d(TAG, "create ConnectedThread")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    val buffer = ByteArray(128)
                    Arrays.fill(buffer, 0x00.toByte())
                    bytes = mmInStream!!.read(buffer)

                    // Send the obtained bytes to the main thread
                    mHandler?.obtainMessage(MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }

            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        fun write(buffer: ByteArray) {
            try {
                mmOutStream!!.write(buffer)

                // Disabled: Share the sent message back to the main thread
                // mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                //        .sendToTarget();
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write")
            }

        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed")
            }

        }

    }    // End of class ConnectedThread

    /**
     * Auto connect timer
     */
    private inner class ConnectTimerTask : TimerTask() {
        override fun run() {
            if (mIsServiceStopped)
                return

            mHandler?.post {
                if (state == STATE_CONNECTED || state == STATE_CONNECTING)
                    return@post

                Log.d(TAG, "ConnectTimerTask :: Retry connect()")

                val addrs = ConnectionInfo.deviceAddress
                val ba = BluetoothAdapter.getDefaultAdapter()
                if (ba != null && addrs != null) {
                    val device = ba.getRemoteDevice(addrs)

                    if (device != null) {
                        connect(device)
                    }
                }
            }
        }
    }

}
