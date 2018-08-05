package com.hardcopy.mqttchat.manager

import android.content.Context
import com.hardcopy.mqttchat.R
import com.hardcopy.mqttchat.manager.MqttActionListener.Action
import com.hardcopy.mqttchat.mqtt.Connection
import com.hardcopy.mqttchat.mqtt.Connections
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * The [Action] that is associated with this instance of
 * `ActionListener`
 * action: Action
 * additionalArgs : The arguments passed to be used for formatting strings
 * clientHandle : Handle of the [Connection] this action was being executed on
 * context : [Context] for performing various operations
 */

class  MqttActionListener(
        var context: Context, var action: Action,
        var clientHandle: String?, var actionCallback: IMqttActionCallback?,
        vararg val additionalArgs: String?
) : IMqttActionListener {
    /**
     * Actions that can be performed Asynchronously **and** associated with a
     * [ActionListener] object
     *
     */
    enum class Action {
        /** Connect Action  */
        CONNECT,
        /** Disconnect Action  */
        DISCONNECT,
        /** Subscribe Action  */
        SUBSCRIBE,
        /** Publish Action  */
        PUBLISH
    }

    /**
     * The action associated with this listener has been successful.
     *
     * @param asyncActionToken
     * This argument is not used
     */
    override fun onSuccess(asyncActionToken: IMqttToken) {
        when (action) {
            MqttActionListener.Action.CONNECT -> connect()
            MqttActionListener.Action.DISCONNECT -> disconnect()
            MqttActionListener.Action.SUBSCRIBE -> subscribe()
            MqttActionListener.Action.PUBLISH -> publish()
        }
    }

    /**
     * A publish action has been successfully completed, update connection
     * object associated with the client this action belongs to, then notify the
     * user of success
     */
    private fun publish() {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        val actionTaken = context.getString(R.string.toast_pub_success,
                *additionalArgs)
        c.addAction(actionTaken)
        //Notify.toast(context, actionTaken, Toast.LENGTH_SHORT);
        actionCallback?.onActionResult(IMqttActionCallback.ACTION_RESULT_PUBLISHED, 0, 0,
                clientHandle, actionTaken, null)
    }

    /**
     * A subscribe action has been successfully completed, update the connection
     * object associated with the client this action belongs to and then notify
     * the user of success
     */
    private fun subscribe() {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        val actionTaken = context.getString(R.string.toast_sub_success, *additionalArgs)
        c.addAction(actionTaken)
        actionCallback?.onActionResult(IMqttActionCallback.ACTION_RESULT_SUBSCRIBED, 0, 0,
                clientHandle, actionTaken, null)
    }

    /**
     * A disconnection action has been successfully completed, update the
     * connection object associated with the client this action belongs to and
     * then notify the user of success.
     */
    private fun disconnect() {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        c.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED)
        val actionTaken = context.getString(R.string.toast_disconnected)
        c.addAction(actionTaken)
        actionCallback?.onActionResult(IMqttActionCallback.ACTION_RESULT_DISCONNECTED, 0, 0,
                clientHandle, null, null)
    }

    /**
     * A connection action has been successfully completed, update the
     * connection object associated with the client this action belongs to and
     * then notify the user of success.
     */
    private fun connect() {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        val actionTaken = context.getString(R.string.toast_connected) + c.id
        c.changeConnectionStatus(Connection.ConnectionStatus.CONNECTED)
        c.addAction("Client Connected")
        actionCallback?.onActionResult(IMqttActionCallback.ACTION_RESULT_CONNECTED, 0, 0,
                clientHandle, actionTaken, null)
    }

    /**
     * The action associated with the object was a failure
     *
     * @param token
     * This argument is not used
     * @param exception
     * The exception which indicates why the action failed
     */
    override fun onFailure(token: IMqttToken?, exception: Throwable) {
        when (action) {
            MqttActionListener.Action.CONNECT -> connect(exception)
            MqttActionListener.Action.DISCONNECT -> disconnect(exception)
            MqttActionListener.Action.SUBSCRIBE -> subscribe(exception)
            MqttActionListener.Action.PUBLISH -> publish(exception)
        }
    }

    /**
     * A publish action was unsuccessful, notify user and update client history
     *
     * @param exception
     * This argument is not used
     */
    private fun publish(exception: Throwable) {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        val action = context.getString(R.string.toast_pub_failed, *additionalArgs)
        c.addAction(action)
    }

    /**
     * A subscribe action was unsuccessful, notify user and update client history
     * @param exception This argument is not used
     */
    private fun subscribe(exception: Throwable) {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        val action = context.getString(R.string.toast_sub_failed, *additionalArgs)
        c.addAction(action)
    }

    /**
     * A disconnect action was unsuccessful, notify user and update client history
     * @param exception This argument is not used
     */
    private fun disconnect(exception: Throwable) {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        c.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED)
        c.addAction("Disconnect Failed - an error occured")
    }

    /**
     * A connect action was unsuccessful, notify the user and update client history
     * @param exception This argument is not used
     */
    private fun connect(exception: Throwable) {
        val c = Connections.getInstance(context).getConnection(clientHandle)
        c.changeConnectionStatus(Connection.ConnectionStatus.ERROR)
        c.addAction("Client failed to connect")
    }


}