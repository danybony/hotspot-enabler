package net.bonysoft.hotspotenabler

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class BtReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val selectedDevicePreferences = SelectedDevicePreferences(
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        val connectedDevice =
            intent.getParcelableExtra<BluetoothDevice>("android.bluetooth.device.extra.DEVICE")
        if (selectedDevicePreferences.getSelectedDeviceMacAddress() != connectedDevice?.address) {
            return
        }
        val hotspotEnabler = createHotspotEnabler(context)
        if (intent.action == "android.bluetooth.device.action.ACL_CONNECTED") {
            hotspotEnabler.enableTethering()
        } else {
            hotspotEnabler.disableTethering()
        }
    }
}
