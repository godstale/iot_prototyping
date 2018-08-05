package com.hardcopy.mqttchat.utils

object Const {
    // Preference
    const val PREFERENCE_NAME = "MqttChat"

    const val PREFERENCE_KEY_SET_CLIENT_ID = "SettingClientId"
    const val PREFERENCE_KEY_SET_SERVER = "SettingServer"
    const val PREFERENCE_KEY_SET_PORT = "SettingPort"

    /** Notification  */
    const val NOTI_TARGET_ACTIVITY = "com.hardcopy.vcontroller.MainActivity"

    /* MQTT Default values **/
    /** Default QOS value */
    const val DEFAULT_QOS = 0
    /** Default timeout */
    const val DEFAULT_TIMEOUT = 1000
    /** Default keep alive value */
    const val DEFAULT_KEEPALIVE = 10
    /** Default clean session */
    const val DEFAULT_CLEAN_SEESION = true
    /** Default SSL enabled flag */
    const val DEFAULT_SSL = false
    /** Default message retained flag  */
    const val DEFAULT_RETAINED = false

    /* Bundle Keys for MQTT Connection */
    /** Server Bundle Key  */
    const val BUNDLE_MQTT_SERVER = "server"
    /** Port Bundle Key  */
    const val BUNDLE_MQTT_PORT = "port"
    /** ClientID Bundle Key  */
    const val BUNDLE_MQTT_CLIENT_ID = "clientId"
    /** Topic Bundle Key  */
    const val BUNDLE_MQTT_TOPIC = "topic"
    /** History Bundle Key  */
    const val BUNDLE_MQTT_HISTORY = "history"
    /** Message Bundle Key  */
    const val BUNDLE_MQTT_MESSAGE = "message"
    /** Retained Flag Bundle Key  */
    const val BUNDLE_MQTT_RETAINED = "retained"
    /** QOS Value Bundle Key  */
    const val BUNDLE_MQTT_QOS = "qos"
    /** User name Bundle Key  */
    const val BUNDLE_MQTT_USER_NAME = "username"
    /** Password Bundle Key  */
    const val BUNDLE_MQTT_PASSWORD = "password"
    /** Keep Alive value Bundle Key  */
    const val BUNDLE_MQTT_KEEPALIVE = "keepalive"
    /** Timeout Bundle Key  */
    const val BUNDLE_MQTT_TIMEOUT = "timeout"
    /** SSL Enabled Flag Bundle Key  */
    const val BUNDLE_MQTT_SSL = "ssl"
    /** SSL Key File Bundle Key  */
    const val BUNDLE_MQTT_SSL_KEY = "ssl_key"
    /** Clean Session Flag Bundle Key  */
    const val BUNDLE_MQTT_CLEAN_SESSION = "cleanSession"

    /* Property names */
    /** Property name for the history field in [Connection] object for use with [java.beans.PropertyChangeEvent]  */
    const val PROPERTY_HISTORY = "history"
    /** Property name for the connection status field in [Connection] object for use with [java.beans.PropertyChangeEvent]  */
    const val PROPERTY_CONNECTION_STATUS = "connectionStatus"

    /** Empty String for comparisons  */
    val EMPTY = String()
}
