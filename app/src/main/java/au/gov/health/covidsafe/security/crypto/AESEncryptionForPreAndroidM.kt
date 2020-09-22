package au.gov.health.covidsafe.security.crypto

import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.app.TracerApp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

/**
 * This object provides AES encryption and decryption, with the AES key stored in an RSA encrypted
 * format.  The RSA keys are in turn stored in Android KeyStore.  In this way, even if hackers get
 * the AES encrypted key from Shared Preference, they wouldn't be able to decode and use them,
 * because they can't get the RSA keys.
 *
 * This object should be used for pre Android M (API 23).  For Android M and above, use MasterKeys
 * instead to generate and store the AES key into Android KeyStore directly.
 */
object AESEncryptionForPreAndroidM {
    // keystore: for storing RSA keys
    private val ANDROID_KEY_STORE = "AndroidKeyStore"
    private val RSA_KEY_ALIAS = "RSA_KEY_ALIAS"

    private val RSA_MODE = "RSA/ECB/PKCS1Padding"
    private val AES_MODE = "AES/CBC/PKCS5Padding"

    lateinit var keyStore: KeyStore

    init {
        generateAndStoreRSAKeyPairs()
        generateEncryptAndStoreAESKey()
    }

    /**
     * Generate RSA key pairs and store them into the Android KeyStore
     */
    private fun generateAndStoreRSAKeyPairs() {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(RSA_KEY_ALIAS)) {
            // Generate a key pair for encryption
            val start: Calendar = Calendar.getInstance()
            val end: Calendar = Calendar.getInstance()
            end.add(Calendar.YEAR, 1)

            val spec = KeyPairGeneratorSpec.Builder(TracerApp.AppContext)
                    .setAlias(RSA_KEY_ALIAS)
                    .setSubject(X500Principal("CN=$RSA_KEY_ALIAS"))
                    .setSerialNumber(BigInteger.TEN)
                    .setStartDate(start.time)
                    .setEndDate(end.time)
                    .setKeySize(2048)
                    .build()

            val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    ANDROID_KEY_STORE
            )

            kpg.initialize(spec)
            kpg.generateKeyPair()
        }
    }

    /**
     * This function is used to encrypt the AES key
     */
    private fun rsaEncrypt(plainBytes: ByteArray): ByteArray {
        val privateKeyEntry = keyStore.getEntry(RSA_KEY_ALIAS, null)
                as KeyStore.PrivateKeyEntry

        val inputCipher: Cipher = Cipher.getInstance(RSA_MODE)
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)
        val outputStream = ByteArrayOutputStream()
        val cipherOutputStream = CipherOutputStream(outputStream, inputCipher)
        cipherOutputStream.write(plainBytes)
        cipherOutputStream.close()
        return outputStream.toByteArray()
    }

    /**
     * This function is used to decrypt the AES key
     */
    private fun rsaDecrypt(encrypted: ByteArray): ByteArray {
        val privateKeyEntry = keyStore.getEntry(RSA_KEY_ALIAS, null)
                as KeyStore.PrivateKeyEntry

        val outputCipher = Cipher.getInstance(RSA_MODE)
        outputCipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)

        val cipherInputStream = CipherInputStream(ByteArrayInputStream(encrypted), outputCipher)

        val values: ArrayList<Byte> = ArrayList()
        var nextByte: Int

        while (cipherInputStream.read().also { nextByte = it } != -1) {
            values.add(nextByte.toByte())
        }

        val bytes = ByteArray(values.size)

        for (i in bytes.indices) {
            bytes[i] = values[i]
        }

        return bytes
    }

    /**
     * Generate an AES key and encrypt it by RSA
     */
    private fun generateEncryptAndStoreAESKey() {
        var encodedRSAEncryptedAESKey = Preference.getEncodedRSAEncryptedAESKey(TracerApp.AppContext)

        if (encodedRSAEncryptedAESKey == null) {
            // generate an AES key and iv
            val secureRandom = SecureRandom()
            val key = ByteArray(16)
            secureRandom.nextBytes(key)

            val iv = ByteArray(16)
            secureRandom.nextBytes(iv)

            // the IV is stored into Shared Preferences
            Preference.putEncodedAESInitialisationVector(
                    TracerApp.AppContext,
                    Base64.encodeToString(
                            iv,
                            Base64.DEFAULT
                    )
            )

            // encrypt it with RSA
            val encryptedKey = rsaEncrypt(key)

            // encode the RSA encrypted AES key into Base64
            encodedRSAEncryptedAESKey = Base64.encodeToString(encryptedKey, Base64.DEFAULT)

            // store it into shared preference
            Preference.putEncodedRSAEncryptedAESKey(TracerApp.AppContext, encodedRSAEncryptedAESKey)
        }
    }

    /**
     * Get the RSA encrypted AES key from shared preferences and decrypt it
     */
    private fun getAESKeyFromSharedPreferences(): Key {
        Preference.getEncodedRSAEncryptedAESKey(TracerApp.AppContext)?.let {
            // decode base64
            val rsaEncryptedAESKey = Base64.decode(it, Base64.DEFAULT)

            // decrypt the key
            val aesKey = rsaDecrypt(rsaEncryptedAESKey)

            return SecretKeySpec(aesKey, "AES")
        }

        throw IllegalStateException("Encrypted AES Key not available in shared preferences.")
    }

    /**
     * Encrypt a string with AES
     */
    fun encrypt(plainText: String): String {
        Preference.getEncodedAESInitialisationVector(TracerApp.AppContext)?.let {
            val iv = Base64.decode(it, Base64.DEFAULT)
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getAESKeyFromSharedPreferences(),
                    IvParameterSpec(iv)
            )
            val encodedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encodedBytes, Base64.DEFAULT)
        }

        throw IllegalStateException("AES IV not available in shared preferences.")
    }

    /**
     * Decrypt a string with AES
     */
    fun decrypt(aesEncryptedText: String): String {
        Preference.getEncodedAESInitialisationVector(TracerApp.AppContext)?.let {
            val iv = Base64.decode(it, Base64.DEFAULT)

            val encryptedBytes = Base64.decode(aesEncryptedText, Base64.DEFAULT)
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getAESKeyFromSharedPreferences(),
                    IvParameterSpec(iv)
            )
            return cipher.doFinal(encryptedBytes).toString(Charsets.UTF_8)
        }

        throw IllegalStateException("AES IV not available in shared preferences.")
    }
}