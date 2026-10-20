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

        val firstPage = blocks.firstOrNull()?.pageNumber ?: 1
        val headings = blocks.filter { it.type == BlockType.HEADING }.map { it.title }.distinct()

        // =========================================================================
        // 1. CHAPTER SUMMARY (Executive synthesis of the chapter)
        // =========================================================================
        val summaryBullets = if (headings.isNotEmpty()) {
            headings.take(6).map { "Section: $it" }
        } else {
            blocks.filter { it.type == BlockType.PARAGRAPH }.take(4).map { it.title }
        }

        val summaryExcerpt = blocks.firstOrNull { it.content.length > 50 }?.rawExcerpt ?: blocks.first().rawExcerpt
        lessons.add(
            RawLesson(
                type = LessonType.SUMMARY,
                title = "Chapter Summary: ${request.documentTitle}",
                body = "This chapter establishes core theoretical foundations, principles, and applications of ${request.documentTitle}. Review these verified micro-lessons and short notes for comprehensive exam mastery.",
                bullets = summaryBullets,
                whyItMatters = "Understanding the overarching chapter framework allows individual facts to be organized logically in long-term memory.",
                sources = listOf(
                    SourceReference(
                        pageNumber = firstPage,
                        excerpt = summaryExcerpt
                    )
                ),
                confidence = ConfidenceLabel.HIGH
            )
        )

        // =========================================================================
        // 2. EXAM-READY SHORT NOTES (High-yield quick revision cards)
        // =========================================================================
        val highYieldBlocks = blocks.filter {
            it.type == BlockType.DEFINITION || it.type == BlockType.FORMULA || it.type == BlockType.LIST
        }

        if (highYieldBlocks.isNotEmpty()) {
            // Group by page or chunk into exam-ready short note cards
            val shortNoteChunks = highYieldBlocks.chunked(3)
            for ((chunkIndex, chunk) in shortNoteChunks.take(4).withIndex()) {
                val notePoints = chunk.map { block ->
                    when (block.type) {
                        BlockType.DEFINITION -> "${block.title}: ${block.content.take(120)}"
                        BlockType.FORMULA -> "Formula: ${block.content}"
                        BlockType.LIST -> "${block.title} (${block.items.take(3).joinToString("; ")})"
                        else -> "${block.title} — ${block.content.take(100)}"
                    }
                }

                val primaryBlock = chunk.first()
                lessons.add(
                    RawLesson(
                        type = LessonType.SHORT_NOTE,
                        title = "Short Notes: Part ${chunkIndex + 1} (Exam-Ready)",
                        body = "Must-know high-yield points for quick revision and exam recall:",
                        bullets = notePoints,
                        whyItMatters = "Rapid review before examinations reinforces critical formulas and definitions.",
                        sources = listOf(
                            SourceReference(
                                pageNumber = primaryBlock.pageNumber,
                                excerpt = primaryBlock.rawExcerpt
                            )
                        ),
                        confidence = ConfidenceLabel.HIGH
                    )
                )
            }
        }

        // =========================================================================
        // 3. KEY TOPICS (In-depth breakdowns of major chapter sections)
        // =========================================================================
        val headingBlocks = blocks.filter { it.type == BlockType.HEADING }
        for (hBlock in headingBlocks.take(8)) {
            val normalized = hBlock.title.trim().lowercase()
            if (seenTitles.contains(normalized)) continue
            seenTitles.add(normalized)

            // Find paragraphs following this heading
            val relatedParagraphs = blocks.filter {
                it.pageNumber == hBlock.pageNumber && it.type == BlockType.PARAGRAPH
            }.take(3)

            val topicBullets = relatedParagraphs.map { p ->
                val firstSentence = p.content.split(Regex("[.!?]")).firstOrNull()?.trim() ?: p.content
                firstSentence.take(120)
            }

            val topicExcerpt = relatedParagraphs.firstOrNull()?.rawExcerpt ?: hBlock.rawExcerpt
            lessons.add(
                RawLesson(
                    type = LessonType.KEY_TOPIC,
                    title = "Key Topic: ${hBlock.title}",
                    body = "Comprehensive breakdown of ${hBlock.title} from page ${hBlock.pageNumber}:",
                    bullets = if (topicBullets.isNotEmpty()) topicBullets else listOf(hBlock.content),
                    whyItMatters = "Key topics represent core curriculum milestones and frequent exam focal points.",
                    sources = listOf(
                        SourceReference(
                            pageNumber = hBlock.pageNumber,
                            excerpt = topicExcerpt
                        )
                    ),
                    confidence = ConfidenceLabel.HIGH
                )
            )
        }

        // =========================================================================
        // 4. MICRO-LESSONS (Definitions, Processes, Comparisons, Formulas, Facts)
        // =========================================================================
        val extractedDefinitions = mutableListOf<Pair<String, TextBlock>>()

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
                        body = "Step-by-step mechanism detailed in the text:",
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
                        whyItMatters = "Quantitative relationship critical for derivations, problem solving, and calculations.",
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

                BlockType.HEADING -> null
            }

            if (rawLesson != null) {
                seenTitles.add(normalizedTitle)
                lessons.add(rawLesson)
            }
        }

        // =========================================================================
        // 5. MCQ QUIZZES & PRACTICE TESTS (4 choices, answer, and explanation)
        // =========================================================================
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
                        title = "MCQ: $term",
                        body = "Select the correct concept that matches the verified definition below:",
                        question = "\"${block.content.take(200)}\"",
                        answer = term,
                        options = if (options.size >= 2) options else listOf(term, "None of the above", "All of the above", "Cannot be determined"),
                        explanation = "As established on page ${block.pageNumber}, $term refers to: ${block.content.take(140)}.",
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
        } else {
            // Generate quizzes from key fact blocks if no definitions found
            val factBlocks = blocks.filter { it.type == BlockType.PARAGRAPH && it.content.length in 50..400 }
            for (fBlock in factBlocks.take(5)) {
                lessons.add(
                    RawLesson(
                        type = LessonType.QUIZ,
                        title = "Recall: ${fBlock.title}",
                        body = "True or False regarding the chapter content:",
                        question = "\"${fBlock.content.take(180)}\"",
                        answer = "True",
                        options = listOf("True", "False"),
                        explanation = "Verified directly from page ${fBlock.pageNumber} of the text.",
                        sources = listOf(
                            SourceReference(
                                pageNumber = fBlock.pageNumber,
                                excerpt = fBlock.rawExcerpt
                            )
                        ),
                        confidence = ConfidenceLabel.HIGH
                    )
                )
            }
        }

        // Bounded count: return well-formed lessons without empty duplicates
        val boundedLessons = if (lessons.size > request.targetCount + 15) {
            lessons.take(request.targetCount + 15)
        } else {
            lessons
        }

        return Result.success(boundedLessons)
    }
}
