package app.studyscroll.rules

import app.studyscroll.data.ai.GenerationRequest
import app.studyscroll.data.ai.OfflineRuleBasedEngine
import app.studyscroll.data.pdf.BlockType
import app.studyscroll.data.pdf.TextBlock
import app.studyscroll.domain.model.AuthMode
import app.studyscroll.domain.model.LessonType
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineRuleBasedEngineTest {

    private val engine = OfflineRuleBasedEngine()
    private val offlineProfile = ProviderProfile(
        id = "provider_offline",
        type = ProviderType.OFFLINE,
        displayName = "Offline Engine",
        baseUrl = "",
        modelId = "offline",
        authMode = AuthMode.NONE
    )

    @Test
    fun `engine generates lessons with valid types, sources, and quizzes`() = runBlocking {
        val blocks = listOf(
            TextBlock(
                pageNumber = 1,
                type = BlockType.HEADING,
                title = "Cell Biology",
                content = "Chapter 1 Cell Biology",
                rawExcerpt = "Chapter 1 Cell Biology"
            ),
            TextBlock(
                pageNumber = 2,
                type = BlockType.DEFINITION,
                title = "Mitochondria",
                content = "The powerhouse of the cell responsible for ATP generation through cellular respiration.",
                rawExcerpt = "Mitochondria: The powerhouse of the cell..."
            ),
            TextBlock(
                pageNumber = 3,
                type = BlockType.DEFINITION,
                title = "Ribosome",
                content = "A cellular particle made of RNA and protein that serves as the site for protein synthesis.",
                rawExcerpt = "Ribosome: A cellular particle..."
            ),
            TextBlock(
                pageNumber = 4,
                type = BlockType.FORMULA,
                title = "ATP Yield",
                content = "Glucose + 6O2 -> 6CO2 + 6H2O + 38 ATP",
                rawExcerpt = "Glucose + 6O2 -> 6CO2 + 6H2O + 38 ATP"
            )
        )

        val request = GenerationRequest(
            documentId = "doc_123",
            documentTitle = "Cell Biology Chapter 1",
            textBlocks = blocks,
            targetCount = 30
        )

        val result = engine.generateLessons(offlineProfile, apiKey = null, request = request)
        assertTrue(result.isSuccess)

        val lessons = result.getOrThrow()
        assertTrue("Should produce multiple lessons", lessons.size >= 4)

        // Verify Overview/Summary card
        val summaryCard = lessons.find { it.type == LessonType.SUMMARY }
        assertNotNull("Should contain a summary/overview card", summaryCard)

        // Verify Definition card
        val mitoCard = lessons.find { it.title == "Mitochondria" }
        assertNotNull("Should contain Mitochondria definition", mitoCard)
        assertEquals(LessonType.DEFINITION, mitoCard?.type)
        assertEquals(2, mitoCard?.sources?.first()?.pageNumber)
        assertFalse(mitoCard?.sources?.first()?.excerpt.isNullOrBlank())

        // Verify Active Recall Quiz card
        val quizCard = lessons.find { it.type == LessonType.QUIZ }
        assertNotNull("Should generate active recall quiz cards", quizCard)
        assertNotNull(quizCard?.question)
        assertNotNull(quizCard?.answer)
        assertTrue(quizCard?.options?.isNotEmpty() == true)

        // Ensure all lessons have valid 1-based page numbers
        for (lesson in lessons) {
            assertTrue("Every lesson must have at least one source reference", lesson.sources.isNotEmpty())
            assertTrue("Page number must be >= 1", lesson.sources.all { it.pageNumber >= 1 })
            assertTrue("Source excerpt must not be empty", lesson.sources.all { it.excerpt.isNotBlank() })
        }
    }

    @Test
    fun `engine fails gracefully when text blocks are empty`() = runBlocking {
        val request = GenerationRequest(
            documentId = "empty_doc",
            documentTitle = "Empty Document",
            textBlocks = emptyList()
        )

        val result = engine.generateLessons(offlineProfile, apiKey = null, request = request)
        assertFalse("Should fail when no blocks are supplied", result.isSuccess)
    }
}
