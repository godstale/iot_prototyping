package com.hardcopy.mqttchat.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.hardcopy.mqttchat.R
import com.hardcopy.mqttchat.mqtt.Connection
import com.hardcopy.mqttchat.mqtt.Connections
import com.hardcopy.mqttchat.mqtt.MqttTraceCallback
import com.hardcopy.mqttchat.utils.Const
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.FileInputStream
import java.io.FileNotFoundException

object MqttManager {
    private val mChangeListener = ChangeListener()
    private var mActionCallback: MqttActionCallback? = null

    private lateinit var mManagerListener: IMqttManagerListener

    fun setManagerListener(l: IMqttManagerListener) {
        mManagerListener = l
    }

    fun initialize(c: Context) {
        mActionCallback = MqttActionCallback()
        // get all the available connections
        val connections = Connections.getInstance(c).getConnections()
        // register change listener
        for (conn in connections.values) {
            conn.registerChangeListener(mChangeListener)
        }
    }

    /************************************************************************
     *
     * Public methods
     *
     ************************************************************************/

    fun isConnected(c: Context): Int {
        var count = 0
        val connections = Connections.getInstance(c).connections
        for (connection in connections.values) {
            if (connection.isConnected) {
                count++
            }
        }
        return count
    }

    fun addConnection(c: Context, data: Bundle?): String {
        if (data == null)
            return ""

        val conOpt = MqttConnectOptions()
        /*
         * Mutal Auth connections could do something like this
         *
         * SSLContext context = SSLContext.getDefault();
         * context.init({new CustomX509KeyManager()},null,null); //where CustomX509KeyManager proxies calls to keychain api
         * SSLSocketFactory factory = context.getSSLSocketFactory();
         *
         * MqttConnectOptions options = new MqttConnectOptions();
         * options.setSocketFactory(factory);
         * client.connect(options);
         */

        // The basic client information
        val server = data.get(Const.BUNDLE_MQTT_SERVER) as String
        val clientId = data.get(Const.BUNDLE_MQTT_CLIENT_ID) as String
        val port = Integer.parseInt(data.get(Const.BUNDLE_MQTT_PORT) as String)
        val cleanSession = data.get(Const.BUNDLE_MQTT_CLEAN_SESSION) as Boolean

        var ssl = data.get(Const.BUNDLE_MQTT_SSL) as Boolean
        val sslKey = data.get(Const.BUNDLE_MQTT_SSL_KEY) as String
        var uri: String? = null
        if (ssl) {
            Log.e("SSLConnection", "Doing an SSL Connect")
            uri = "ssl://"
        } else {
            uri = "tcp://"
        }
        uri = "$uri$server:$port"

        val client: MqttAndroidClient
        client = Connections.getInstance(c).createClient(c, uri, clientId)

        if (ssl) {
            try {
                if (!sslKey.equals("", ignoreCase = true)) {
                    val key = FileInputStream(sslKey)
                    conOpt.socketFactory = client.getSSLSocketFactory(key, "mqtttest")
                } else {
                    ssl = false
                }
            } catch (e: MqttSecurityException) {
                Log.e(this.javaClass.canonicalName, "MqttException Occured: ", e)
                ssl = false
            } catch (e: FileNotFoundException) {
                Log.e(this.javaClass.canonicalName, "MqttException Occured: SSL Key file not found", e)
                ssl = false
            }

        }

        // create a client handle
        var clientHandle = uri + clientId

        // last will message
        val message = data.get(Const.BUNDLE_MQTT_MESSAGE) as String
        val topic = data.get(Const.BUNDLE_MQTT_TOPIC) as String
        val qos = data.get(Const.BUNDLE_MQTT_QOS) as Int
        val retained = data.get(Const.BUNDLE_MQTT_RETAINED) as Boolean

        // connection options
        val username = data.get(Const.BUNDLE_MQTT_USER_NAME) as String
        val password = data.get(Const.BUNDLE_MQTT_PASSWORD) as String
        val timeout = data.get(Const.BUNDLE_MQTT_TIMEOUT) as Int
        val keepalive = data.get(Const.BUNDLE_MQTT_KEEPALIVE) as Int

        val connection = Connection(clientHandle, clientId, server, port, c, client, ssl)
        connection.registerChangeListener(mChangeListener)

        // connect client
        connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING)

