package com.climax.imousdkdemo

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import com.lechange.opensdk.device.LCOpenSDK_DeviceInit
import com.lechange.opensdk.media.DeviceInitInfo

/**
 * Created by nickhuang on 2022/3/11.
 */
class SearchDeviceInitInfoService: Service() {

    private var mReceiver: BroadcastReceiver? = null

    //啟動或bind這個service元件時，第一個呼叫的方法
    override fun onCreate() {
        Log.e(TAG, "onCreate")
        // 注册广播监听器监听网络变化
        // 注册广播监听器监听网络变化
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        mReceiver = Broadcast()
        registerReceiver(mReceiver, intentFilter)
    }

    //啟動service元件時呼叫的方法
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        /*
        Handler().postDelayed({
            LCOpenSDK_DeviceInit.getInstance().searchDeviceInitInfoExs("7J0A75CPAZD23DD", 30 * 1000) { sncode, searchedDeviceInitInfo ->
                Log.d(TAG, "[searchDeviceInitInfoExs callback]")
                Log.d(TAG, "sncode: $sncode")
                Log.d(TAG, "searchedDeviceInitInfo: $searchedDeviceInitInfo")

            }
        }, 5000)
         */

        return super.onStartCommand(intent, flags, startId)
    }

    //停止service時呼叫的方法
    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.e(TAG, "onBind")
        return MyBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.e(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    fun startDownload111(): String {
        var result = "";
        //模擬背景請求工作
        Log.e(TAG, "Start Download")
        Thread.sleep(3000)
        result = "success"
        Log.e(TAG, "finish Download")
        return result
    }

    open fun startSearchDeviceInitInfoExs(deviceId: String, timeout: Int, listener: LCOpenSDK_DeviceInit.ISearchDeviceListener) {
        LCOpenSDK_DeviceInit.getInstance().stopSearchDeviceExs()
        LCOpenSDK_DeviceInit.getInstance().searchDeviceInitInfoExs(deviceId, timeout, listener)
    }

    inner class MyBinder : Binder() {
        fun startSearchDeviceInitInfoExs(deviceId: String, timeout: Int, listener: LCOpenSDK_DeviceInit.ISearchDeviceListener) {
            this@SearchDeviceInitInfoService.startSearchDeviceInitInfoExs(deviceId, timeout, listener)
        }

        fun stopSearchDeviceInitInfoExs() {
            LCOpenSDK_DeviceInit.getInstance().stopSearchDeviceExs()
        }

        fun startDownload(): String {
            var result = "";
            //模擬背景請求工作
            Log.e(TAG, "Start Download")
            Thread.sleep(3000)
            result = "success"
            Log.e(TAG, "finish Download")
            return result
        }
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }

    inner class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.action)) {
                //监听到网络变化
                Log.d(TAG, "检测到网络变化")
                startSearchDeviceInitInfoExs("7J0A75CPAZD23DD", 30 * 1000) { sncode, searchedDeviceInitInfo ->
                    Log.d(TAG, "[searchDeviceInitInfoExs callback]")
                    Log.d(TAG, "sncode: $sncode")
                    Log.d(TAG, "searchedDeviceInitInfo: $searchedDeviceInitInfo")
                }
            }
        }
    }
}