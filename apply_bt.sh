#!/data/data/com.termux/files/usr/bin/bash
set -e
cd ~/tt01remote
mkdir -p app/src/main/java/com/example/tt01remote/hid

cat > app/build.gradle.kts << 'INNEREOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
}

android {
    namespace = "com.example.tt01remote"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tt01remote"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/**"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    implementation("com.google.protobuf:protobuf-javalite:3.25.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
INNEREOF

cat > app/src/main/AndroidManifest.xml << 'INNEREOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- Bluetooth: BLUETOOTH/BLUETOOTH_ADMIN for pre-Android 12, BLUETOOTH_CONNECT for 12+ -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:theme="@style/Theme.TT01Remote"
        android:usesCleartextTraffic="false">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
INNEREOF

cat > app/src/main/java/com/example/tt01remote/hid/HidDescriptor.kt << 'INNEREOF'
package com.example.tt01remote.hid

/**
 * Raw USB/Bluetooth HID Report Descriptor describing two report types this
 * app can send: a standard keyboard report (for D-pad / select / back,
 * which Android's own input stack maps to DPAD/BACK system-wide) and a
 * Consumer Control report (for power / volume / mute / home, the usage
 * page real TV remotes use for those functions).
 *
 * Reference: USB HID Usage Tables (usb.org). Two Report IDs share one
 * descriptor so a single Bluetooth HID Device registration can send both
 * kinds of button presses.
 */
object HidDescriptor {

    const val REPORT_ID_KEYBOARD = 1
    const val REPORT_ID_CONSUMER = 2

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ---- Keyboard collection (Report ID 1) ----
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x06,             // Usage (Keyboard)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(), //   Report ID (1)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(),    //   Usage Minimum (224)
        0x29, 0xE7.toByte(),    //   Usage Maximum (231)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95.toByte(), 0x08,    //   Report Count (8)
        0x81.toByte(), 0x02,    //   Input (Data,Var,Abs) - modifier byte
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x75, 0x08,             //   Report Size (8)
        0x81.toByte(), 0x01,    //   Input (Cnst) - reserved byte
        0x95.toByte(), 0x06,    //   Report Count (6)
        0x75, 0x08,             //   Report Size (8)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x65,             //   Logical Maximum (101)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,             //   Usage Minimum (0)
        0x29, 0x65,             //   Usage Maximum (101)
        0x81.toByte(), 0x00,    //   Input (Data,Ary,Abs) - keycodes
        0xC0.toByte(),          // End Collection

        // ---- Consumer Control collection (Report ID 2) ----
        0x05, 0x0C,             // Usage Page (Consumer)
        0x09, 0x01,             // Usage (Consumer Control)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(), //   Report ID (2)
        0x15, 0x00,             //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03, //   Logical Maximum (1023)
        0x19, 0x00,             //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03, //   Usage Maximum (1023)
        0x75, 0x10,             //   Report Size (16)
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x81.toByte(), 0x00,    //   Input (Data,Ary,Abs)
        0xC0.toByte()           // End Collection
    )

    // Keyboard/Keypad usage page (0x07) — Android maps these to DPAD/BACK/ENTER system-wide.
    object Key {
        const val UP = 0x52
        const val DOWN = 0x51
        const val LEFT = 0x50
        const val RIGHT = 0x4F
        const val SELECT = 0x28 // Enter
        const val BACK = 0x29   // Escape
    }

    // Consumer usage page (0x0C) — standard remote-control style functions.
    object Consumer {
        const val POWER = 0x0030
        const val HOME = 0x0223
        const val MENU = 0x0040
        const val VOLUME_UP = 0x00E9
        const val VOLUME_DOWN = 0x00EA
        const val MUTE = 0x00E2
    }
}
INNEREOF

cat > app/src/main/java/com/example/tt01remote/hid/HidRemoteController.kt << 'INNEREOF'
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
INNEREOF

cat > app/src/main/java/com/example/tt01remote/MainViewModel.kt << 'INNEREOF'
package com.example.tt01remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tt01remote.hid.HidRemoteController
import com.example.tt01remote.hid.HidState
import com.example.tt01remote.net.PairingClient
import com.example.tt01remote.net.RemoteClient
import com.example.tt01remote.proto.RemoteKeyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Screen {
    MODE_SELECT, ENTER_IP, ENTER_PIN, CONNECTED,
    BT_WAITING, BT_CONNECTED, ERROR
}

data class UiState(
    val screen: Screen = Screen.MODE_SELECT,
    val host: String = "",
    val message: String = "",
    val busy: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var pairingClient: PairingClient? = null
    private var remoteClient: RemoteClient? = null
    private var hidController: HidRemoteController? = null

    // ---- Mode select ----

    fun chooseWifiMode() {
        _state.value = _state.value.copy(screen = Screen.ENTER_IP, message = "")
    }

    /** Call only after BLUETOOTH_CONNECT has been granted. */
    fun chooseBluetoothMode() {
        val controller = HidRemoteController(getApplication())
        hidController = controller
        controller.onStateChanged = { hidState ->
            when (hidState) {
                HidState.WAITING_FOR_PAIRING -> _state.value = _state.value.copy(
                    screen = Screen.BT_WAITING, busy = false,
                    message = "TT01のBluetooth設定から「TT01 Remote」を選んでペアリングしてください"
                )
                HidState.CONNECTED -> _state.value = _state.value.copy(
                    screen = Screen.BT_CONNECTED, busy = false, message = ""
                )
                HidState.UNAVAILABLE -> _state.value = _state.value.copy(
                    screen = Screen.ERROR, busy = false,
                    message = "Bluetoothを利用できません。端末のBluetoothがONになっているか確認してください。"
                )
            }
        }
        _state.value = _state.value.copy(screen = Screen.BT_WAITING, busy = true, message = "")
        controller.start()
    }

    // ---- Wi-Fi / network flow ----

    fun startPairing(host: String) {
        _state.value = _state.value.copy(busy = true, message = "", host = host)
        viewModelScope.launch {
            try {
                val client = PairingClient(getApplication(), host)
                pairingClient = client
                client.connectAndRequestPin()
                _state.value = _state.value.copy(
                    screen = Screen.ENTER_PIN, busy = false,
                    message = "TT01の画面に表示された数字を入力してください"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    screen = Screen.ERROR, busy = false,
                    message = "接続できませんでした: ${e.message}\nIPアドレスと同一Wi-Fiを確認してください。"
                )
            }
        }
    }

    fun submitPin(pin: String) {
        val client = pairingClient ?: return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            val ok = runCatching { client.submitPin(pin) }.getOrDefault(false)
            if (ok) {
                connectRemote()
            } else {
                _state.value = _state.value.copy(
                    screen = Screen.ERROR, busy = false,
                    message = "ペアリングに失敗しました。PINを再確認するか、最初からやり直してください。"
                )
            }
        }
    }

    private fun connectRemote() {
        viewModelScope.launch {
            val client = RemoteClient(getApplication(), _state.value.host)
            client.onDisconnected = {
                _state.value = _state.value.copy(
                    screen = Screen.ERROR, message = "TT01との接続が切れました。"
                )
            }
            val ok = client.connect(viewModelScope)
            remoteClient = client
            _state.value = _state.value.copy(
                screen = if (ok) Screen.CONNECTED else Screen.ERROR,
                busy = false,
                message = if (ok) "" else "リモート接続に失敗しました。"
            )
        }
    }

    fun sendKey(keyCode: RemoteKeyCode) {
        viewModelScope.launch { remoteClient?.sendKey(keyCode) }
    }

    // ---- Bluetooth HID button actions ----

    fun sendBtKey(usageId: Int) = hidController?.sendKey(usageId)
    fun sendBtConsumer(usageId: Int) = hidController?.sendConsumer(usageId)

    fun reset() {
        pairingClient?.close()
        remoteClient?.close()
        hidController?.stop()
        pairingClient = null
        remoteClient = null
        hidController = null
        _state.value = UiState()
    }

    override fun onCleared() {
        pairingClient?.close()
        remoteClient?.close()
        hidController?.stop()
        super.onCleared()
    }
}
INNEREOF

