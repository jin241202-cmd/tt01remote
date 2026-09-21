package com.example.tt01remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tt01remote.net.PairingClient
import com.example.tt01remote.net.RemoteClient
import com.example.tt01remote.proto.RemoteKeyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Screen { ENTER_IP, ENTER_PIN, CONNECTED, ERROR }

data class UiState(
    val screen: Screen = Screen.ENTER_IP,
    val host: String = "",
    val message: String = "",
    val busy: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var pairingClient: PairingClient? = null
    private var remoteClient: RemoteClient? = null

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

    fun reset() {
        pairingClient?.close()
        remoteClient?.close()
        pairingClient = null
        remoteClient = null
        _state.value = UiState()
    }

    override fun onCleared() {
        pairingClient?.close()
        remoteClient?.close()
        super.onCleared()
    }
}
