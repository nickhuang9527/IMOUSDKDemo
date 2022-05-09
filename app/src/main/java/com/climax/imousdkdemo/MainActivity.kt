package com.climax.imousdkdemo

import android.Manifest
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.*
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
import com.lechange.opensdk.utils.LCOpenSDK_Utils
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var mInitSDKButton: Button
    private lateinit var mConnectToApOfCamButton: Button
    private lateinit var mInitDeviceInitSDKButton: Button
    private lateinit var mStartSearchDeviceInitInfoButton: Button
    private lateinit var mStopSearchDeviceInitInfoButton: Button
    private lateinit var mInitDeviceByIpButton: Button
    private lateinit var mGetSoftApWifiListButton: Button
    private lateinit var mStartSoftApConfigButton: Button

    private lateinit var mStartServiceButton: Button
    private lateinit var mStopServiceButton: Button
    private lateinit var mBindServiceButton: Button
    private lateinit var mUnbindServiceButton: Button

    private lateinit var mProgressDialog: ProgressDialog

    //
    private var openapiUrl = "openapi-sg.easy4ip.com:443"
    private val userToken = "Ut_00004ece1788ea344788bd57faeab6aa"
    private val deviceId = "7J0A75CPAZD23DD"
    private val safetyCode = "L2FA63C4"

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

    val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, iBinder: IBinder) {
            val myBinder: SearchDeviceInitInfoService.MyBinder =
                iBinder as SearchDeviceInitInfoService.MyBinder
            val result: String = myBinder.startDownload()
            Log.e("＊＊＊", result)

            myBinder.startSearchDeviceInitInfoExs(deviceId, 1000 * 30)  { sncode, searchedDeviceInitInfo ->
                Log.d(TAG, "[searchDeviceInitInfoExs callback]")
                Log.d(TAG, "sncode: $sncode")
                Log.d(TAG, "searchedDeviceInitInfo: $searchedDeviceInitInfo")
            }
            //透過service做一些畫面的操作
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.e("＊＊＊", "onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupView()
        checkPermissions()

        mWifiUtil = WifiUtil(this)
        LCOpenSDK_Utils.enableLogPrint(true)

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
        mInitDeviceInitSDKButton = findViewById(R.id.button_init_device_init_sdk)
        mConnectToApOfCamButton = findViewById(R.id.button_connect_to_ap_of_cam)
        mStartSearchDeviceInitInfoButton = findViewById(R.id.button_start_search_device_init_info)
        mStopSearchDeviceInitInfoButton = findViewById(R.id.button_stop_search_device_init_info)
        mInitDeviceByIpButton = findViewById(R.id.button_init_device_by_ip)
        mGetSoftApWifiListButton = findViewById(R.id.button_get_softap_wifi_list)
        mStartSoftApConfigButton = findViewById(R.id.button_start_soft_ap_config)

        mStartServiceButton = findViewById(R.id.button_start_service)
        mStopServiceButton = findViewById(R.id.button_stop_service)
        mBindServiceButton = findViewById(R.id.button_bind_service)
        mUnbindServiceButton = findViewById(R.id.button_unbind_service)

        mProgressDialog = ProgressDialog(this)
        mProgressDialog.setTitle("Loading...")

        mInitSDKButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "1. Init lechange openSDK")
            Log.d(TAG, "[initOpenApi] url: $openapiUrl, userToken: $userToken")
            val initParams = InitParams(this.applicationContext, openapiUrl, userToken)
            LCOpenSDK_Api.initOpenApi(initParams)
        }

        mInitDeviceInitSDKButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "2. Init device init sdk")
            Log.d(TAG, "[LCOpenSDK_DeviceInit.getInstance()]")
            LCOpenSDK_DeviceInit.getInstance()
        }

        mConnectToApOfCamButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "3. Connect to Ap of IMOU Cam")
