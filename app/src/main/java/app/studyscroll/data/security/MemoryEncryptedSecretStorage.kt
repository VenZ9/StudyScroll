package app.studyscroll.data.security

import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MemoryEncryptedSecretStorage(
    private val secretKey: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
) : SecretStorage {

    private val encryptedStore = ConcurrentHashMap<String, String>()

    override fun saveApiKey(providerId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            removeApiKey(providerId)
            return
        }
        val encryptedBytes = AesGcmCrypto.encrypt(apiKey.toByteArray(Charsets.UTF_8), secretKey)
        val encoded = Base64.getEncoder().encodeToString(encryptedBytes)
        encryptedStore[providerId] = encoded
    }

    override fun getApiKey(providerId: String): String? {
        val encoded = encryptedStore[providerId] ?: return null
        return try {
            val encryptedBytes = Base64.getDecoder().decode(encoded)
            val decryptedBytes = AesGcmCrypto.decrypt(encryptedBytes, secretKey)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override fun hasApiKey(providerId: String): Boolean {
        return encryptedStore.containsKey(providerId)
    }

    override fun removeApiKey(providerId: String) {
        encryptedStore.remove(providerId)
    }

    override fun clearAll() {
        encryptedStore.clear()
    }

    fun getRawEncryptedBlob(providerId: String): String? = encryptedStore[providerId]
}
