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
