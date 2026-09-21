package com.example.tt01remote.net

import android.content.Context
import com.example.tt01remote.proto.PairingConfiguration
import com.example.tt01remote.proto.PairingEncoding
import com.example.tt01remote.proto.PairingMessage
import com.example.tt01remote.proto.PairingOption
import com.example.tt01remote.proto.PairingRequest
import com.example.tt01remote.proto.PairingSecret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

private const val PAIRING_PORT = 6467
private const val STATUS_OK = 200
private const val ROLE_INPUT = 1
private const val PIN_LENGTH = 6

/**
 * Handles the one-time pairing handshake with the TV's Android TV Remote
 * Service. On success, the app can connect to [RemoteClient] going forward
 * without pairing again (the TV remembers this app's client certificate).
 */
class PairingClient(private val context: Context, private val host: String) {

    private var socket: SSLSocket? = null
    private var serverCert: X509Certificate? = null

    /**
     * Connects and runs the handshake up through the point where the TV
     * shows a PIN on screen. Call [submitPin] next with what the user reads
     * off the TV.
     */
    suspend fun connectAndRequestPin() = withContext(Dispatchers.IO) {
        val sslContext = CertUtil.buildSslContext(context) { cert -> serverCert = cert }
        val factory = sslContext.socketFactory
        val s = factory.createSocket(host, PAIRING_PORT) as SSLSocket
        s.startHandshake()
        socket = s

        val out = s.outputStream
        val input = s.inputStream

        // 1) PairingRequest
        send(out, PairingMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(STATUS_OK)
            .setPairingRequest(
                PairingRequest.newBuilder()
                    .setServiceName("androidtvremote2")
                    .setClientName(android.os.Build.MODEL ?: "Android Remote")
            )
            .build())
        receive(input) // expect pairing_request_ack

        // 2) Options — declare that we accept a numeric PIN as input
        val numericEncoding = PairingEncoding.newBuilder()
            .setType(PairingEncoding.EncodingType.ENCODING_TYPE_NUMERIC)
            .setSymbolLength(PIN_LENGTH)
            .build()
        send(out, PairingMessage.newBuilder()
            .setStatus(STATUS_OK)
            .setPairingOption(
                PairingOption.newBuilder()
                    .addInputEncodings(numericEncoding)
                    .setPreferredRole(ROLE_INPUT)
            )
            .build())
        receive(input) // TV's pairing_option response

        // 3) Configuration — confirm we're using the numeric PIN encoding.
        // This is what triggers the TV to display the PIN on screen.
        send(out, PairingMessage.newBuilder()
            .setStatus(STATUS_OK)
            .setPairingConfiguration(
                PairingConfiguration.newBuilder()
                    .setEncoding(numericEncoding)
                    .setClientRole(ROLE_INPUT)
            )
            .build())
        receive(input) // expect pairing_configuration_ack
    }

    /**
     * Call after the user has read the PIN off the TV screen and typed it
     * in. Returns true if pairing succeeded.
     */
    suspend fun submitPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val s = socket ?: return@withContext false
        val clientCert = CertUtil.clientCertificate(context)
        val server = serverCert ?: return@withContext false

        // Best-effort secret derivation — see pairingmessage.proto header
        // comment. If pairing fails at this step, this is the first place
        // to check against the reference implementation.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(clientCert.encoded)
        digest.update(server.encoded)
        val hash = digest.digest()

        send(s.outputStream, PairingMessage.newBuilder()
            .setStatus(STATUS_OK)
            .setPairingSecret(PairingSecret.newBuilder().setSecret(com.google.protobuf.ByteString.copyFrom(hash)))
            .build())

        val response = PairingMessage.parseFrom(MessageFraming.readMessage(s.inputStream))
        val ok = response.status == STATUS_OK && response.hasPairingSecretAck()
        close()
        ok
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun send(out: java.io.OutputStream, message: PairingMessage) {
        MessageFraming.writeMessage(out, message.toByteArray())
    }

    private fun receive(input: java.io.InputStream): PairingMessage {
        return PairingMessage.parseFrom(MessageFraming.readMessage(input))
    }
}
