package app.studyscroll.data.ai

import app.studyscroll.data.pdf.BlockType
import app.studyscroll.data.pdf.TextBlock
import app.studyscroll.domain.model.ConfidenceLabel
import app.studyscroll.domain.model.LessonType
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import app.studyscroll.domain.model.SourceReference

class OfflineRuleBasedEngine : LessonGenerationProvider {

    override val providerType: ProviderType = ProviderType.OFFLINE
    override val displayName: String = "Offline (Template Engine)"

    override suspend fun testConnection(profile: ProviderProfile, apiKey: String?): Result<String> {
        return Result.success("Offline engine is always available locally without network or API key.")
    }

    override suspend fun generateLessons(
        profile: ProviderProfile,
        apiKey: String?,
        request: GenerationRequest
    ): Result<List<RawLesson>> {
        val blocks = request.textBlocks
        if (blocks.isEmpty()) {
            return Result.failure(IllegalArgumentException("No extracted text blocks available for lesson generation."))
        }

        val lessons = mutableListOf<RawLesson>()
        val seenTitles = mutableSetOf<String>()

        // 1. Chapter Overview Card
        val firstPage = blocks.firstOrNull()?.pageNumber ?: 1
        val headings = blocks.filter { it.type == BlockType.HEADING }.map { it.title }.take(6)
        val overviewBullets = if (headings.isNotEmpty()) {
            headings.map { "Section: $it" }
        } else {
            listOf("Source-grounded micro-lessons extracted directly from chapter text.")
        }

        lessons.add(
            RawLesson(
                type = LessonType.SUMMARY,
                title = "Overview: ${request.documentTitle}",
                body = "This study deck provides focused, source-grounded micro-lessons directly extracted from ${request.documentTitle}. Review each card to master key definitions, formulas, and concepts.",
                bullets = overviewBullets,
                whyItMatters = "Active recall in bite-sized chunks dramatically boosts long-term conceptual retention.",
                sources = listOf(
                    SourceReference(
                        pageNumber = firstPage,
                        excerpt = blocks.first().rawExcerpt
                    )
                ),
                confidence = ConfidenceLabel.HIGH
            )
        )

        // Collect definitions for creating high quality quiz cards later
        val extractedDefinitions = mutableListOf<Pair<String, TextBlock>>()

        // 2. Process each block into appropriate lesson type
        for (block in blocks) {
            val normalizedTitle = block.title.trim().lowercase()
            if (seenTitles.contains(normalizedTitle)) continue

            val rawLesson: RawLesson? = when (block.type) {
                BlockType.DEFINITION -> {
                    extractedDefinitions.add(Pair(block.title, block))
                    RawLesson(
                        type = LessonType.DEFINITION,
                        title = block.title,
                        body = block.content,
                        whyItMatters = "Foundational terminology essential for mastering this chapter.",
                        sources = listOf(
                            SourceReference(
                                pageNumber = block.pageNumber,
                                excerpt = block.rawExcerpt
                            )
                        ),
                        confidence = ConfidenceLabel.HIGH
                    )
                }

                BlockType.PROCESS -> {
                    val steps = block.content.split(Regex("(?i)(?:step\\s*\\d+:?|firstly,?|secondly,?|then,?|finally,?)"))
                        .map { it.trim() }
                        .filter { it.length > 10 }
                    RawLesson(
                        type = LessonType.PROCESS,
                        title = block.title,
                        body = "Step-by-step procedure detailed in the chapter:",
                        bullets = if (steps.isNotEmpty()) steps else listOf(block.content),
                        whyItMatters = "Understanding ordered mechanisms enables correct procedural problem-solving.",
                        sources = listOf(
                            SourceReference(
                                pageNumber = block.pageNumber,
                                excerpt = block.rawExcerpt
                            )
                        ),
                        confidence = ConfidenceLabel.HIGH
                    )
                }

                BlockType.COMPARISON -> {
                    RawLesson(
                        type = LessonType.COMPARISON,
                        title = block.title,
                        body = block.content,
                        whyItMatters = "Comparing contrasting concepts clarifies boundary conditions and prevents confusion on exams.",
                        sources = listOf(
                            SourceReference(
                                pageNumber = block.pageNumber,
                                excerpt = block.rawExcerpt
                            )
                        ),
                        confidence = ConfidenceLabel.HIGH
                    )
                }

                BlockType.FORMULA -> {
                    RawLesson(
                        type = LessonType.FORMULA,
                        title = block.title,
                        body = block.content,
                        whyItMatters = "Quantitative relationship critical for derivations and calculations.",
                        sources = listOf(
                            SourceReference(
                                pageNumber = block.pageNumber,
                                excerpt = block.rawExcerpt
                            )
                        ),
                        confidence = ConfidenceLabel.HIGH
                    )
                }

                BlockType.LIST -> {
                    if (block.items.isNotEmpty()) {
                        RawLesson(
                            type = LessonType.KEY_FACT,
                            title = block.title,
                            body = "Key takeaways from page ${block.pageNumber}:",
                            bullets = block.items.take(8),
                            sources = listOf(
                                SourceReference(
                                    pageNumber = block.pageNumber,
                                    excerpt = block.rawExcerpt
                                )
                            ),
                            confidence = ConfidenceLabel.HIGH
                        )
                    } else null
                }

                BlockType.PARAGRAPH -> {
                    if (block.content.length in 40..600) {
                        RawLesson(
                            type = LessonType.KEY_FACT,
                            title = block.title,
                            body = block.content,
                            sources = listOf(
                                SourceReference(
                                    pageNumber = block.pageNumber,
                                    excerpt = block.rawExcerpt
                                )
                            ),
                            confidence = ConfidenceLabel.HIGH
                        )
                    } else null
                }

                BlockType.HEADING -> null // Headings are used for context and overview
            }

            if (rawLesson != null) {
                seenTitles.add(normalizedTitle)
                lessons.add(rawLesson)
            }
        }

        // 3. Generate active recall Quiz cards from extracted definitions
        if (extractedDefinitions.isNotEmpty()) {
            for (i in extractedDefinitions.indices) {
                val (term, block) = extractedDefinitions[i]
                val otherTerms = extractedDefinitions
                    .filterIndexed { idx, _ -> idx != i }
                    .map { it.first }
                    .shuffled()
                    .take(3)

                val options = (listOf(term) + otherTerms).shuffled()

                lessons.add(
                    RawLesson(
                        type = LessonType.QUIZ,
                        title = "Recall Check: $term",
                        body = "Which concept is described by the following excerpt?",
                        question = "\"${block.content.take(180)}\"",
                        answer = term,
                        options = if (options.size >= 2) options else listOf("True", "False"),
                        explanation = "As established on page ${block.pageNumber}, $term refers to: ${block.content.take(120)}.",
                        sources = listOf(
                            SourceReference(
                                pageNumber = block.pageNumber,
                                excerpt = block.rawExcerpt
                            )
                        ),
                        confidence = ConfidenceLabel.HIGH
                    )
                )
            }
        }

        // Limit to target count (e.g. 30-60 cards) while preserving diversity
        val finalLessons = if (lessons.size > request.targetCount + 10) {
            lessons.take(request.targetCount + 10)
        } else {
            lessons
        }

        return Result.success(finalLessons)
    }
}
