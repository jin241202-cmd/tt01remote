package com.example.tt01remote.net

import java.io.InputStream
import java.io.OutputStream
import com.google.protobuf.CodedOutputStream

/**
 * Every message on the wire is: <varint length prefix><serialized protobuf bytes>.
 */
object MessageFraming {

    @Throws(java.io.IOException::class)
    fun writeMessage(out: OutputStream, payload: ByteArray) {
        // Varint-encode the length ourselves (matches protobuf's varint format).
        val lengthBuf = java.io.ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(lengthBuf)
        cos.writeUInt32NoTag(payload.size)
        cos.flush()
        out.write(lengthBuf.toByteArray())
        out.write(payload)
        out.flush()
    }

    @Throws(java.io.IOException::class)
    fun readMessage(input: InputStream): ByteArray {
        var length = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b == -1) throw java.io.EOFException("Stream closed while reading length")
            length = length or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        val buf = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(buf, read, length - read)
            if (n == -1) throw java.io.EOFException("Stream closed while reading payload")
            read += n
        }
        return buf
    }
}
