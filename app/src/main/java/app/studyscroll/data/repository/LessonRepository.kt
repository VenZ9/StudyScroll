package app.studyscroll.data.repository

import app.studyscroll.data.ai.GenerationRequest
import app.studyscroll.data.ai.ProviderManager
import app.studyscroll.data.local.dao.DocumentDao
import app.studyscroll.data.local.dao.LessonDao
import app.studyscroll.data.local.dao.PageTextDao
import app.studyscroll.data.local.dao.QuizDao
import app.studyscroll.data.local.entity.LessonEntity
import app.studyscroll.data.local.entity.QuizAttemptEntity
import app.studyscroll.data.pdf.ExtractedPage
import app.studyscroll.data.pdf.TextSegmenter
import app.studyscroll.domain.model.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class LessonRepository(
    private val lessonDao: LessonDao,
    private val documentDao: DocumentDao,
    private val pageTextDao: PageTextDao,
    private val quizDao: QuizDao,
    private val providerManager: ProviderManager
) {

    fun getLessonsForDocument(documentId: String): Flow<List<Lesson>> =
        lessonDao.getLessonsForDocument(documentId).map { list -> list.map { it.toDomain() } }

    fun getBookmarkedLessons(documentId: String): Flow<List<Lesson>> =
        lessonDao.getBookmarkedLessons(documentId).map { list -> list.map { it.toDomain() } }

    fun getReviewLessons(documentId: String): Flow<List<Lesson>> =
        lessonDao.getReviewLessons(documentId).map { list -> list.map { it.toDomain() } }

    fun getQuizLessons(documentId: String): Flow<List<Lesson>> =
        lessonDao.getQuizLessons(documentId).map { list -> list.map { it.toDomain() } }

    suspend fun updateBookmark(lessonId: String, bookmarked: Boolean) = withContext(Dispatchers.IO) {
        lessonDao.updateBookmark(lessonId, bookmarked)
    }

    suspend fun updateUnderstood(lessonId: String, understood: Boolean?) = withContext(Dispatchers.IO) {
        lessonDao.updateUnderstood(lessonId, understood)
    }

    suspend fun recordQuizAttempt(lessonId: String, answer: String, isCorrect: Boolean) = withContext(Dispatchers.IO) {
        quizDao.insertAttempt(
            QuizAttemptEntity(
                lessonId = lessonId,
                userAnswer = answer,
                isCorrect = isCorrect
            )
        )
    }

    suspend fun generateLessons(
        documentId: String,
        providerId: String,
        pageRange: Pair<Int, Int>? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        val document = documentDao.getDocumentById(documentId)
            ?: return@withContext Result.failure(IllegalArgumentException("Document not found: $documentId"))

        // Fetch page texts from DB (either all or page range)
        val pageTextEntities = if (pageRange != null) {
            pageTextDao.getPageTextsForRange(documentId, pageRange.first, pageRange.second)
        } else {
            pageTextDao.getPageTextsForDocument(documentId)
        }

        if (pageTextEntities.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No extracted text found for this document."))
        }

        val extractedPages = pageTextEntities.map { entity ->
            ExtractedPage(pageNumber = entity.pageNumber, text = entity.text)
        }

        // Segment text into semantic page-aware blocks
        val textBlocks = TextSegmenter.segmentPages(extractedPages)
        if (textBlocks.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Unable to segment chapter text into readable units."))
        }

        val request = GenerationRequest(
            documentId = documentId,
            documentTitle = document.title,
            textBlocks = textBlocks,
            targetCount = 45
        )

        val generationResult = providerManager.generateLessons(providerId, request)
        if (!generationResult.isSuccess) {
            return@withContext Result.failure(
                generationResult.exceptionOrNull() ?: RuntimeException("Lesson generation failed.")
            )
        }

        val rawLessons = generationResult.getOrNull() ?: emptyList()
        val providerProfile = providerManager.getProvider(providerId)
        val providerLabel = providerProfile?.displayName ?: "Offline"

        val lessonEntities = rawLessons.mapIndexed { index, raw ->
            LessonEntity(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                orderIndex = index,
                type = raw.type,
                title = raw.title,
                body = raw.body,
                bullets = raw.bullets,
                example = raw.example,
                question = raw.question,
                answer = raw.answer,
                explanation = raw.explanation,
                options = raw.options,
                whyItMatters = raw.whyItMatters,
                sources = raw.sources,
                confidence = raw.confidence,
                bookmarked = false,
                understood = null,
                generatedBy = providerLabel
            )
        }

        // Atomically replace lessons in database to prevent duplicates on regeneration
        lessonDao.replaceLessonsForDocument(documentId, lessonEntities)
        documentDao.updateLessonCount(documentId, lessonEntities.size)

        Result.success(lessonEntities.size)
    }
}
