package net.bonysoft.hotspotenabler

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import java.lang.reflect.Method

class NougatHotspotEnabler(private val context: Context) : HotspotEnabler {

    override fun enableTethering() {
        setTetheringStatus(context, true)
    }

    override fun disableTethering() {
        setTetheringStatus(context, false)
    }

    private fun setTetheringStatus(context: Context, enabled: Boolean) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        try {
            val method: Method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(wifiManager, null, enabled) // true to enable, false to disable
        } catch (e: NoSuchMethodException) {
            // WiFi tethering is blocked.
        }
    }

}
