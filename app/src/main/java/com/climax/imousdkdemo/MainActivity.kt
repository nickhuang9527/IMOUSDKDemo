package com.climax.imousdkdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var mInitSDKButton: Button
    private lateinit var mConnectToApOfCamButton: Button
    private lateinit var mSearchDeviceInitInfoButton: Button
    private lateinit var mInitDeviceByIpButton: Button
    private lateinit var mGetSoftApWifiListButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupView()
    }

    private fun setupView() {
        mInitSDKButton = findViewById(R.id.button_init_sdk)
        mConnectToApOfCamButton = findViewById(R.id.button_connect_to_ap_of_cam)
        mSearchDeviceInitInfoButton = findViewById(R.id.button_search_device_init_info)
        mInitDeviceByIpButton = findViewById(R.id.button_init_device_by_ip)
        mGetSoftApWifiListButton = findViewById(R.id.button_get_softap_wifi_list)

        mInitSDKButton.setOnClickListener {
            Log.d(TAG, "Init lechange openSDK")
        }

        mConnectToApOfCamButton.setOnClickListener {
            Log.d(TAG, "Connect to Ap of IMOU Cam")
        }

        mSearchDeviceInitInfoButton.setOnClickListener {
            Log.d(TAG, "Search deviceInitInfo")
        }

        mInitDeviceByIpButton.setOnClickListener {
            Log.d(TAG, "Init device by ip")
        }

        mGetSoftApWifiListButton.setOnClickListener {
            Log.d(TAG, "Get SoftAp Wifi list")
        }
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }
}