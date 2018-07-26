package com.hardcopy.btchat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.hardcopy.btchat.R.layout.activity_splash
import com.hardcopy.btchat.utils.Logs


class SplashActivity : Activity() {
    companion object {
        val TAG = "SplashActivity"
        val REQUEST_BLUETOOTH_PERMISSIONS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_splash)

        Logs.d(TAG, "# SplashActivity - onCreate()")

        // since Marshmallow, you need these permissions for BT scan
        val P1 = Manifest.permission.ACCESS_FINE_LOCATION
        val P2 = Manifest.permission.ACCESS_COARSE_LOCATION
        if(ContextCompat.checkSelfPermission(this, P1) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, P2) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(P1, P2),
                    REQUEST_BLUETOOTH_PERMISSIONS)
            return
        }

        // move to main
        moveToMain()
    }

    fun moveToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Logs.d("Permission granted all!!")
                    moveToMain()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Logs.d("Permission denied!!")
                }
                return
            }
        }
    }
}
