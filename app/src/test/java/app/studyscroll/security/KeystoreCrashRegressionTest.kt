package app.studyscroll.security

import app.studyscroll.data.security.AesGcmCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class KeystoreCrashRegressionTest {

    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `encrypt generates unique 12-byte IV internally and roundtrips successfully`() {
        val plaintext = "gsk_test_api_key_secure_storage_value_12345".toByteArray(Charsets.UTF_8)

        // Multiple encryptions of the same plaintext must use distinct random IVs
        val encrypted1 = AesGcmCrypto.encrypt(plaintext, key)
        val encrypted2 = AesGcmCrypto.encrypt(plaintext, key)

        assertNotNull(encrypted1)
        assertNotNull(encrypted2)

        // Minimum length = 12-byte IV + plaintext length + 16-byte GCM tag
        assertTrue("Ciphertext must include IV and GCM auth tag", encrypted1.size >= 12 + plaintext.size + 16)

        // First 12 bytes are the IV - must differ between encryptions
        val iv1 = encrypted1.copyOfRange(0, 12)
        val iv2 = encrypted2.copyOfRange(0, 12)
        assertFalse("Each encryption must generate a unique IV", iv1.contentEquals(iv2))

        // Decryption roundtrips
        val decrypted1 = AesGcmCrypto.decrypt(encrypted1, key)
        val decrypted2 = AesGcmCrypto.decrypt(encrypted2, key)

        assertArrayEquals("Decrypted byte array must match original plaintext", plaintext, decrypted1)
        assertArrayEquals("Decrypted byte array must match original plaintext", plaintext, decrypted2)
        assertEquals("gsk_test_api_key_secure_storage_value_12345", String(decrypted1, Charsets.UTF_8))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decrypt throws IllegalArgumentException on truncated ciphertext smaller than IV`() {
        val tooShort = ByteArray(10) { 0 }
        AesGcmCrypto.decrypt(tooShort, key)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decrypt throws IllegalArgumentException on empty ciphertext`() {
        AesGcmCrypto.decrypt(ByteArray(0), key)
    }
}
