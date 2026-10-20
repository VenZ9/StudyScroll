package app.studyscroll.data.pdf

import app.studyscroll.core.Constants
import java.io.File
import java.io.InputStream
import java.io.OutputStream

sealed class PdfValidationResult {
    data class Success(val fileSizeBytes: Long) : PdfValidationResult()
    data class Failure(val reason: String) : PdfValidationResult()
}

object PdfValidator {

    /**
     * Strictly validates a known file size against the 25 MiB limit (26,214,400 bytes).
     *
     * - < 25 MiB: Accept
     * - == 25 MiB (26,214,400 bytes): Accept
     * - > 25 MiB: Reject with Constants.ERROR_PDF_EXCEEDS_LIMIT
     * - <= 0: Reject with Constants.ERROR_PDF_EMPTY
     */
    fun validateKnownSize(sizeBytes: Long): PdfValidationResult {
        return when {
            sizeBytes <= 0L -> PdfValidationResult.Failure(Constants.ERROR_PDF_EMPTY)
            sizeBytes > Constants.MAX_PDF_BYTES -> PdfValidationResult.Failure(Constants.ERROR_PDF_EXCEEDS_LIMIT)
            else -> PdfValidationResult.Success(sizeBytes)
        }
    }

    /**
     * Streams an input stream into a destination file, enforcing the strict 25 MiB limit
     * chunk-by-chunk without loading the entire stream into memory.
     * If the bytes read exceed 26,214,400 bytes, reading is aborted immediately,
     * the partial destination file is deleted, and Failure is returned.
     */
    fun copyAndValidateStream(
        inputStream: InputStream,
        destinationFile: File,
        maxBytes: Long = Constants.MAX_PDF_BYTES
    ): PdfValidationResult {
        var totalBytesRead = 0L
        val buffer = ByteArray(8192)

        try {
            destinationFile.outputStream().use { output ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytesRead += bytesRead
                    if (totalBytesRead > maxBytes) {
                        output.flush()
                        output.close()
                        destinationFile.delete()
                        return PdfValidationResult.Failure(Constants.ERROR_PDF_EXCEEDS_LIMIT)
                    }
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }

            if (totalBytesRead == 0L) {
                destinationFile.delete()
                return PdfValidationResult.Failure(Constants.ERROR_PDF_EMPTY)
            }

            return PdfValidationResult.Success(totalBytesRead)
        } catch (e: Exception) {
            destinationFile.delete()
            return PdfValidationResult.Failure(e.message ?: Constants.ERROR_PDF_UNREADABLE)
        }
    }
}
