package com.example.tt01remote.net

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Generates (once) and persists a self-signed RSA certificate used to
 * identify this app to the TV during pairing, matching what the official
 * Android TV Remote app / Google TV app do.
 */
object CertUtil {

    private const val KEYSTORE_FILE = "tt01remote_client.p12"
    private const val KEYSTORE_PASSWORD = "tt01remote" // local file only, not sent over the wire
    private const val ALIAS = "tt01remote-client"

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private fun keystoreFile(context: Context): File =
        File(context.filesDir, KEYSTORE_FILE)

    fun loadOrCreateKeyStore(context: Context): KeyStore {
        val file = keystoreFile(context)
        val ks = KeyStore.getInstance("PKCS12")
        if (file.exists()) {
            file.inputStream().use { ks.load(it, KEYSTORE_PASSWORD.toCharArray()) }
            return ks
        }

        ks.load(null, null)

        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val subject = X500Name("CN=tt01remote")
        val now = System.currentTimeMillis()
        val notBefore = Date(now - 24L * 60 * 60 * 1000)
        val notAfter = Date(now + 20L * 365 * 24 * 60 * 60 * 1000)
        val serial = BigInteger.valueOf(now)

        val certBuilder = JcaX509v3CertificateBuilder(
            subject, serial, notBefore, notAfter, subject, keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        ks.setKeyEntry(ALIAS, keyPair.private, KEYSTORE_PASSWORD.toCharArray(), arrayOf(cert))
        file.outputStream().use { ks.store(it, KEYSTORE_PASSWORD.toCharArray()) }
        return ks
    }

    fun clientCertificate(context: Context): X509Certificate {
        val ks = loadOrCreateKeyStore(context)
        return ks.getCertificate(ALIAS) as X509Certificate
    }

    /**
     * Builds an SSLContext that presents our client certificate and accepts
     * whatever certificate the TV presents (trust-on-first-use — the TV's
     * cert is self-signed too, and the PIN exchange is what proves you're
     * talking to the right device, not the CA chain).
     */
    fun buildSslContext(context: Context, onServerCert: (X509Certificate) -> Unit): SSLContext {
        val ks = loadOrCreateKeyStore(context)
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, KEYSTORE_PASSWORD.toCharArray())
        val keyManagers: Array<KeyManager> = kmf.keyManagers

        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                chain?.firstOrNull()?.let { onServerCert(it) }
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(keyManagers, arrayOf<TrustManager>(trustManager), null)
        return sslContext
    }
}
