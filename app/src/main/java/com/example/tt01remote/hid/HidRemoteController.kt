package com.example.tt01remote.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import java.util.concurrent.Executor

enum class HidState { UNAVAILABLE, WAITING_FOR_PAIRING, CONNECTED }

/**
 * Registers this app as a Bluetooth HID Device (the same OS-level profile a
 * physical Bluetooth TV remote or keyboard uses) so the TV's own Bluetooth
 * stack handles discovery/pairing — no custom protocol, no dependency on
 * docomo's app-level pairing service.
 */
@SuppressLint("MissingPermission") // permission presence is checked by the caller before any method here is used
class HidRemoteController(private val context: Context) {

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    var onStateChanged: ((HidState) -> Unit)? = null

    private val directExecutor = Executor { command -> command.run() }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
            connectedDevice = null
            onStateChanged?.invoke(HidState.UNAVAILABLE)
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                onStateChanged?.invoke(HidState.WAITING_FOR_PAIRING)
            } else {
                onStateChanged?.invoke(HidState.UNAVAILABLE)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    onStateChanged?.invoke(HidState.CONNECTED)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device?.address) connectedDevice = null
                    onStateChanged?.invoke(HidState.WAITING_FOR_PAIRING)
                }
            }
        }
    }

    /** Starts advertising this phone as a pairable Bluetooth HID remote. */
    fun start() {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            onStateChanged?.invoke(HidState.UNAVAILABLE)
            return
        }
        adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
    }

    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "TT01 Remote",
            "TV remote control",
            "tt01remote",
            BluetoothHidDevice.SUBCLASS2_REMOTE_CONTROL,
            HidDescriptor.DESCRIPTOR
        )
        hidDevice?.registerApp(sdp, null, null, directExecutor, callback)
    }

    fun sendKey(usageId: Int) {
        val device = connectedDevice ?: return
        val press = byteArrayOf(0, 0, usageId.toByte(), 0, 0, 0, 0, 0)
        val release = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        hidDevice?.sendReport(device, HidDescriptor.REPORT_ID_KEYBOARD, press)
        hidDevice?.sendReport(device, HidDescriptor.REPORT_ID_KEYBOARD, release)
    }

    fun sendConsumer(usageId: Int) {
        val device = connectedDevice ?: return
        val press = byteArrayOf((usageId and 0xFF).toByte(), ((usageId shr 8) and 0xFF).toByte())
        val release = byteArrayOf(0, 0)
        hidDevice?.sendReport(device, HidDescriptor.REPORT_ID_CONSUMER, press)
        hidDevice?.sendReport(device, HidDescriptor.REPORT_ID_CONSUMER, release)
    }

    fun stop() {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        hidDevice?.let {
            it.unregisterApp()
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        hidDevice = null
        connectedDevice = null
    }
}
