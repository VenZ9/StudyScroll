package app.studyscroll.data.pdf

import android.content.Context
import app.studyscroll.core.Constants
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfBoxTextExtractor(
    private val context: Context
) : PdfTextExtractor {

    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    override suspend fun extract(
        file: File,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): ExtractionResult = withContext(Dispatchers.IO) {
        ensureInitialized()

        if (!file.exists() || file.length() == 0L) {
            return@withContext ExtractionResult(
                isSuccess = false,
                pageCount = 0,
                pages = emptyList(),
                errorMessage = Constants.ERROR_PDF_EMPTY
            )
        }

        try {
            PDDocument.load(file).use { document ->
                if (document.isEncrypted) {
                    return@withContext ExtractionResult(
                        isSuccess = false,
                        pageCount = 0,
                        pages = emptyList(),
                        errorMessage = Constants.ERROR_PDF_PASSWORD_PROTECTED
                    )
                }

                val totalPages = document.numberOfPages
                if (totalPages == 0) {
                    return@withContext ExtractionResult(
                        isSuccess = false,
                        pageCount = 0,
                        pages = emptyList(),
                        errorMessage = Constants.ERROR_PDF_CORRUPT
                    )
                }

                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }

                val extractedPages = ArrayList<ExtractedPage>(totalPages)
                var totalExtractedChars = 0

                for (page in 1..totalPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val pageText = stripper.getText(document).trim()

                    extractedPages.add(ExtractedPage(pageNumber = page, text = pageText))
                    totalExtractedChars += pageText.length
                    onProgress?.invoke(page, totalPages)
                }

                // If all pages have virtually no text, this is likely a scanned or image-only PDF
                if (totalExtractedChars < 20) {
                    return@withContext ExtractionResult(
                        isSuccess = false,
                        pageCount = totalPages,
                        pages = extractedPages,
                        errorMessage = Constants.ERROR_PDF_NO_TEXT
                    )
                }

                ExtractionResult(
                    isSuccess = true,
                    pageCount = totalPages,
                    pages = extractedPages
                )
            }
        } catch (e: Exception) {
            ExtractionResult(
                isSuccess = false,
                pageCount = 0,
                pages = emptyList(),
                errorMessage = e.message ?: Constants.ERROR_PDF_CORRUPT
            )
        }
    }
}
