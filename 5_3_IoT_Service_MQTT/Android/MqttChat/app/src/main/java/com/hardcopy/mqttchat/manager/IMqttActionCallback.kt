package com.hardcopy.mqttchat.manager

interface IMqttActionCallback {
    companion object {
        const val ACTION_RESULT_CONNECTED = 0
        const val ACTION_RESULT_DISCONNECTED = 1
        const val ACTION_RESULT_SUBSCRIBED = 2
        const val ACTION_RESULT_PUBLISHED = 3
    }
    fun onActionResult(msg_type: Int, arg0: Int, arg1: Int, arg2: String?, arg3: String?, arg4: Any?)
}