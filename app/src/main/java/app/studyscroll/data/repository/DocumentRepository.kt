package app.studyscroll.data.repository

import android.content.Context
import android.net.Uri
import app.studyscroll.core.Constants
import app.studyscroll.data.local.dao.DocumentDao
import app.studyscroll.data.local.dao.PageTextDao
import app.studyscroll.data.local.entity.DocumentEntity
import app.studyscroll.data.local.entity.PageTextEntity
import app.studyscroll.data.pdf.PdfTextExtractor
import app.studyscroll.data.pdf.PdfValidationResult
import app.studyscroll.data.pdf.PdfValidator
import app.studyscroll.data.security.SecretStorage
import app.studyscroll.domain.model.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val pageTextDao: PageTextDao,
    private val pdfExtractor: PdfTextExtractor,
    private val secretStorage: SecretStorage,
    private val settingsRepository: SettingsRepository
) {

    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getDocument(id: String): Document? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)?.toDomain()
    }

    fun observeDocument(id: String): Flow<Document?> =
        documentDao.observeDocumentById(id).map { it?.toDomain() }

    suspend fun importPdf(
        fileName: String,
        fileSizeBytes: Long,
        inputStreamProvider: () -> InputStream?
    ): Result<Document> = withContext(Dispatchers.IO) {
        // Step 1: Pre-validation of known size
        if (fileSizeBytes > 0) {
            val preCheck = PdfValidator.validateKnownSize(fileSizeBytes)
            if (preCheck is PdfValidationResult.Failure) {
                return@withContext Result.failure(IllegalArgumentException(preCheck.reason))
            }
        }

        // Step 2: Stream copy to app-private cache with strict bounded byte limit check
        val docId = UUID.randomUUID().toString()
        val cacheDir = File(context.cacheDir, "imported_pdfs").apply { mkdirs() }
        val tempPdfFile = File(cacheDir, "$docId.pdf")

        val inputStream = inputStreamProvider()
            ?: return@withContext Result.failure(IllegalArgumentException(Constants.ERROR_PDF_UNREADABLE))

        val streamValidation = inputStream.use { stream ->
            PdfValidator.copyAndValidateStream(stream, tempPdfFile)
        }

        if (streamValidation is PdfValidationResult.Failure) {
            return@withContext Result.failure(IllegalArgumentException(streamValidation.reason))
        }

        val actualSize = (streamValidation as PdfValidationResult.Success).fileSizeBytes

        // Step 3: Extract text locally preserving 1-based page numbers
        val extractionResult = pdfExtractor.extract(tempPdfFile)
        if (!extractionResult.isSuccess) {
            tempPdfFile.delete()
            return@withContext Result.failure(IllegalStateException(extractionResult.errorMessage ?: Constants.ERROR_PDF_CORRUPT))
        }

        // Step 4: Persist document and extracted page texts to Room database
        val cleanTitle = fileName.removeSuffix(".pdf").ifBlank { "Untitled Document" }
        val documentEntity = DocumentEntity(
            id = docId,
            title = cleanTitle,
            uriString = tempPdfFile.absolutePath,
            fileSizeBytes = actualSize,
            pageCount = extractionResult.pageCount,
            lessonCount = 0,
            extractionStatus = "SUCCESS"
        )

        documentDao.insertDocument(documentEntity)

        val pageTextEntities = extractionResult.pages.mapIndexed { idx, page ->
            PageTextEntity(
                documentId = docId,
                pageNumber = page.pageNumber,
                blockOrder = idx,
                text = page.text
            )
        }
        pageTextDao.insertPageTexts(pageTextEntities)

        Result.success(documentEntity.toDomain())
    }

    suspend fun renameDocument(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        documentDao.renameDocument(id, newTitle)
    }

    suspend fun updateCurrentPosition(id: String, position: Int) = withContext(Dispatchers.IO) {
        documentDao.updateCurrentPosition(id, position)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(id)
        if (doc != null) {
            val file = File(doc.uriString)
            if (file.exists()) {
                file.delete()
            }
            documentDao.deleteDocumentById(id)
        }
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        // Delete cached PDFs
        val cacheDir = File(context.cacheDir, "imported_pdfs")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        documentDao.deleteAllDocuments()
        secretStorage.clearAll()
        settingsRepository.clearAllPreferences()
    }
}
