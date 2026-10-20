package app.studyscroll.data.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.Key
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object AesGcmCrypto {
    private const val VERSION: Byte = 1
    private const val GCM_IV_LENGTH = 12 // 12 bytes recommended for GCM
    private const val GCM_TAG_LENGTH = 128 // 128 bit auth tag
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: ByteArray, key: Key): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintext)

        val output = ByteArrayOutputStream()
        output.write(VERSION.toInt())
        output.write(iv.size)
        output.write(iv)
        output.write(ciphertext)
        return output.toByteArray()
    }

    fun decrypt(encryptedBytes: ByteArray, key: Key): ByteArray {
        require(encryptedBytes.isNotEmpty()) { "Encrypted payload is empty" }
        val input = ByteArrayInputStream(encryptedBytes)
        val version = input.read().toByte()
        require(version == VERSION) { "Unsupported encryption version: $version" }

        val ivLength = input.read()
        require(ivLength == GCM_IV_LENGTH) { "Invalid IV length: $ivLength" }
        val iv = ByteArray(ivLength)
        val readIv = input.read(iv)
        require(readIv == ivLength) { "Corrupt IV" }

        val ciphertext = input.readBytes()
        require(ciphertext.isNotEmpty()) { "Missing ciphertext" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }
}
