package com.example.tt01remote.net

import android.content.Context
import com.example.tt01remote.proto.RemoteConfigure
import com.example.tt01remote.proto.RemoteDeviceInfo
import com.example.tt01remote.proto.RemoteDirection
import com.example.tt01remote.proto.RemoteKeyCode
import com.example.tt01remote.proto.RemoteKeyInject
import com.example.tt01remote.proto.RemoteMessage
import com.example.tt01remote.proto.RemotePingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.net.ssl.SSLSocket

private const val REMOTE_PORT = 6466

/**
 * Maintains the always-on connection used to send key presses to the TV.
 * Must be called after pairing has succeeded once (the TV recognizes this
 * app's client certificate from then on, no PIN needed).
 */
class RemoteClient(private val context: Context, private val host: String) {

    private var socket: SSLSocket? = null
    private var readerJob: Job? = null
    var onDisconnected: (() -> Unit)? = null

    suspend fun connect(scope: CoroutineScope): Boolean = withContext(Dispatchers.IO) {
        try {
            val sslContext = CertUtil.buildSslContext(context) { }
            val s = sslContext.socketFactory.createSocket(host, REMOTE_PORT) as SSLSocket
            s.startHandshake()
            socket = s

            readerJob = scope.launch(Dispatchers.IO) { readLoop(s) }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readLoop(s: SSLSocket) {
        try {
            while (!s.isClosed) {
                val bytes = MessageFraming.readMessage(s.inputStream)
                val msg = RemoteMessage.parseFrom(bytes)
                when {
                    msg.hasRemoteConfigure() -> {
                        // TV asking us to identify ourselves — reply with device info.
                        val reply = RemoteMessage.newBuilder()
                            .setRemoteConfigure(
                                RemoteConfigure.newBuilder()
                                    .setCode1(622)
                                    .setDeviceInfo(
                                        RemoteDeviceInfo.newBuilder()
                                            .setModel("TT01 Remote")
                                            .setVendor("tt01remote")
                                            .setPackageName(context.packageName)
                                            .setAppVersion("1.0")
                                    )
                            )
                            .build()
                        MessageFraming.writeMessage(s.outputStream, reply.toByteArray())
                    }
                    msg.hasRemotePingRequest() -> {
                        val reply = RemoteMessage.newBuilder()
                            .setRemotePingResponse(
                                RemotePingResponse.newBuilder().setVal1(msg.remotePingRequest.val1)
                            )
                            .build()
                        MessageFraming.writeMessage(s.outputStream, reply.toByteArray())
                    }
                    else -> { /* volume/app-info updates — ignored for now */ }
                }
            }
        } catch (e: Exception) {
            // Connection dropped.
        } finally {
            onDisconnected?.invoke()
        }
    }

    suspend fun sendKey(keyCode: RemoteKeyCode) = withContext(Dispatchers.IO) {
        val s = socket ?: return@withContext
        try {
            val msg = RemoteMessage.newBuilder()
                .setRemoteKeyInject(
                    RemoteKeyInject.newBuilder()
                        .setKeyCode(keyCode)
                        .setDirection(RemoteDirection.SHORT)
                )
                .build()
            MessageFraming.writeMessage(s.outputStream, msg.toByteArray())
        } catch (e: Exception) {
            onDisconnected?.invoke()
        }
    }

    fun close() {
        readerJob?.cancel()
        runCatching { socket?.close() }
        socket = null
    }
}