cat > app/src/main/java/com/example/tt01remote/MainActivity.kt << 'INNEREOF'
package com.example.tt01remote

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.tt01remote.ui.RemoteScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val state by viewModel.state.collectAsState()

                    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) viewModel.chooseBluetoothMode()
                    }

                    RemoteScreen(
                        state = state,
                        onChooseWifi = viewModel::chooseWifiMode,
                        onChooseBluetooth = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                viewModel.chooseBluetoothMode()
                            }
                        },
                        onConnect = viewModel::startPairing,
                        onSubmitPin = viewModel::submitPin,
                        onKey = viewModel::sendKey,
                        onBtKey = viewModel::sendBtKey,
                        onBtConsumer = viewModel::sendBtConsumer,
                        onReset = viewModel::reset,
                    )
                }
            }
        }
    }
}
INNEREOF

cat > app/src/main/java/com/example/tt01remote/ui/RemoteScreen.kt << 'INNEREOF'
package com.example.tt01remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tt01remote.Screen
import com.example.tt01remote.UiState
import com.example.tt01remote.hid.HidDescriptor
import com.example.tt01remote.proto.RemoteKeyCode

@Composable
fun RemoteScreen(
    state: UiState,
    onChooseWifi: () -> Unit,
    onChooseBluetooth: () -> Unit,
    onConnect: (String) -> Unit,
    onSubmitPin: (String) -> Unit,
    onKey: (RemoteKeyCode) -> Unit,
    onBtKey: (Int) -> Unit,
    onBtConsumer: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.screen) {
            Screen.MODE_SELECT -> ModeSelect(onChooseWifi, onChooseBluetooth)
            Screen.ENTER_IP -> EnterIpForm(state, onConnect)
            Screen.ENTER_PIN -> EnterPinForm(state, onSubmitPin)
            Screen.CONNECTED -> RemotePad(onKey, onReset)
            Screen.BT_WAITING -> BtWaiting(state, onReset)
            Screen.BT_CONNECTED -> BtRemotePad(onBtKey, onBtConsumer, onReset)
            Screen.ERROR -> ErrorView(state, onReset)
        }
    }
}

