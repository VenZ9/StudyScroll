package app.studyscroll.data.pdf

import java.io.File

data class ExtractedPage(
    val pageNumber: Int, // 1-based page number
    val text: String
)

data class ExtractionResult(
    val isSuccess: Boolean,
    val pageCount: Int,
    val pages: List<ExtractedPage>,
    val errorMessage: String? = null
)

interface PdfTextExtractor {
    suspend fun extract(file: File, onProgress: ((current: Int, total: Int) -> Unit)? = null): ExtractionResult
}
