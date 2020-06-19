package au.gov.health.covidsafe.streetpass.persistence

import android.util.Base64
import au.gov.health.covidsafe.BuildConfig
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptionKeys(val ephPubKey: ByteArray, val aesKey: SecretKey, val macKey: SecretKey, val nonce: ByteArray)

object Encryption {

    const val KEY_GEN_TIME_DELTA = 450000  // 7.5 minutes

    private val TAG = this.javaClass.simpleName

    // Get the server's ECDH public key
    private fun readKey(): PublicKey {

        val decodedKey: ByteArray = Base64.decode(BuildConfig.ENCRYPTION_PUBLIC_KEY, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decodedKey)
        return KeyFactory.getInstance("EC").generatePublic(keySpec)
    }

    // Compute a SHA-256 hash
    private fun hash(content: ByteArray): ByteArray {
        val hash: MessageDigest = MessageDigest.getInstance("SHA-256")
        hash.update(content)
        return hash.digest()
    }

    // Generate ECDH P256 key-pair
    private fun makeECKeys(): KeyPair {
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }

    // Convert an ECDH public key coordinate to R
    private fun getPublicKey(kp: KeyPair): ByteArray {
        val key: PublicKey = kp.public
        if (key is ECPublicKey) {
            if (key.w.affineX == BigInteger.ZERO && key.w.affineY == BigInteger.ZERO) {
                return ByteArray(1)
            }
            var x: ByteArray = key.w.affineX.toByteArray()
            if (x.size == 33 && x[0] == 0.toByte()) {
                x = x.sliceArray(1..32)
            } else if (x.size >= 33) {
                throw IllegalStateException("Unexpected x coordinate in ECDH public key")
            } else if (x.size < 32) {
                x = ByteArray(32 - x.size).plus(x)
            }
            // Using P256 so q = p, p (mod 2) = 1
            // Compression flag is 0x2 when y (mod 2) = 0 and 0x3 when y (mod 2) = 1
            val flag: Int = 2 or (key.w.affineY and 1.toBigInteger()).toInt()
            val fba: ByteArray = byteArrayOf(flag.toByte())
            return fba.plus(x)
        }
        throw IllegalStateException("Key pair does not contain an ECDH public key")
    }

    // Perform a key agreement against the server's long-term ECDH public key
    private fun doKeyAgreement(kp: KeyPair): KeyAgreement {
        val ka: KeyAgreement = KeyAgreement.getInstance("ECDH")
        ka.init(kp.private)
        ka.doPhase(serverPubKey, true)
        return ka
    }

    // Compute a message authentication code for the given data
    private fun computeMAC(key: SecretKey, data: ByteArray): ByteArray {
        val mac: Mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return mac.doFinal(data).sliceArray(0..15)
    }

    // Convert an int to a 2-byte big-endian ByteArray
    private fun counterBytes(counter: Int): ByteArray {
        return byteArrayOf(((counter and 0xFF00) shr 8).toByte(), (counter and 0x00FF).toByte())
    }

    // Create a new cipher instance for symmetric crypt
    private fun makeSymCipher(): Cipher {
        return Cipher.getInstance("AES/CBC/PKCS5Padding")
    }

    private val NONCE_PADDING = ByteArray(14) { 0x0E.toByte() }
    private val serverPubKey: PublicKey = readKey()

    private var cachedEphPubKey: ByteArray? = null
    private var cachedAesKey: SecretKey? = null
    private var cachedMacKey: SecretKey? = null
    private var keyGenTime: Long = Long.MIN_VALUE
    private var counter: Int = 0

    private fun generateKeys() {

        // ECDH
        val kp: KeyPair = makeECKeys()
        val ka: KeyAgreement = doKeyAgreement(kp)
        val ephSecret: ByteArray = ka.generateSecret()
        cachedEphPubKey = getPublicKey(kp)

        // KDF
        val derivedKey: ByteArray = hash(ephSecret)
        cachedAesKey = SecretKeySpec(derivedKey.sliceArray(0..15), "AES")
        cachedMacKey = SecretKeySpec(derivedKey.sliceArray(16..31), "HmacSHA256")

    }

    fun encryptPayload(data: ByteArray): String {

        val keys = encryptionKeys()

        val prefix: ByteArray = keys.ephPubKey.plus(keys.nonce)

        // Encrypt
        // IV = AES(ctr, iv=null), AES(plaintext, iv=IV) === AES(ctr_with_padding || plaintext, iv=null)
        // Using the latter construction to reduce key expansions
        val ivParams = IvParameterSpec(ByteArray(16))  // null IV
        val symCipher: Cipher = makeSymCipher()
        symCipher.init(Cipher.ENCRYPT_MODE, keys.aesKey, ivParams)
        val ciphertextWithIV: ByteArray = symCipher.doFinal(keys.nonce.plus(NONCE_PADDING).plus(data))

        // MAC
        val size: Int = ciphertextWithIV.size - 1
        val blob: ByteArray = prefix.plus(ciphertextWithIV.sliceArray(16..size))
        val mac: ByteArray = computeMAC(keys.macKey, blob)

        return Base64.encodeToString(blob.plus(mac), Base64.DEFAULT)
    }



    @Synchronized
    private fun encryptionKeys(): EncryptionKeys {
        if (keyGenTime <= System.currentTimeMillis() - KEY_GEN_TIME_DELTA || counter >= 65535) {
            generateKeys()
            keyGenTime = System.currentTimeMillis()
            counter = 0
        } else {
            counter++
        }
        return EncryptionKeys(cachedEphPubKey!!, cachedAesKey!!, cachedMacKey!!, counterBytes(counter))
    }
}