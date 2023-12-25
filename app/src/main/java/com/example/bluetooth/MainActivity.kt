package com.example.bluetooth

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetooth.databinding.ActivityMainBinding
import com.example.bluetooth.utils.BluetoothChatService
import com.example.bluetooth.utils.Constants
import com.example.bluetooth.utils.Constants.Companion.MESSAGE_STATE_CHANGE
import com.example.bluetooth.utils.Constants.Companion.MULTIPLE_PERMISSION_ID
import com.example.bluetooth.utils.Constants.Companion.REQUEST_ENABLE_BT
import com.example.bluetooth.utils.Constants.Companion.appSettingOpen
import com.example.bluetooth.utils.Constants.Companion.warningPermissionDialog

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private val TAG = "myTag"

    private lateinit var binding : ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()


    private lateinit var mHandler: Handler
    private lateinit var mChatService: BluetoothChatService


    private val multiPermissionList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayListOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (checkMultiplePermission()) {
            doOperation()
        }


    }

    // Method to perform the desired operation
    private fun doOperation() {
        Toast.makeText(this, "All Permissions Granted Successfully!", Toast.LENGTH_LONG).show()

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        mHandler = Handler(Looper.getMainLooper())
        mChatService = BluetoothChatService(this, mHandler)

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
        } else {
            binding.onOfBt.setOnClickListener {
                if (!bluetoothAdapter.isEnabled) {
                    // Bluetooth is not enabled, request to enable it
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    // Bluetooth is already enabled
                    Log.d(TAG, "Bluetooth is already on")
                    Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show()

                    // Print paired devices
                    printPairedDevices()
                    // get near by devices
                    startDeviceDiscovery()
                }
            }
        }
    }

    private fun startDeviceDiscovery() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Scanning for devices...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Initialize your ListView and its adapter
        val listView: ListView = binding.listView
        val adapter = ArrayAdapter<String>(this, R.layout.simple_list_item_1)
        listView.adapter = adapter

        // Set an OnItemClickListener for the ListView
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            val device = discoveredDevices.elementAt(position)
            Toast.makeText(this, "Selected Device:\n$selectedItem", Toast.LENGTH_LONG).show()
            mChatService.start()
            mChatService.connect(device, false)
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        val discoveryStarted = bluetoothAdapter.startDiscovery()
        if (!discoveryStarted) {
            Log.e(TAG, "Failed to start device discovery")
            Toast.makeText(this, "Failed to start device discovery", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        } else {
            // Delay the toast and log messages for better user experience
            Handler().postDelayed({
                progressDialog.dismiss()
                if (discoveredDevices.isNotEmpty()) {
                    Toast.makeText(this, "Devices found!", Toast.LENGTH_SHORT).show()
                    // Print discovered devices
                    discoveredDevices.forEach { device ->
                        val deviceName = device.name ?: "Unknown Device"
                        val deviceHardwareAddress = device.address // MAC address
                        Log.d(TAG, "Discovered device: $deviceName $deviceHardwareAddress")
                        adapter.add("$deviceName\n$deviceHardwareAddress")
                    }
                } else {
                    Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "No devices found")
                }
            }, 5000) // Delayed for 5 seconds (adjust as needed)
        }
    }


    private fun printPairedDevices() {

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedDevices != null) {
            Log.d(TAG, "Number of paired devices: ${pairedDevices.size}")
            pairedDevices.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address
                Log.d(TAG, "$deviceName $deviceHardwareAddress")
            }
        } else {
            Log.d(TAG, "Paired devices list is null")
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        discoveredDevices.add(device)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show()
                    // Bluetooth is enabled, you can perform additional actions if needed
                    printPairedDevices()
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Bluetooth enabling canceled", Toast.LENGTH_SHORT).show()
                    showBluetoothCompulsoryDialog()
                }
            }
        }
    }

    private fun showBluetoothCompulsoryDialog() {
        // Display a dialog informing the user that Bluetooth is compulsory for the app
        AlertDialog.Builder(this)
            .setTitle("Bluetooth Required")
            .setMessage("Bluetooth is compulsory for this app. Please enable Bluetooth.")
            .setPositiveButton("Enable Bluetooth") { _, _ ->
                // User clicked on "Enable Bluetooth" button
                enableBluetooth()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // User clicked on "Cancel" button
                // Handle cancellation, you may choose to exit the app or take other actions
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun enableBluetooth() {
        // Request to enable Bluetooth
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        // Ensure Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled, handle accordingly
            // For example, prompt the user to enable Bluetooth
            enableBluetooth()
            return
        }
        // Connect to the selected device using BluetoothChatService
        mChatService.start()
        mChatService.connect(device, true) // Set 'false' for insecure connection, adjust as needed
    }


    private fun checkMultiplePermission() : Boolean {
        val listPermissionNeeded = arrayListOf<String>()

        for (permission in multiPermissionList) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }

        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                MULTIPLE_PERMISSION_ID
            )
            return false
        }

        return true
    }

    // Override onRequestPermissionsResult to handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Handle the result of multiple permissions request
        if (requestCode == MULTIPLE_PERMISSION_ID) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true

                // Check if all permissions are granted
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }

                // Perform the operation if all permissions are granted
                if (isGrant) {
                    doOperation()
                } else {
                    var someDenied = false

                    // Check if some permissions were permanently denied
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                            if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                                someDenied = true
                            }
                        }
                    }

                    // Show a toast and open app settings if some permissions were permanently denied
                    if (someDenied) {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                        appSettingOpen(this)
                    } else {
                        // Show a warning dialog and recheck permissions if denied
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        mChatService.stop()
    }


}