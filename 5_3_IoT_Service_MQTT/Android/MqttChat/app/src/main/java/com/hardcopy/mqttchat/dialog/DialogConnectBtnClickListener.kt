package com.hardcopy.mqttchat.dialog

interface DialogConnectBtnClickListener {
    fun didPressConnectBtn(clientId: String, serverAddr: String, port: String)
}