package com.climax.imousdkdemo.utils.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.DhcpInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.climax.imousdkdemo.models.network.WirelessSecurityMode

/**
 * Created by nickhuang on 2022/3/3.
 */
class WifiUtil(val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun setWifiEnable(isEnabled: Boolean) {
        if (wifiManager.isWifiEnabled != isEnabled) {
            wifiManager.isWifiEnabled = isEnabled
        }
    }

    fun getWifiState(): Int {
        return wifiManager.wifiState
    }

    fun startScanWifi() {
        wifiManager.startScan()
    }

    fun getScanResults(): List<ScanResult> {
        return wifiManager.scanResults
    }

    fun connectToWifi(ssid: String, password: String?, wirelessSecurityMode: WirelessSecurityMode, isHiddenSSID: Boolean) {
        Log.d(TAG, "connectToWifi()")
        Log.d(TAG, "ssid: $ssid")
        Log.d(TAG, "password: $password")
        Log.d(TAG, "wirelessSecurityMode: $wirelessSecurityMode")
        Log.d(TAG, "isHiddenSSID: $isHiddenSSID")
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = "\"$ssid\""
        wifiConfiguration.hiddenSSID = isHiddenSSID
        when (wirelessSecurityMode) {
            WirelessSecurityMode.None -> wifiConfiguration.allowedKeyManagement.set(
                WifiConfiguration.KeyMgmt.NONE)
            WirelessSecurityMode.WEP -> {
                wifiConfiguration.wepKeys[0] = "\"$password\""
                wifiConfiguration.wepTxKeyIndex = 0
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            }
            WirelessSecurityMode.WPA -> {
                wifiConfiguration.preSharedKey = "\"$password\""
            }
        }

        setWifiEnable(true)

        val wifiList = wifiManager.configuredNetworks as List<WifiConfiguration>

        for (configuration in wifiList) {
            if (configuration.SSID == "\"$ssid\"") {
                wifiManager.removeNetwork(configuration.networkId)
            }
        }

        val networkId = wifiManager.addNetwork(wifiConfiguration)

        val isSuccess = wifiManager.disconnect()
        wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()
//        for (config in wifiManager.configuredNetworks) {
//            if (config.SSID == "\"$ssid\"") {
//                wifiManager.disconnect()
//                wifiManager.enableNetwork(config.networkId, true)
//                wifiManager.reconnect()
//            }
//        }

    }

    fun getConnectionInfo(): WifiInfo {
        return wifiManager.connectionInfo
    }

    fun getGatewayIp(): String? {
        val dhcpInfo: DhcpInfo = wifiManager.getDhcpInfo()
        return run {
            val gatewayIps = dhcpInfo.gateway.toLong()
            this.long2ip(gatewayIps)
        }
    }

    fun long2ip(ip: Long): String? {
        val stringBuffer = StringBuffer()
        stringBuffer.append((ip and 255L).toString())
        stringBuffer.append('.')
        stringBuffer.append((ip shr 8 and 255L).toString())
        stringBuffer.append('.')
        stringBuffer.append((ip shr 16 and 255L).toString())
        stringBuffer.append('.')
        stringBuffer.append((ip shr 24 and 255L).toString())
        return stringBuffer.toString()
    }

    fun registerReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter) {
        context.registerReceiver(receiver, intentFilter)
    }

    fun unregisterScanResultsReceiver(receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "$receiver is already unregistered")
        }
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }
}