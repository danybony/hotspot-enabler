package net.bonysoft.hotspotenabler

import android.content.SharedPreferences

private const val DEVICE_KEY = "selected_device_key"

class SelectedDevicePreferences(private val sharedPreferences: SharedPreferences) {

    fun setSelectedDevice(macAddress: String?) {
        sharedPreferences.edit()
            .putString(DEVICE_KEY, macAddress)
            .apply()
    }

    fun getSelectedDeviceMacAddress(): String? = sharedPreferences.getString(DEVICE_KEY, null)
}
