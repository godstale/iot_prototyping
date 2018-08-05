package com.hardcopy.mqttchat

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.hardcopy.mqttchat.R.layout.activity_main
import com.hardcopy.mqttchat.dialog.DialogConnectBtnClickListener
import com.hardcopy.mqttchat.dialog.DialogConnectInfo
import com.hardcopy.mqttchat.manager.IMqttManagerListener
import com.hardcopy.mqttchat.manager.MqttManager
import com.hardcopy.mqttchat.mqtt.Connection
import com.hardcopy.mqttchat.mqtt.Connections
import com.hardcopy.mqttchat.utils.Const
import com.hardcopy.mqttchat.utils.Logs
import com.hardcopy.mqttchat.utils.Settings
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    private val TAG = "MainActivity"
    private lateinit var mMqttManager : MqttManager

    private var mIsConnected = false
    private var mClientHandle : String? = null
    private var mMode = MqttMode.MODE_PUBLISH
    private lateinit var mImm : InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_main)

        Logs.d(TAG, "# MainActivity - onCreate()")

        // Register for broadcasts when a device is discovered

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

    private fun finalizeActivity() {
        Logs.d(TAG, "# Activity - finalizeActivity()")

        // Close bluetooth connection
        // Unregister broadcast listeners

    }

    private fun finishApp() {
        finishAffinity()
        System.runFinalization()
        System.exit(0)
    }

    private fun initialize() {
        // initialize UI
        showConnectionStatus(mIsConnected)
        showMsgLayout(mIsConnected)
        changeBtnStatus(mIsConnected)

        // initialize button
        btn_connect_broker.setOnClickListener(mOnClickListener)
        btn_action.setOnClickListener(mOnClickListener)
        btn_publish.setOnClickListener(mOnClickListener)
        btn_subscribe.setOnClickListener(mOnClickListener)

        // initialize MqttManager
        mMqttManager = MqttManager
        mMqttManager.initialize(this)
        mMqttManager.setManagerListener(mMqttManagerListener)

        mImm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private fun saveUserInput(clientId: String, serverUri: String, port: String): Bundle {
        val bundle = Bundle()

        val cleanSession = Const.DEFAULT_CLEAN_SEESION
        val username = Const.EMPTY
        val password = Const.EMPTY
        val sslkey = Const.EMPTY
        val ssl = Const.DEFAULT_SSL
        val keepalive = Const.DEFAULT_KEEPALIVE
        val timeout = Const.DEFAULT_TIMEOUT
        val qos = Const.DEFAULT_QOS

        var topic: String? = edit_topic.text.toString()
        var message: String? = edit_msg.text.toString()
        val retained = Const.DEFAULT_RETAINED

        if (message == null)
            message = Const.EMPTY
        if (topic == null)
            topic = Const.EMPTY

        // put the data collected into the bundle
        bundle.putString(Const.BUNDLE_MQTT_CLIENT_ID, clientId)
        bundle.putString(Const.BUNDLE_MQTT_SERVER, serverUri)
        bundle.putString(Const.BUNDLE_MQTT_PORT, port)
        bundle.putBoolean(Const.BUNDLE_MQTT_CLEAN_SESSION, cleanSession)
        bundle.putString(Const.BUNDLE_MQTT_USER_NAME, username)
        bundle.putString(Const.BUNDLE_MQTT_PASSWORD, password)
        bundle.putBoolean(Const.BUNDLE_MQTT_SSL, ssl)
        bundle.putString(Const.BUNDLE_MQTT_SSL_KEY, sslkey)
        bundle.putInt(Const.BUNDLE_MQTT_TIMEOUT, timeout)
        bundle.putInt(Const.BUNDLE_MQTT_KEEPALIVE, keepalive)
        bundle.putString(Const.BUNDLE_MQTT_MESSAGE, message)
        bundle.putString(Const.BUNDLE_MQTT_TOPIC, topic)
        bundle.putInt(Const.BUNDLE_MQTT_QOS, qos)
        bundle.putBoolean(Const.BUNDLE_MQTT_RETAINED, retained)

        return bundle
    }

    /**********************************************************************
     * UI
     **********************************************************************/
    private fun changeBtnStatus(isConnected: Boolean) {
        btn_publish.isEnabled = isConnected
        btn_subscribe.isEnabled = isConnected
        btn_connect_broker.text = if(isConnected) getString(R.string.disconnect) else getString(R.string.button_connect_broker)
    }

    private fun changeActionBtn(mode: MqttMode) {
        when(mode) {
            MqttMode.MODE_PUBLISH -> {
                btn_action.text = getString(R.string.btn_action_publish)
            }
            MqttMode.MODE_SUBSCRIBE -> {
                btn_action.text = getString(R.string.btn_action_subscribe)
            }
        }
    }

    private fun resetInput() {
        edit_topic.setText("")
        edit_msg.setText("")
    }

    private fun showMsgLayout(isConnected: Boolean) {
        layout_input_msg.visibility = if(isConnected) View.VISIBLE else View.INVISIBLE
    }

    private fun showConnectionStatus(isConnected: Boolean) {
        if(isConnected) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_online, null))
            } else {
                img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_online))
            }
            text_status.text = resources.getString(R.string.mqtt_state_connected)
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible, null))
            } else {
                img_status.setImageDrawable(resources.getDrawable(android.R.drawable.presence_invisible))
            }
            text_status.text = resources.getString(R.string.disconnect)
        }
    }

    private fun hideKeyboard(view: View) {
        mImm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    /**********************************************************************
     * Listener, Inner class
     **********************************************************************/
    private val mOnClickListener = View.OnClickListener {
        when(it.id) {
            R.id.btn_connect_broker -> {
                if(mIsConnected) {
                    mMqttManager.disconnect(this@MainActivity, mClientHandle!!)
                    mClientHandle = ""
                    mIsConnected = mMqttManager.isConnected(this) > 0
                    if(!mIsConnected) {
                        text_chat.append("\n")
                        text_chat.append("MQTT has been disconnected successfully")
                        showMsgLayout(mIsConnected)
                        showConnectionStatus(mIsConnected)
                        changeBtnStatus(mIsConnected)
                    }
                }
                else {
                    val dialog = DialogConnectInfo(this@MainActivity, object: DialogConnectBtnClickListener {
                        override fun didPressConnectBtn(clientId: String, serverAddr: String, port: String) {
                            val bundle = saveUserInput(clientId, serverAddr, port)
                            Settings.setClientId(this@MainActivity, clientId)
                            Settings.setServer(this@MainActivity, serverAddr)
                            Settings.setPort(this@MainActivity, port)

                            mClientHandle = mMqttManager.addConnection(this@MainActivity, bundle)
                        }
                    })
                    dialog.show()
                }
            }
            R.id.btn_publish -> {
                mMode = MqttMode.MODE_PUBLISH
                edit_msg.isEnabled = true
                changeActionBtn(mMode)
                resetInput()
            }
            R.id.btn_subscribe -> {
                mMode = MqttMode.MODE_SUBSCRIBE
                edit_msg.isEnabled = false
                changeActionBtn(mMode)
                resetInput()
            }
            R.id.btn_action -> {
                when(mMode) {
                    MqttMode.MODE_PUBLISH -> {
                        mMqttManager.publish(this, mClientHandle!!, edit_topic.text.toString(),
                                edit_msg.text.toString(), Const.DEFAULT_QOS, Const.DEFAULT_RETAINED)
                    }
                    MqttMode.MODE_SUBSCRIBE -> {
                        mMqttManager.subscribe(this, mClientHandle!!, edit_topic.text.toString(),
                                Const.DEFAULT_QOS)
                    }
                }
                runOnUiThread {
                    hideKeyboard(layout_input_msg)
                }
            }
        }
    }

    private val mMqttManagerListener = object : IMqttManagerListener {
        override fun onMqttManagerCallback(msgType: Int, arg0: Int, arg1: Int, arg2: String?, arg3: String?, arg4: Any?) {
            when (msgType) {
                IMqttManagerListener.CALLBACK_MQTT_ADD_CONNECTION -> {
                    Logs.d(TAG, "CALLBACK_MQTT_ADD_CONNECTION")
                    if (arg4 != null)  {
                        val c = arg4 as Connection
                        // Update history
                        val msg = ("[+] " + resources.getString(R.string.new_connection_added)
                                + " : " + c.id + "\n")
                        // Add to broker list
                        text_chat.append("\n")
                        text_chat.append(msg)
                    }
                }

                IMqttManagerListener.CALLBACK_MQTT_CONNECTION_DELETED -> {
                    Logs.d(TAG, "CALLBACK_MQTT_CONNECTION_DELETED")
                    if (arg4 != null) {
                        val c = arg4 as Connection
                        // Update history
                        val msg = ("[-] " + resources.getString(R.string.conn_deleted)
                                + " : " + c.id + "\n")
                    }
                }

                IMqttManagerListener.CALLBACK_MQTT_DISCONNECTED -> {
                    Logs.d(TAG, "CALLBACK_MQTT_DISCONNECTED")
                    if (arg4 == null) {
                        val c = Connections.getInstance(this@MainActivity).getConnection(arg2)
                        // Update history
                        val msg = ("[!] " + resources.getString(R.string.disconnected_from)
                                + " " + c.id + "\n")
                        text_chat.append("\n")
                        text_chat.append(msg)
                    }
                }
                IMqttManagerListener.CALLBACK_MQTT_CONNECTED -> {
                    Logs.d(TAG, "CALLBACK_MQTT_CONNECTED")
                    mIsConnected = true
                    text_chat.append("MQTT connection has been established.\n")
                    showMsgLayout(mIsConnected)
                    showConnectionStatus(mIsConnected)
                    changeBtnStatus(mIsConnected)

                    if (arg2 == null) {
                        val c = Connections.getInstance(this@MainActivity).getConnection(null)
                        // Update history
                        val msg = ("[+] " + resources.getString(R.string.connected_to)
                                + c.id + "\n")
                        text_chat.append(msg)
                    }
                }
                IMqttManagerListener.CALLBACK_MQTT_SUBSCRIBED -> {
                    Logs.d(TAG, "CALLBACK_MQTT_SUBSCRIBED")
                    if (arg2 != null && arg3 != null) {
                        // Update history
                        val msg = "[*] $arg3\n"
                        text_chat.append(msg)
                    }
                }
                IMqttManagerListener.CALLBACK_MQTT_PUBLISHED -> {
                    Logs.d(TAG, "CALLBACK_MQTT_PUBLISHED")
                    if (arg2 != null && arg3 != null) {
                        // Update history
                        val msg = "--> $arg3\n"
                        text_chat.append(msg)
                    }
                }
                IMqttManagerListener.CALLBACK_MQTT_CONNECTION_LOST -> {
                    Logs.d(TAG, "CALLBACK_MQTT_CONNECTION_LOST")
                    if (arg4 != null && arg3 != null) {
                        //Connection c = (Connection) arg4;
                        // Update history
                        val msg = "[!] $arg3\n"
                        mIsConnected = false
                        showMsgLayout(mIsConnected)
                        showConnectionStatus(mIsConnected)
                        changeBtnStatus(mIsConnected)
                        text_chat.append(msg)
                    }
                }

                IMqttManagerListener.CALLBACK_MQTT_MESSAGE_ARRIVED -> {
                    Logs.d(TAG, "CALLBACK_MQTT_MESSAGE_ARRIVED")
                    if (arg3 != null && arg4 == null)
                        return
                    else {
                        val c = arg4 as Connection
                        // Update history
                        val msg = ("<-- " + c.id + " "
                                + resources.getString(R.string.message_received_header) + arg3 + "\n")
                        text_chat.append(msg)
                    }
                }

                IMqttManagerListener.CALLBACK_MQTT_MESSAGE_DELIVERED -> {
                    Logs.d(TAG, "MQTT msg delivered")
                }

                IMqttManagerListener.CALLBACK_MQTT_PROPERTY_CHANGED -> {
                    // Use connection status message only
                    if (arg2 != null) {
                        Logs.d(TAG, "MQTT property changed: $arg2")
                    }
                }
            }
        }
    }

    enum class MqttMode {
        MODE_PUBLISH,
        MODE_SUBSCRIBE
    }
}
