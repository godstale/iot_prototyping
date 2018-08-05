package com.hardcopy.mqttchat.manager

interface IMqttManagerListener {
    companion object {
        const val CALLBACK_MQTT_NULL= 0
        const val CALLBACK_MQTT_ADD_CONNECTION = 1
        const val CALLBACK_MQTT_CONNECTION_DELETED = 2
        const val CALLBACK_MQTT_CONNECTION_LOST = 3
        const val CALLBACK_MQTT_MESSAGE_ARRIVED = 4
        const val CALLBACK_MQTT_MESSAGE_DELIVERED = 5
        const val CALLBACK_MQTT_CONNECTED = 6
        const val CALLBACK_MQTT_DISCONNECTED = 7
        const val CALLBACK_MQTT_PUBLISHED = 8
        const val CALLBACK_MQTT_SUBSCRIBED = 9
        const val CALLBACK_MQTT_PROPERTY_CHANGED = 11

    }

    fun onMqttManagerCallback(msgType: Int, arg0: Int, arg1: Int, arg2: String?, arg3: String?, arg4: Any?)
}