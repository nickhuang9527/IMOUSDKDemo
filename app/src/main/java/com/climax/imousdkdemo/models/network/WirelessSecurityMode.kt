package com.climax.imousdkdemo.models.network

import java.io.Serializable

/**
 * Created by nickhuang on 2022/3/3.
 */
enum class WirelessSecurityMode(val mode: String) : Serializable {
    None("none"),
    WEP("wep"),
    WPA("wpa");

    companion object {
        fun from(mode: String): WirelessSecurityMode {
            return values().firstOrNull { it.mode == mode } ?: None
        }

        fun fromCapabilities(capabilities: String): WirelessSecurityMode {
            return values().firstOrNull { capabilities.toLowerCase().contains(it.mode) } ?: None
        }

        val allCases: ArrayList<WirelessSecurityMode>
            get() = arrayListOf(None, WEP, WPA)
    }
}