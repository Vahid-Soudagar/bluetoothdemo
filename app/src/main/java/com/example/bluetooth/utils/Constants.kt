package com.example.bluetooth.utils

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class Constants {

    companion object {
        // Constant representing the request code for multiple permissions
        const val MULTIPLE_PERMISSION_ID = 14
        const val REQUEST_ENABLE_BT = 15
        const val REQUEST_DISCOVERABLE = 16
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_TOAST = 5

        // Key names received from the BluetoothChatService Handler
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast"

        private val TAG = "BluetoothChatService"

        // Name for the SDP record when creating server socket
        val NAME_SECURE = "BluetoothChatSecure"
        val NAME_INSECURE = "BluetoothChatInsecure"

        // Unique UUID for this application
        val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

        const val STATE_NONE = 0 // we're doing nothing

        const val STATE_LISTEN = 1 // now listening for incoming connections

        const val STATE_CONNECTING = 2 // now initiating an outgoing connection

        const val STATE_CONNECTED = 3 // now connected to a remote device


        /**
         * Open app settings to allow the user to enable necessary permissions.
         * @param context The context in which the app settings will be opened.
         */
        fun appSettingOpen(context: Context) {
            // Show a toast message guiding the user to settings
            Toast.makeText(context, "Go to settings and enable permission", Toast.LENGTH_LONG)
                .show()

            // Create an intent to open the app settings for the current package
            val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            settingIntent.data = Uri.parse("package:${context.packageName}")

            // Start the intent to open app settings
            context.startActivity(settingIntent)
        }

        /**
         * Display a warning dialog indicating that all permissions are required for the app.
         * @param context The context in which the dialog will be displayed.
         * @param listener The listener to handle the button click in the dialog.
         */
        fun warningPermissionDialog(context: Context, listener: DialogInterface.OnClickListener) {
            // Create a material alert dialog with a warning message
            MaterialAlertDialogBuilder(context)
                .setMessage("All Permissions are required for this App")
                .setCancelable(false)
                .setPositiveButton("Ok", listener)
                .create()
                .show()
        }

    }
}