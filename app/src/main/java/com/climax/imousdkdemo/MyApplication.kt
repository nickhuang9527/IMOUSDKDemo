package com.climax.imousdkdemo

import android.app.Application

/**
 * Created by nickhuang on 2022/3/7.
 */
class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
//            System.loadLibrary("c++_shared")
            System.loadLibrary("netsdk")
            System.loadLibrary("configsdk")
            System.loadLibrary("jninetsdk")
            System.loadLibrary("gnustl_shared")
            System.loadLibrary("LechangeSDK")
            System.loadLibrary("SmartConfig")
            System.loadLibrary("SoftAPConfig")
            System.loadLibrary("GMCrypto")
            System.loadLibrary("LCOpenApiClient")
        } catch (var1: Exception) {
            System.err.println("loadLibrary Exception$var1")
        } catch (var2: Error) {
            System.err.println("loadLibrary Exception$var2")
        }
    }
}