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
import com.example.tt01remote.proto.RemoteKeyCode

@Composable
fun RemoteScreen(
    state: UiState,
    onConnect: (String) -> Unit,
    onSubmitPin: (String) -> Unit,
    onKey: (RemoteKeyCode) -> Unit,
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
            Screen.ENTER_IP -> EnterIpForm(state, onConnect)
            Screen.ENTER_PIN -> EnterPinForm(state, onSubmitPin)
            Screen.CONNECTED -> RemotePad(onKey, onReset)
            Screen.ERROR -> ErrorView(state, onReset)
        }
    }
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
private fun RemotePad(onKey: (RemoteKeyCode) -> Unit, onReset: () -> Unit) {
    Text("TT01 リモコン", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RemoteButton("電源", onKey, RemoteKeyCode.KEYCODE_POWER)
        RemoteButton("ホーム", onKey, RemoteKeyCode.KEYCODE_HOME)
        RemoteButton("戻る", onKey, RemoteKeyCode.KEYCODE_BACK)
    }

    Spacer(Modifier.height(24.dp))

    // D-pad
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
private fun RemoteButton(label: String, onKey: (RemoteKeyCode) -> Unit, code: RemoteKeyCode) {
    Button(
        onClick = { onKey(code) },
        modifier = Modifier.size(72.dp),
    ) { Text(label) }
}

@Composable
private fun ErrorView(state: UiState, onReset: () -> Unit) {
    Text("エラー", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(16.dp))
    Text(state.message)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onReset) { Text("最初からやり直す") }
}