@Composable
private fun ModeSelect(onChooseWifi: () -> Unit, onChooseBluetooth: () -> Unit) {
    Text("接続方法を選択", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onChooseWifi, modifier = Modifier.fillMaxWidth()) {
        Text("Wi-Fi経由で接続")
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onChooseBluetooth, modifier = Modifier.fillMaxWidth()) {
        Text("Bluetooth経由で接続")
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "docomoのペアリングサービス終了により、Wi-Fi経由はTT01では動作しない場合があります。Bluetoothの方が確実です。",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun EnterIpForm(state: UiState, onConnect: (String) -> Unit) {
    var ip by remember { mutableStateOf("") }
    Text("TT01のIPアドレスを入力", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        "設定 > ネットワークとインターネット > Wi-Fi の詳細で確認できます",
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = ip,
        onValueChange = { ip = it },
        label = { Text("例: 192.168.1.42") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    if (state.busy) {
        CircularProgressIndicator()
    } else {
        Button(onClick = { onConnect(ip.trim()) }, enabled = ip.isNotBlank()) {
            Text("ペアリング開始")
        }
    }
    if (state.message.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        Text(state.message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EnterPinForm(state: UiState, onSubmitPin: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    Text("TT01の画面に表示されたコードを入力", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = pin,
        onValueChange = { pin = it },
        label = { Text("6桁のコード") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    if (state.busy) {
        CircularProgressIndicator()
    } else {
        Button(onClick = { onSubmitPin(pin.trim()) }, enabled = pin.isNotBlank()) {
            Text("確定")
        }
    }
}

@Composable
private fun BtWaiting(state: UiState, onReset: () -> Unit) {
    Text("Bluetoothペアリング待ち", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    if (state.busy) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
    }
    Text(
        "TT01側の「設定 > リモコンとアクセサリ」(または Bluetooth設定)を開き、\n" +
            "新しいデバイスを追加 → 「TT01 Remote」を選んでペアリングしてください。",
        style = MaterialTheme.typography.bodyMedium
    )
    if (state.message.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        Text(state.message, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onReset) { Text("戻る") }
}

@Composable
private fun RemotePad(onKey: (RemoteKeyCode) -> Unit, onReset: () -> Unit) {
    Text("TT01 リモコン (Wi-Fi)", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RemoteButton("電源", onKey, RemoteKeyCode.KEYCODE_POWER)
        RemoteButton("ホーム", onKey, RemoteKeyCode.KEYCODE_HOME)
        RemoteButton("戻る", onKey, RemoteKeyCode.KEYCODE_BACK)
    }

    Spacer(Modifier.height(24.dp))

    RemoteButton("▲", onKey, RemoteKeyCode.KEYCODE_DPAD_UP)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RemoteButton("◀", onKey, RemoteKeyCode.KEYCODE_DPAD_LEFT)
        RemoteButton("決定", onKey, RemoteKeyCode.KEYCODE_DPAD_CENTER)
        RemoteButton("▶", onKey, RemoteKeyCode.KEYCODE_DPAD_RIGHT)
    }
    RemoteButton("▼", onKey, RemoteKeyCode.KEYCODE_DPAD_DOWN)

    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RemoteButton("音量+", onKey, RemoteKeyCode.KEYCODE_VOLUME_UP)
        RemoteButton("消音", onKey, RemoteKeyCode.KEYCODE_MUTE)
        RemoteButton("音量-", onKey, RemoteKeyCode.KEYCODE_VOLUME_DOWN)
    }

    Spacer(Modifier.height(32.dp))
    OutlinedButton(onClick = onReset) { Text("接続をやり直す") }
}

@Composable
private fun BtRemotePad(onBtKey: (Int) -> Unit, onBtConsumer: (Int) -> Unit, onReset: () -> Unit) {
    Text("TT01 リモコン (Bluetooth)", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ConsumerButton("電源", onBtConsumer, HidDescriptor.Consumer.POWER)
        ConsumerButton("ホーム", onBtConsumer, HidDescriptor.Consumer.HOME)
        KeyButton("戻る", onBtKey, HidDescriptor.Key.BACK)
    }

    Spacer(Modifier.height(24.dp))

    KeyButton("▲", onBtKey, HidDescriptor.Key.UP)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        KeyButton("◀", onBtKey, HidDescriptor.Key.LEFT)
        KeyButton("決定", onBtKey, HidDescriptor.Key.SELECT)
        KeyButton("▶", onBtKey, HidDescriptor.Key.RIGHT)
    }
    KeyButton("▼", onBtKey, HidDescriptor.Key.DOWN)

    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ConsumerButton("音量+", onBtConsumer, HidDescriptor.Consumer.VOLUME_UP)
        ConsumerButton("消音", onBtConsumer, HidDescriptor.Consumer.MUTE)
        ConsumerButton("音量-", onBtConsumer, HidDescriptor.Consumer.VOLUME_DOWN)
    }

    Spacer(Modifier.height(32.dp))
    OutlinedButton(onClick = onReset) { Text("接続をやり直す") }
}

@Composable
private fun RemoteButton(label: String, onKey: (RemoteKeyCode) -> Unit, code: RemoteKeyCode) {
    Button(onClick = { onKey(code) }, modifier = Modifier.size(72.dp)) { Text(label) }
}

@Composable
private fun KeyButton(label: String, onKey: (Int) -> Unit, usageId: Int) {
    Button(onClick = { onKey(usageId) }, modifier = Modifier.size(72.dp)) { Text(label) }
}

@Composable
private fun ConsumerButton(label: String, onConsumer: (Int) -> Unit, usageId: Int) {
    Button(onClick = { onConsumer(usageId) }, modifier = Modifier.size(72.dp)) { Text(label) }
}

@Composable
private fun ErrorView(state: UiState, onReset: () -> Unit) {
    Text("エラー", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(16.dp))
    Text(state.message)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onReset) { Text("最初からやり直す") }
}
INNEREOF

git add -A
git commit -m "Add Bluetooth HID remote mode"
git push
