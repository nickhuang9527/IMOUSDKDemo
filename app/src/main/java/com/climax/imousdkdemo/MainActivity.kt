package com.climax.imousdkdemo

import android.Manifest
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.climax.imousdkdemo.models.network.WirelessSecurityMode
import com.climax.imousdkdemo.utils.network.WifiUtil
import com.lechange.opensdk.api.InitParams
import com.lechange.opensdk.api.LCOpenSDK_Api
import com.lechange.opensdk.device.LCOpenSDK_DeviceInit
import com.lechange.opensdk.media.DeviceInitInfo
import com.lechange.opensdk.searchwifi.LCOpenSDK_SearchWiFi
import com.lechange.opensdk.softap.LCOpenSDK_SoftAPConfig

class MainActivity : AppCompatActivity() {

    private lateinit var mInitSDKButton: Button
    private lateinit var mConnectToApOfCamButton: Button
    private lateinit var mStartSearchDeviceInitInfoButton: Button
    private lateinit var mStopSearchDeviceInitInfoButton: Button
    private lateinit var mInitDeviceByIpButton: Button
    private lateinit var mGetSoftApWifiListButton: Button
    private lateinit var mStartSoftApConfigButton: Button

    private lateinit var mProgressDialog: ProgressDialog

    //
    private var openapiUrl = "openapi-sg.easy4ip.com:443"
    private val userToken = "Ut_0000236cc6c3fe8644ea953cf8d730c6"
    private val deviceId = "7J0A75CPAZD89A9"
    private val safetyCode = "L2746585"

