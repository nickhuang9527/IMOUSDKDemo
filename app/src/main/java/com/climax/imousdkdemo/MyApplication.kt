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
    }
}