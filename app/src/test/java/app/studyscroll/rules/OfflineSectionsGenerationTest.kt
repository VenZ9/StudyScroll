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

class OfflineSectionsGenerationTest {

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
    fun `engine generates all required functional sections with source grounding`() = runBlocking {
        val blocks = listOf(
            TextBlock(
                pageNumber = 1,
                type = BlockType.HEADING,
                title = "Neurobiology and Synaptic Transmission",
                content = "Chapter 3 Neurobiology and Synaptic Transmission",
                rawExcerpt = "Chapter 3 Neurobiology and Synaptic Transmission"
            ),
            TextBlock(
                pageNumber = 1,
                type = BlockType.DEFINITION,
                title = "Action Potential",
                content = "A rapid sequence of changes in the voltage across a membrane, driven by the coordinated opening and closing of voltage-gated ion channels.",
                rawExcerpt = "Action Potential: A rapid sequence of changes..."
            ),
            TextBlock(
                pageNumber = 2,
                type = BlockType.DEFINITION,
                title = "Synapse",
                content = "A junction between two nerve cells, consisting of a minute gap across which impulses pass by diffusion of a neurotransmitter.",
                rawExcerpt = "Synapse: A junction between two nerve cells..."
            ),
            TextBlock(
                pageNumber = 3,
                type = BlockType.PROCESS,
                title = "Synaptic Transmission",
                content = "First action potential reaches axon terminal. Second calcium channels open allowing calcium influx. Third neurotransmitter vesicles fuse with presynaptic membrane.",
                rawExcerpt = "Synaptic Transmission steps..."
            ),
            TextBlock(
                pageNumber = 4,
                type = BlockType.COMPARISON,
                title = "Chemical vs Electrical Synapses",
                content = "Chemical synapses use neurotransmitters across a synaptic cleft, whereas electrical synapses transmit signals directly via gap junctions.",
                rawExcerpt = "Chemical synapses use neurotransmitters..."
            ),
            TextBlock(
                pageNumber = 5,
                type = BlockType.FORMULA,
                title = "Nernst Equation",
                content = "E = (RT / zF) * ln([ion_out] / [ion_in]) calculates the equilibrium potential for a specific ion across an excitable membrane.",
                rawExcerpt = "E = (RT / zF) * ln([ion_out] / [ion_in])"
            )
        )

        val request = GenerationRequest(
            documentId = "neuro_ch3",
            documentTitle = "Neurobiology Chapter 3",
            textBlocks = blocks,
            targetCount = 20
        )

        val result = engine.generateLessons(offlineProfile, apiKey = null, request = request)
        assertTrue("Generation should succeed", result.isSuccess)

        val lessons = result.getOrThrow()
        assertTrue("Should generate comprehensive lessons", lessons.size >= 5)

        // 1. Chapter Summary section
        val summaryLessons = lessons.filter { it.type == LessonType.SUMMARY }
        assertFalse("Must generate Chapter Summary cards", summaryLessons.isEmpty())
        for (summary in summaryLessons) {
            assertTrue("Summary title must not be blank", summary.title.isNotBlank())
            assertTrue("Summary body must not be blank", summary.body.isNotBlank())
            assertTrue("Summary must have valid page sources", summary.sources.isNotEmpty())
            assertTrue(summary.sources.all { it.pageNumber >= 1 })
        }

        // 2. Short Notes (exam-ready) section
        val shortNotes = lessons.filter { it.type == LessonType.SHORT_NOTE }
        assertFalse("Must generate Exam-Ready Short Notes", shortNotes.isEmpty())
        for (note in shortNotes) {
            assertTrue("Short note must contain exam bullets", note.bullets.isNotEmpty())
            assertTrue("Short note must cite source pages", note.sources.isNotEmpty())
            assertTrue("Short note must not contain single word fragments", note.body.split(" ").size >= 5)
        }

        // 3. Key Topics section
        val keyTopics = lessons.filter { it.type == LessonType.KEY_TOPIC }
        assertFalse("Must generate Key Topics section", keyTopics.isEmpty())
        for (topic in keyTopics) {
            assertNotNull("Key topic must have why it matters / takeaway", topic.whyItMatters)
            assertTrue("Key topic must cite source pages", topic.sources.isNotEmpty())
        }

        // 4. Micro-lessons section
        val microLessons = lessons.filter {
            it.type in setOf(
                LessonType.DEFINITION,
                LessonType.KEY_FACT,
                LessonType.PROCESS,
                LessonType.COMPARISON,
                LessonType.FORMULA,
                LessonType.EXAMPLE
            )
        }
        assertFalse("Must generate Micro-lessons", microLessons.isEmpty())
        for (micro in microLessons) {
            assertTrue("Micro lesson must have >= 5 words", micro.body.split(" ").size >= 5)
            assertTrue("Micro lesson must have source evidence", micro.sources.isNotEmpty())
            assertTrue("Source excerpt must be non-empty", micro.sources.all { it.excerpt.isNotBlank() })
        }

        // 5. MCQ Quizzes with answers and explanations
        val quizzes = lessons.filter { it.type == LessonType.QUIZ }
        assertFalse("Must generate MCQ Quizzes", quizzes.isEmpty())
        for (quiz in quizzes) {
            assertNotNull("Quiz must have a question", quiz.question)
            assertNotNull("Quiz must have an answer", quiz.answer)
            assertNotNull("Quiz must have an explanation", quiz.explanation)
            assertEquals("Quiz must have 4 multiple choice options", 4, quiz.options.size)
            assertTrue(
                "Quiz answer must be one of the 4 options",
                quiz.options.contains(quiz.answer)
            )
            assertTrue("Quiz must cite the source page where the answer is found", quiz.sources.isNotEmpty())
        }
    }
}