    private var deviceInitInfo: DeviceInitInfo? = null
    private var mWifiUtil: WifiUtil? = null
    private var mScanWifiCountDownTimer: CountDownTimer? = null
    private val mWifiScanResultsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "mWifiScanResultsReceiver.onReceive($context, $intent)")

            val results = mWifiUtil?.getScanResults() ?: return
            Log.d(TAG, "results: $results")

            val deviceApSsid = "DAP-$deviceId"
            val deviceAp = results.firstOrNull { it.SSID == deviceApSsid } ?: return

            mWifiUtil?.unregisterScanResultsReceiver(this)
            connectToDeviceAp(deviceAp)
        }
    }
    private val mNetworkStateChangeActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "mNetworkStateChangeActionReceiver.onReceive($context, $intent)")

            if (intent?.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                val wifiInfo = mWifiUtil?.getConnectionInfo() ?: return
                Log.d(TAG, "networkInfo.state : ${networkInfo?.state}")
                Log.d(TAG, "wifiInfo : $wifiInfo")
                if (networkInfo?.state == NetworkInfo.State.CONNECTED) {
                    val deviceApSsid = "DAP-$deviceId"
                    Log.d(TAG, "deviceApSsid : $deviceApSsid")
                    if (wifiInfo.networkId != -1 && wifiInfo.ssid == "\"$deviceApSsid\"") {
                        Log.d(TAG, "Connect to $deviceApSsid succeed")

                        hideLoadingView()
                        stopScanWifiCountDownTimer()
                        mWifiUtil?.unregisterScanResultsReceiver(this)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupView()
        checkPermissions()

        mWifiUtil = WifiUtil(this)

//        WifiConnectivityManager.getInstance().bindToWifiNetwork()
    }

    override fun onPause() {
        super.onPause()
        mWifiUtil?.unregisterScanResultsReceiver(mWifiScanResultsReceiver)
        mWifiUtil?.unregisterScanResultsReceiver(mNetworkStateChangeActionReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanWifiCountDownTimer()
        mWifiUtil?.unregisterScanResultsReceiver(mWifiScanResultsReceiver)
        mWifiUtil?.unregisterScanResultsReceiver(mNetworkStateChangeActionReceiver)

//        WifiConnectivityManager.getInstance().unbindToWifiNetwork()
    }

    private fun setupView() {
        mInitSDKButton = findViewById(R.id.button_init_sdk)
        mConnectToApOfCamButton = findViewById(R.id.button_connect_to_ap_of_cam)
        mStartSearchDeviceInitInfoButton = findViewById(R.id.button_start_search_device_init_info)
        mStopSearchDeviceInitInfoButton = findViewById(R.id.button_stop_search_device_init_info)
        mInitDeviceByIpButton = findViewById(R.id.button_init_device_by_ip)
        mGetSoftApWifiListButton = findViewById(R.id.button_get_softap_wifi_list)
        mStartSoftApConfigButton = findViewById(R.id.button_start_soft_ap_config)

        mProgressDialog = ProgressDialog(this)
        mProgressDialog.setTitle("Loading...")

        mInitSDKButton.setOnClickListener {
            Log.d(TAG, "Init lechange openSDK")
            val initParams = InitParams(this.applicationContext, openapiUrl, userToken)
            LCOpenSDK_Api.initOpenApi(initParams)
        }

        mConnectToApOfCamButton.setOnClickListener {
            Log.d(TAG, "Connect to Ap of IMOU Cam")
            startScanWifiCountDownTimer()
        }

        mStartSearchDeviceInitInfoButton.setOnClickListener {
            Log.d(TAG, "Start Search deviceInitInfo")
            LCOpenSDK_DeviceInit.getInstance()
                .searchDeviceInitInfoExs(deviceId, 30 * 1000) { sncode, searchedDeviceInitInfo ->
                    Log.d(TAG, "sncode: $sncode")
                    Log.d(TAG, "searchedDeviceInitInfo: $searchedDeviceInitInfo")
                    deviceInitInfo = searchedDeviceInitInfo
                }
        }

        mStopSearchDeviceInitInfoButton.setOnClickListener {
            Log.d(TAG, "Stop Search deviceInitInfo")
            LCOpenSDK_DeviceInit.getInstance().stopSearchDeviceExs()
        }

        mInitDeviceByIpButton.setOnClickListener {
            Log.d(TAG, "Init device by ip")
            deviceInitInfo?.let {
                when (it.mStatus) {
                    0 -> Log.d(TAG, "Device doesn't support initialization")
                    1 -> {
                        Log.d(TAG, "Device hasn't been initialized")
                        LCOpenSDK_DeviceInit.getInstance().initDeviceByIpEx(it, safetyCode) { isSuccess ->
                            Log.d(TAG, "initDeviceByIpEx isSuccess : $isSuccess")
                        }
                    }
                    2 -> Log.d(TAG, "device has been initialized")
                }
            }

        }

        mGetSoftApWifiListButton.setOnClickListener {
            Log.d(TAG, "Get SoftAp Wifi list")
            val gatewayIp = mWifiUtil?.getGatewayIp() ?: return@setOnClickListener
            /*
            LCOpenSDK_SearchWiFi.getSoftApWifiList4Sc(gatewayIp, object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Log.d(TAG, "msg: $msg")
                }
            })

             */
            LCOpenSDK_SearchWiFi.getSoftApWifiList(gatewayIp, safetyCode, object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Log.d(TAG, "msg: $msg")
                }
            })
        }

        mStartSoftApConfigButton.setOnClickListener {
            val ssid = "ASUS_D8"
            val password = "請填入password"
            val isSc = true
            LCOpenSDK_SoftAPConfig.startSoftAPConfig(ssid, password, deviceId, safetyCode, isSc, object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Log.d(TAG, "msg: $msg")
                }
            }, 30 * 1000)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "requestCode: $requestCode")
        Log.d(TAG, "permissions: $permissions")
        Log.d(TAG, "grantResults: $grantResults")
    }

    fun checkPermissions() {
//        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PERMISSION_REQUEST_ACCESS_COARSE_LOCATION)
        checkPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            PERMISSION_REQUEST_ACCESS_FIND_LOCATION
        )
    }

    // Function to check and request permission
    fun checkPermission(permission: String, requestCode: Int) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            Toast.makeText(this@MainActivity, "Permission already granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    //region Loading

    private fun showLoadingView() {
        mProgressDialog.show()
    }

    private fun hideLoadingView() {
        mProgressDialog.hide()
    }

    //endregion

    //region ConnectToApOfDevice

    private fun startScanWifiAndConnectToDeviceAp() {
        Log.d(TAG, "scanWifiAndConnectToDeviceAP")
        mWifiUtil?.setWifiEnable(true)
        mWifiUtil?.unregisterScanResultsReceiver(mWifiScanResultsReceiver)
        mWifiUtil?.registerReceiver(
            mWifiScanResultsReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
        mWifiUtil?.startScanWifi()
    }

    private fun startScanWifiCountDownTimer() {
        Log.d(TAG, "startScanWifiAndConnectToDeviceApCountDownTimer")

        showLoadingView()

        stopScanWifiCountDownTimer()

        mScanWifiCountDownTimer = object : CountDownTimer(30000, 10000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "onTick: $millisUntilFinished")
                startScanWifiAndConnectToDeviceAp()
            }

            override fun onFinish() {
                Log.d(TAG, "onFinish")

                hideLoadingView()
                stopScanWifiCountDownTimer()
            }
        }.start()
    }

    private fun stopScanWifiCountDownTimer() {
        Log.d(TAG, "stopScanWifiAndConnectToDeviceApCountDownTimer")

        mScanWifiCountDownTimer?.cancel()
        mScanWifiCountDownTimer = null
    }

    private fun connectToDeviceAp(deviceAp: ScanResult) {
        Log.d(TAG, "connectToDeviceAp: $deviceAp")
        val ssid = deviceAp.SSID
        val password = safetyCode
        val wirelessSecurityMode = WirelessSecurityMode.fromCapabilities(deviceAp.capabilities)

        mWifiUtil?.registerReceiver(
            mNetworkStateChangeActionReceiver,
            IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        )
        mWifiUtil?.connectToWifi(ssid, password, wirelessSecurityMode, false)
    }

    //endregion

    companion object {
        private val TAG = this::class.java.simpleName
        const val PERMISSION_REQUEST_ACCESS_COARSE_LOCATION = 200
        const val PERMISSION_REQUEST_ACCESS_FIND_LOCATION = 201
    }
}