package com.climax.imousdkdemo.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.climax.imousdkdemo.MyApplication

class WifiConnectivityManager() {

    companion object {
        private var mInstance: WifiConnectivityManager? = null
        private val TAG = this::class.java.simpleName

        fun getInstance(): WifiConnectivityManager {
            return synchronized(this) {
                if(mInstance == null) {
                    mInstance = WifiConnectivityManager()
                }

                mInstance as WifiConnectivityManager
            }
        }
    }

    private var mNetworkCallback: ConnectivityManager.NetworkCallback? = null

    private val mConnectivityManager by lazy {
        MyApplication.instance.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    public fun bindToWifiNetwork() {
        val networkRequest = NetworkRequest.Builder().apply {
            addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        }.build()

        mNetworkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                network.let {
                    val networkInfo = mConnectivityManager.getNetworkInfo(it)
                    val capabilities = mConnectivityManager.getNetworkCapabilities(it)

                    val isConnected = networkInfo?.isConnected
                    val hasWifiTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

                    if(isConnected!! && hasWifiTransport!!) {
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> mConnectivityManager.bindProcessToNetwork(it)
                            else -> ConnectivityManager.setProcessDefaultNetwork(it)
                        }
                        Log.d(TAG, "[WifiConnectivityManager] Bind process to Wifi network")
                    }
                }

//                mConnectivityManager.unregisterNetworkCallback(this)
//                Log.d(TAG, "[WifiConnectivityManager] Unregister network callback")
            }
        }

        mConnectivityManager.registerNetworkCallback(networkRequest, mNetworkCallback as ConnectivityManager.NetworkCallback)
//        connManager.requestNetwork(networkRequest, callback)
        Log.d(TAG, "[WifiConnectivityManager] Register network callback")
    }

    public fun unbindToWifiNetwork() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> mConnectivityManager.bindProcessToNetwork(null)
            else -> ConnectivityManager.setProcessDefaultNetwork(null)
        }
        Log.d(TAG, "[WifiConnectivityManager] Unbind process to Wifi network")

        mNetworkCallback?.let {
            mConnectivityManager.unregisterNetworkCallback(it)
            mNetworkCallback = null
            Log.d(TAG, "[WifiConnectivityManager] Unregister network callback")
        }
    }
}