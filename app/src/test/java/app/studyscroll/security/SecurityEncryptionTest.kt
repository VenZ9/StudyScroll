package app.studyscroll.security

import app.studyscroll.data.security.AesGcmCrypto
import app.studyscroll.data.security.MemoryEncryptedSecretStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class SecurityEncryptionTest {

    private val secretStorage = MemoryEncryptedSecretStorage()

    @Test
    fun `saveApiKey encrypts key and roundtrip returns identical plaintext`() {
        val providerId = "groq_test"
        val rawApiKey = "gsk_1234567890abcdefghijklmnopqrstuvwxyz"

        secretStorage.saveApiKey(providerId, rawApiKey)
        assertTrue(secretStorage.hasApiKey(providerId))

        val retrievedKey = secretStorage.getApiKey(providerId)
        assertEquals(rawApiKey, retrievedKey)

        // Verify stored blob is encrypted ciphertext and does not contain raw key
        val rawBlob = secretStorage.getRawEncryptedBlob(providerId)
        assertNotNull(rawBlob)
        assertFalse("Ciphertext must not contain plaintext API key", rawBlob!!.contains(rawApiKey))
    }

    @Test
    fun `replaceApiKey safely overwrites existing secret`() {
        val providerId = "gemini_test"
        val oldKey = "old_secret_key_123"
        val newKey = "new_secret_key_456"

        secretStorage.saveApiKey(providerId, oldKey)
        assertEquals(oldKey, secretStorage.getApiKey(providerId))

        secretStorage.saveApiKey(providerId, newKey)
        assertEquals(newKey, secretStorage.getApiKey(providerId))
    }

    @Test
    fun `removeApiKey deletes secret cleanly`() {
        val providerId = "nvidia_test"
        secretStorage.saveApiKey(providerId, "nvapi_secret")
        assertTrue(secretStorage.hasApiKey(providerId))

        secretStorage.removeApiKey(providerId)
        assertFalse(secretStorage.hasApiKey(providerId))
        assertNull(secretStorage.getApiKey(providerId))
    }

    @Test(expected = Exception::class)
    fun `tampered ciphertext fails GCM authentication`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val plaintext = "super_secret_token".toByteArray(Charsets.UTF_8)
        val encrypted = AesGcmCrypto.encrypt(plaintext, key)

        // Tamper with ciphertext byte (e.g. at the end)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0xFF).toByte()

        // Decrypt must throw AEADBadTagException / general exception due to authentication tag failure
        AesGcmCrypto.decrypt(encrypted, key)
    }
}
