package net.bonysoft.hotspotenabler

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager


const val MY_PERMISSIONS_MANAGE_WRITE_SETTINGS = 100
const val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 69

class MainActivity : AppCompatActivity() {

    private val selectedDevicePreferences: SelectedDevicePreferences by lazy {
        SelectedDevicePreferences(
            PreferenceManager.getDefaultSharedPreferences(this)
        )
    }
    private var mLocationPermission = false
    private var mSettingPermission = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.version).text =
            getString(R.string.footer_version_format, BuildConfig.VERSION_NAME)
        val spinner = findViewById<Spinner>(R.id.devices_spinner)
        val bluetoothDevices = (BluetoothAdapter.getDefaultAdapter()?.bondedDevices
            ?.map { BluetoothDeviceNameWrapper(it) }
            ?.sortedBy { it.toString() }
            ?: emptyList())
            .toMutableList()
            .apply { add(0, BluetoothDeviceNameWrapper.NoneSelected) }
            .toTypedArray()

        val adapter = ArrayAdapter<BluetoothDeviceNameWrapper>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            bluetoothDevices
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setPromptId(R.string.spinner_prompt)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedDevice =
                    parent?.getItemAtPosition(position) as BluetoothDeviceNameWrapper
                selectedDevicePreferences.setSelectedDevice(selectedDevice.address)
            }

        }
        selectedDevicePreferences.getSelectedDeviceMacAddress()?.let { address ->
            spinner.setSelection(bluetoothDevices.indexOfFirst {
                it.address == address
            })
        }
        settingPermission()
        locationsPermission()
    }

    private fun settingPermission() {
        mSettingPermission = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(applicationContext)) {
                mSettingPermission = false
                val intent =
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
                startActivityForResult(intent, MY_PERMISSIONS_MANAGE_WRITE_SETTINGS)
            }
        }
    }


    private fun locationsPermission() {
        mLocationPermission = true
        if (ContextCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermission = false
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == MY_PERMISSIONS_MANAGE_WRITE_SETTINGS) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                mSettingPermission = true
                if (!mLocationPermission) locationsPermission()
            } else {
                settingPermission()
            }
        }

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                mLocationPermission = true
                if (!mSettingPermission) settingPermission()
            } else {
                locationsPermission()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

open class BluetoothDeviceNameWrapper constructor(private val bluetoothDevice: BluetoothDevice?) {
    val address: String? = bluetoothDevice?.address

    override fun toString(): String = bluetoothDevice?.name ?: "Nessuno (disattivato)"

    object NoneSelected : BluetoothDeviceNameWrapper(null)
}

