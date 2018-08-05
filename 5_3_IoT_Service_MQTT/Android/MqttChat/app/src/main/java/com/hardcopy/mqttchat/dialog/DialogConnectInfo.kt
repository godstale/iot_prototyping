package com.hardcopy.mqttchat.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import com.hardcopy.mqttchat.R
import com.hardcopy.mqttchat.utils.Settings
import kotlinx.android.synthetic.main.dialog_connect_broker.*


class DialogConnectInfo(private val mContext: Context,
                            private val mListener: DialogConnectBtnClickListener?)
    : Dialog(mContext), DialogConnectBtnClickListener {

    var mEtId: EditText? = null
    init {
        setCancelable(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        setContentView(R.layout.dialog_connect_broker)
        loadSetting()
        initAction()
    }

    private fun loadSetting() {
        dialog_edit_client_id.setText(Settings.getClientId(mContext))
        dialog_edit_server.setText(Settings.getServer(mContext))
        dialog_edit_port.setText(Settings.getPort(mContext))
    }

    private fun initAction() {
        btn_connect.setOnClickListener {
            didPressConnectBtn(dialog_edit_client_id.text.toString(),
                    dialog_edit_server.text.toString(),
                    dialog_edit_port.text.toString())
        }     // v = view
        btn_cancel.setOnClickListener { dismiss() }     // v = view
    }


    override fun didPressConnectBtn(clientId: String, serverAddr: String, port: String) {
        if(clientId.isEmpty() || serverAddr.isEmpty() || port.isEmpty()) {
            Toast.makeText(mContext, mContext.getString(R.string.alert_insufficient_info), Toast.LENGTH_SHORT).show()
            return
        }
        if (mListener != null) {
            mListener.didPressConnectBtn(clientId, serverAddr, port)
        }
        dismiss()
    }

    override fun show() {
        super.show()
    }
}
