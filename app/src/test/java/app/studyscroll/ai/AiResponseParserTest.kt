package app.studyscroll.ai

import app.studyscroll.data.ai.AiResponseParser
import app.studyscroll.data.pdf.BlockType
import app.studyscroll.data.pdf.TextBlock
import app.studyscroll.domain.model.ConfidenceLabel
import app.studyscroll.domain.model.LessonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseParserTest {

    private val availableBlocks = listOf(
        TextBlock(
            pageNumber = 10,
            type = BlockType.PARAGRAPH,
            title = "Thermodynamics",
            content = "The second law of thermodynamics states that the total entropy of an isolated system always increases over time.",
            rawExcerpt = "The second law of thermodynamics states..."
        )
    )

    @Test
    fun `parseJsonResponse parses valid JSON enclosed in markdown fences`() {
        val rawAiOutput = """
            ```json
            [
              {
                "type": "DEFINITION",
                "title": "Second Law of Thermodynamics",
                "body": "The total entropy of an isolated system always increases over time.",
                "bullets": ["Entropy increases", "Isolated system required"],
                "whyItMatters": "Establishes the arrow of time in physical processes.",
                "pageNumber": 10,
                "excerpt": "The second law of thermodynamics states that the total entropy of an isolated system always increases"
              }
            ]
            ```
        """.trimIndent()

        val lessons = AiResponseParser.parseJsonResponse(rawAiOutput, availableBlocks)
        assertEquals(1, lessons.size)

        val lesson = lessons.first()
        assertEquals(LessonType.DEFINITION, lesson.type)
        assertEquals("Second Law of Thermodynamics", lesson.title)
        assertEquals(10, lesson.sources.first().pageNumber)
        assertEquals(ConfidenceLabel.HIGH, lesson.confidence)
        assertEquals(2, lesson.bullets.size)
    }

    @Test
    fun `parseJsonResponse marks confidence unverified if excerpt does not match source text`() {
        val rawAiOutput = """
            [
              {
                "type": "KEY_FACT",
                "title": "Extrapolated Fact",
                "body": "Some fabricated fact not found in chapter.",
                "pageNumber": 10,
                "excerpt": "This quote is nowhere in the source text at all."
              }
            ]
        """.trimIndent()

        val lessons = AiResponseParser.parseJsonResponse(rawAiOutput, availableBlocks)
        assertEquals(1, lessons.size)
        assertEquals(ConfidenceLabel.UNVERIFIED, lessons.first().confidence)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonResponse throws IllegalArgumentException when no json array is present`() {
        AiResponseParser.parseJsonResponse("Sorry, I cannot answer this request.", availableBlocks)
    }
}