        conOpt.isCleanSession = cleanSession
        conOpt.connectionTimeout = timeout
        conOpt.keepAliveInterval = keepalive
        if (username != Const.EMPTY) {
            conOpt.userName = username
        }
        if (password != Const.EMPTY) {
            conOpt.password = password.toCharArray()
        }

        // ActionListener notify user with toast and change connection status
        val callback = MqttActionListener(c,
                MqttActionListener.Action.CONNECT, clientHandle, mActionCallback, clientId)

        var doConnect = true

        if (message != Const.EMPTY || topic != Const.EMPTY) {
            // need to make a message since last will is set
            try {
                conOpt.setWill(topic, message.toByteArray(), qos,
                        retained)
            } catch (e: Exception) {
                Log.e(this.javaClass.canonicalName, "Exception Occured", e)
                doConnect = false
                callback.onFailure(null, e)
            }

        }
        // set callback for connection status, publish and message receive event
        client.setCallback(MqttManager.MqttCallbackHandler(c, clientHandle))

        //set traceCallback
        client.setTraceCallback(MqttTraceCallback())

        connection.addConnectionOptions(conOpt)
        Connections.getInstance(c).addConnection(connection)
        if (doConnect) {
            try {
                client.connect(conOpt, c, callback)
                mManagerListener.onMqttManagerCallback(IMqttManagerListener.CALLBACK_MQTT_ADD_CONNECTION,
                        0, 0, null, null, connection as Any)
            } catch (e: MqttException) {
                Log.e(this.javaClass.canonicalName, "MqttException Occured", e)
                clientHandle = ""
            }

        }
        return clientHandle
    }

    /**
     * Disconnect the client
     */
    fun disconnect(c: Context, clientHandle: String) {
        val connection = Connections.getInstance(c).getConnection(clientHandle)
        //if the client is not connected, process the disconnect
        if (connection == null || !connection.isConnected) {
            return
        }
        try {
            connection.client.disconnect(c, MqttActionListener(c, MqttActionListener.Action.DISCONNECT,
                    clientHandle, mActionCallback, null))
            connection.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTING)
        } catch (e: MqttException) {
            Log.e(this.javaClass.canonicalName, "Failed to disconnect the client with the handle $clientHandle", e)
            connection.addAction("Client failed to disconnect")
        }

    }

    /**
     * Subscribe to a topic that the user has specified
     */
    fun subscribe(c: Context, clientHandle: String, topic: String, qos: Int) {
        val connection = Connections.getInstance(c).getConnection(clientHandle)
        if (connection == null || !connection.isConnected) {
            return
        }
        try {
            connection.client.subscribe(topic, qos, null,
                    MqttActionListener(c, MqttActionListener.Action.SUBSCRIBE,
                            clientHandle, mActionCallback, topic))
        } catch (e: MqttSecurityException) {
            Log.e(this.javaClass.canonicalName,
                    "Failed to subscribe to$topic the client with the handle $clientHandle", e)
        } catch (e: MqttException) {
            Log.e(this.javaClass.canonicalName,
                    "Failed to subscribe to$topic the client with the handle $clientHandle", e)
        }
    }

    /**
     * Publish the message the user has specified
     */
    fun publish(c: Context, clientHandle: String, topic: String, message: String, qos: Int, retained: Boolean) {
        val args = "$topic;qos:$qos;retained:$retained"

        val connection = Connections.getInstance(c).getConnection(clientHandle)
        if (connection == null || !connection.isConnected) {
            return
        }
        try {
            connection.client.publish(topic, message.toByteArray(), qos, retained, null,
                    MqttActionListener(c, MqttActionListener.Action.PUBLISH,
                            clientHandle, mActionCallback, message, args))
        } catch (e: MqttSecurityException) {
            Log.e(this.javaClass.canonicalName, "Failed to publish a messged from the client with the handle $clientHandle", e)
        } catch (e: MqttException) {
            Log.e(this.javaClass.canonicalName, "Failed to publish a messged from the client with the handle $clientHandle", e)
        }

    }


    /************************************************************************
     *
     * Listener, Callback
     *
     ***********************************************************************/

    /**
     * This class ensures that the user interface is updated as the Connection objects change their states
     */
    private class ChangeListener : PropertyChangeListener {
        /**
         * @see java.beans.PropertyChangeListener.propertyChange
         */
        private var propertyDetails: String? = null

        override fun propertyChange(event: PropertyChangeEvent) {
            // Use connection status message only
            if (event.propertyName != Const.PROPERTY_CONNECTION_STATUS) {
                return
            }
            propertyDetails = "[!] " + event.source
            mManagerListener.onMqttManagerCallback(
                    IMqttManagerListener.CALLBACK_MQTT_PROPERTY_CHANGED,
                    0, 0,
                    propertyDetails, null, null)
        }
    }

    class MqttActionCallback : IMqttActionCallback {
        override fun onActionResult(msg_type: Int, arg0: Int, arg1: Int, arg2: String?, arg3: String?, arg4: Any?) {
            var callbackType = IMqttManagerListener.CALLBACK_MQTT_NULL
            when (msg_type) {
                IMqttActionCallback.ACTION_RESULT_CONNECTED ->
                    callbackType = IMqttManagerListener.CALLBACK_MQTT_CONNECTED
                IMqttActionCallback.ACTION_RESULT_DISCONNECTED ->
                    callbackType = IMqttManagerListener.CALLBACK_MQTT_DISCONNECTED
                IMqttActionCallback.ACTION_RESULT_PUBLISHED ->
                    callbackType = IMqttManagerListener.CALLBACK_MQTT_PUBLISHED
                IMqttActionCallback.ACTION_RESULT_SUBSCRIBED ->
                    callbackType = IMqttManagerListener.CALLBACK_MQTT_SUBSCRIBED
            }
            // notify to activity
            mManagerListener.onMqttManagerCallback(
                    callbackType,
                    0, 0,
                    arg2/*handle*/, arg3, null)

        }
    }

    class MqttCallbackHandler
    /**
     * Creates an `MqttCallbackHandler` object
     * @param context The application's context
     * @param clientHandle The handle to a [Connection] object
     */
    (
            /** [Context] for the application used to format and import external strings */
            private val context: Context,
            /** Client handle to reference the connection that this handler is attached to */
            private val clientHandle: String) : MqttCallback {

        /**
         * @see org.eclipse.paho.client.mqttv3.MqttCallback.connectionLost
         */
        override fun connectionLost(cause: Throwable?) {
            // cause.printStackTrace();
            if (cause != null) {
                val c = Connections.getInstance(context).getConnection(clientHandle)
                c.addAction("Connection Lost")
                c.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED)

                // for notification
                // format string to use a notification text
                val message = context.getString(R.string.connection_lost, c.id, c.hostName)

                // notify to activity
                mManagerListener!!.onMqttManagerCallback(
                        IMqttManagerListener.CALLBACK_MQTT_CONNECTION_LOST,
                        0, 0,
                        c.handle(), message, c)

                //build intent
                val intent = Intent()
                intent.setClassName(context, Const.NOTI_TARGET_ACTIVITY)
                intent.putExtra("handle", clientHandle)
            }
        }

        /**
         * @see org.eclipse.paho.client.mqttv3.MqttCallback.messageArrived
         */
        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            //Get connection object associated with this object
            val c = Connections.getInstance(context).getConnection(clientHandle)

            //create arguments to format message arrived notifcation string
            val args = arrayOfNulls<String>(2)
            args[0] = String(message.payload)
            args[1] = topic + ";qos:" + message.qos + ";retained:" + message.isRetained

            //update client history
            c.addAction(args[0])

            // notify to activity
            mManagerListener.onMqttManagerCallback(
                    IMqttManagerListener.CALLBACK_MQTT_MESSAGE_ARRIVED,
                    0, 0,
                    c.handle(), args[0], c)
        }

        /**
         * @see org.eclipse.paho.client.mqttv3.MqttCallback.deliveryComplete
         */
        override fun deliveryComplete(token: IMqttDeliveryToken) {
            // notify to activity
            // I think this is same with [MqttActionCallback - IMqttActionCallback.ACTION_RESULT_PUBLISHED]
            mManagerListener.onMqttManagerCallback(
                    IMqttManagerListener.CALLBACK_MQTT_MESSAGE_DELIVERED,
                    0, 0, null, null, token)
        }

    } // End of class MqttCallbackHandler
}