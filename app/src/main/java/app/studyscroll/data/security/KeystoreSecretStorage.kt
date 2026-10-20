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

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        try {
            if (keyStore.containsAlias(keyAlias)) {
                val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
                if (entry?.secretKey != null) {
                    return entry.secretKey
                }
                // If entry exists but key is null/corrupt, remove it
                runCatching { keyStore.deleteEntry(keyAlias) }
            }
        } catch (e: Exception) {
            runCatching { keyStore.deleteEntry(keyAlias) }
        }

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

    override fun saveApiKey(providerId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            removeApiKey(providerId)
            return
        }

        try {
            val secretKey = getOrCreateSecretKey()
            val encryptedBytes = AesGcmCrypto.encrypt(apiKey.trim().toByteArray(Charsets.UTF_8), secretKey)
            val encoded = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            prefs.edit().putString(KEY_PREFIX + providerId, encoded).apply()
        } catch (e: Exception) {
            // Attempt key recovery once in case of Keystore invalidation
            try {
                runCatching { keyStore.deleteEntry(keyAlias) }
                val newKey = getOrCreateSecretKey()
                val encryptedBytes = AesGcmCrypto.encrypt(apiKey.trim().toByteArray(Charsets.UTF_8), newKey)
                val encoded = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
                prefs.edit().putString(KEY_PREFIX + providerId, encoded).apply()
            } catch (recoveryEx: Exception) {
                throw IllegalStateException("Unable to securely encrypt API key using Android Keystore. Please re-enter the key.")
            }
        }
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