//            startScanWifiCountDownTimer()
            LCOpenSDK_DeviceInit.getInstance()
                .searchDeviceInitInfoExs(deviceId, 30 * 1000) { sncode, searchedDeviceInitInfo ->
                    Log.d(TAG, "[searchDeviceInitInfoExs callback]")
                    Log.d(TAG, "sncode: $sncode")
                    Log.d(TAG, "searchedDeviceInitInfo: $searchedDeviceInitInfo")
                    deviceInitInfo = searchedDeviceInitInfo
                }
        }

        mStartSearchDeviceInitInfoButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "4. Start Search deviceInitInfo")
            Log.d(TAG, "[searchDeviceInitInfoExs] deviceId: $deviceId")
            LCOpenSDK_DeviceInit.getInstance()
                .searchDeviceInitInfoExs("", 30 * 1000) { sncode, searchedDeviceInitInfo ->
                    Log.d(TAG, "[searchDeviceInitInfoExs callback]")
                    Log.d(TAG, "sncode: $sncode")
                    Log.d(TAG, "searchedDeviceInitInfo: $searchedDeviceInitInfo")
                    deviceInitInfo = searchedDeviceInitInfo
                }
        }

        mStopSearchDeviceInitInfoButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "5. Stop Search deviceInitInfo")
            Log.d(TAG, "[stopSearchDeviceExs]")
            thread {
                LCOpenSDK_DeviceInit.getInstance().stopSearchDeviceExs()
            }
        }

        mInitDeviceByIpButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "6. Init device by ip")
            Log.d(TAG, "[initDeviceByIpEx] deviceInitInfo: $deviceInitInfo, password: $safetyCode")
            deviceInitInfo?.let {
                when (it.mStatus) {
                    0 -> Log.d(TAG, "Device doesn't support initialization")
                    1 -> {
                        Log.d(TAG, "Device hasn't been initialized")
                        LCOpenSDK_DeviceInit.getInstance().initDeviceByIpEx(it, safetyCode) { isSuccess ->
                            Log.d(TAG, "[initDeviceByIpEx callback]")
                            Log.d(TAG, "isSuccess : $isSuccess")
                        }
                    }
                    2 -> Log.d(TAG, "device has been initialized")
                }
            }

        }

        mGetSoftApWifiListButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "7. Get SoftAp Wifi list")
            val gatewayIp = mWifiUtil?.getGatewayIp() ?: return@setOnClickListener
            Log.d(TAG, "[getSoftApWifiList] gatewayIp: $gatewayIp, password: $safetyCode")


            /*
            LCOpenSDK_SearchWiFi.getSoftApWifiList4Sc(gatewayIp, object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Log.d(TAG, "[getSoftApWifiList4Sc callback]")
                    Log.d(TAG, "msg: $msg")
                    Log.d(TAG, "msg.what: ${msg.what}")
                }
            })
             */

            LCOpenSDK_SearchWiFi.getSoftApWifiList(gatewayIp, safetyCode, object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Log.d(TAG, "[getSoftApWifiList callback]")
                    Log.d(TAG, "msg: $msg")
                    Log.d(TAG, "msg.what: ${msg.what}")
                }
            })
        }

        mStartSoftApConfigButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "7. Start Soft Ap Config")
            val ssid = "ASUS_D8"
            val password = "21245121"
            val isSc = true
            Log.d(TAG, "[startSoftAPConfig] ssid: $ssid, password: $password, deviceId: $deviceId, devPassword: $safetyCode, isSc: $isSc")

            LCOpenSDK_SoftAPConfig.startSoftAPConfig(ssid, password, deviceId, safetyCode, isSc, object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Log.d(TAG, "[startSoftAPConfig callback]")
                    Log.d(TAG, "msg: $msg")
                    Log.d(TAG, "msg.arg1.: ${msg.arg1}")
                }
            }, 30 * 1000)
        }

        mStartServiceButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "Start service")
            val intent = Intent(this@MainActivity, SearchDeviceInitInfoService::class.java)
            intent.putExtra("test", "test service")
            startService(intent)
        }

        mStopServiceButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "Stop service")
            val intent = Intent(this@MainActivity, SearchDeviceInitInfoService::class.java)
            stopService(intent)
        }

        mBindServiceButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "Bind service")
            val intent1 = Intent(
                applicationContext,
                SearchDeviceInitInfoService::class.java
            )
            bindService(intent1, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        mUnbindServiceButton.setOnClickListener {
            Log.d(TAG, "------------------------------------------------------")
            Log.d(TAG, "UnBind service")
            unbindService(serviceConnection)

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