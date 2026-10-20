package app.studyscroll.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeystoreSecretStorage(
    private val context: Context,
    private val keyAlias: String = KEY_ALIAS_DEFAULT
) : SecretStorage {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Keystore entry is not a SecretKeyEntry")
    }

    override fun saveApiKey(providerId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            removeApiKey(providerId)
            return
        }
        val secretKey = getOrCreateSecretKey()
        val encryptedBytes = AesGcmCrypto.encrypt(apiKey.toByteArray(Charsets.UTF_8), secretKey)
        val encoded = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_PREFIX + providerId, encoded).apply()
    }

    override fun getApiKey(providerId: String): String? {
        val encoded = prefs.getString(KEY_PREFIX + providerId, null) ?: return null
        return try {
            val encryptedBytes = Base64.decode(encoded, Base64.NO_WRAP)
            val secretKey = getOrCreateSecretKey()
            val decryptedBytes = AesGcmCrypto.decrypt(encryptedBytes, secretKey)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override fun hasApiKey(providerId: String): Boolean {
        return prefs.contains(KEY_PREFIX + providerId)
    }

    override fun removeApiKey(providerId: String) {
        prefs.edit().remove(KEY_PREFIX + providerId).apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "studyscroll_secure_secrets"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_DEFAULT = "studyscroll_master_key"
        private const val KEY_PREFIX = "api_key_"
    }
}
