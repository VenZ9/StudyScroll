package app.studyscroll.pdf

import app.studyscroll.core.Constants
import app.studyscroll.data.pdf.PdfValidationResult
import app.studyscroll.data.pdf.PdfValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class PdfValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `validateKnownSize accepts size exactly 25 MiB minus 1 byte`() {
        val size = Constants.MAX_PDF_BYTES - 1L // 26,214,399 bytes
        val result = PdfValidator.validateKnownSize(size)
        assertTrue(result is PdfValidationResult.Success)
        assertEquals(size, (result as PdfValidationResult.Success).fileSizeBytes)
    }

    @Test
    fun `validateKnownSize accepts size exactly at 25 MiB`() {
        val size = Constants.MAX_PDF_BYTES // 26,214,400 bytes
        val result = PdfValidator.validateKnownSize(size)
        assertTrue(result is PdfValidationResult.Success)
        assertEquals(size, (result as PdfValidationResult.Success).fileSizeBytes)
    }

    @Test
    fun `validateKnownSize rejects size 25 MiB plus 1 byte with exact error message`() {
        val size = Constants.MAX_PDF_BYTES + 1L // 26,214,401 bytes
        val result = PdfValidator.validateKnownSize(size)
        assertTrue(result is PdfValidationResult.Failure)
        assertEquals(Constants.ERROR_PDF_EXCEEDS_LIMIT, (result as PdfValidationResult.Failure).reason)
    }

    @Test
    fun `validateKnownSize rejects zero byte file`() {
        val result = PdfValidator.validateKnownSize(0L)
        assertTrue(result is PdfValidationResult.Failure)
        assertEquals(Constants.ERROR_PDF_EMPTY, (result as PdfValidationResult.Failure).reason)
    }

    @Test
    fun `validateKnownSize rejects negative file size`() {
        val result = PdfValidator.validateKnownSize(-100L)
        assertTrue(result is PdfValidationResult.Failure)
        assertEquals(Constants.ERROR_PDF_EMPTY, (result as PdfValidationResult.Failure).reason)
    }

    @Test
    fun `copyAndValidateStream accepts valid stream within 25 MiB`() {
        val destination = File(tempFolder.root, "valid.pdf")
        val content = ByteArray(1024 * 10) { 1 } // 10 KB
        val stream = ByteArrayInputStream(content)

        val result = PdfValidator.copyAndValidateStream(stream, destination)
        assertTrue(result is PdfValidationResult.Success)
        assertEquals(1024L * 10L, (result as PdfValidationResult.Success).fileSizeBytes)
        assertTrue(destination.exists())
    }

    @Test
    fun `copyAndValidateStream aborts and deletes file when stream exceeds limit`() {
        val destination = File(tempFolder.root, "oversized.pdf")
        val simulatedMax = 1000L // Simulate 1000 bytes max limit for fast testing

        // Stream delivering 1500 bytes
        val stream = object : InputStream() {
            var sent = 0
            override fun read(): Int {
                return if (sent < 1500) {
                    sent++
                    42
                } else -1
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (sent >= 1500) return -1
                val toRead = minOf(len, 1500 - sent)
                for (i in 0 until toRead) {
                    b[off + i] = 42
                }
                sent += toRead
                return toRead
            }
        }

        val result = PdfValidator.copyAndValidateStream(stream, destination, maxBytes = simulatedMax)
        assertTrue(result is PdfValidationResult.Failure)
        assertEquals(Constants.ERROR_PDF_EXCEEDS_LIMIT, (result as PdfValidationResult.Failure).reason)
        assertFalse("Destination file must be deleted on limit breach", destination.exists())
    }
}
