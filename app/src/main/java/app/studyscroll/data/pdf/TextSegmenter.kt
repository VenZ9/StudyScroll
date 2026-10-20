package app.studyscroll.data.pdf

enum class BlockType {
    HEADING,
    DEFINITION,
    PROCESS,
    COMPARISON,
    FORMULA,
    LIST,
    PARAGRAPH
}

data class TextBlock(
    val pageNumber: Int,
    val type: BlockType,
    val title: String,
    val content: String,
    val items: List<String> = emptyList(),
    val rawExcerpt: String
)

object TextSegmenter {

    private val HEADING_REGEX = Regex(
        "^([0-9]+(\\.[0-9]+)*\\s+[A-Z].*|Chapter\\s+[0-9IVXLCDM]+.*|Section\\s+[0-9]+.*|[A-Z][A-Z0-9\\s,:-]{3,50})$"
    )

    private val DEFINITION_PATTERNS = listOf(
        Regex("(?i)^([^:]+):\\s+(.+)"),
        Regex("(?i)^(.+?)\\s+(?:is defined as|refers to|means|is described as|is known as)\\s+(.+)"),
        Regex("(?i)^(?:A|An|The)\\s+([a-zA-Z\\s-]+)\\s+is\\s+(?:a|an|the)\\s+(.+)")
    )

    private val FORMULA_REGEX = Regex(".*[=≈≠≤≥∑∫√π].*")
    private val PROCESS_KEYWORDS = listOf("step 1", "step 2", "first,", "secondly,", "then,", "finally,", "procedure", "algorithm")
    private val COMPARISON_KEYWORDS = listOf("in contrast", "whereas", "on the other hand", "unlike", "compared to", "differences between", "differ from")

    fun segmentPages(pages: List<ExtractedPage>): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()

        for (page in pages) {
            val lines = page.text.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (lines.isEmpty()) continue

            var currentHeading: String? = null
            var paragraphBuffer = StringBuilder()
            var listBuffer = mutableListOf<String>()

            fun flushParagraph() {
                val text = paragraphBuffer.toString().trim()
                if (text.length >= 25) {
                    val blockType = classifyTextBlock(text)
                    val title = deriveTitle(text, currentHeading)
                    blocks.add(
                        TextBlock(
                            pageNumber = page.pageNumber,
                            type = blockType,
                            title = title,
                            content = text,
                            rawExcerpt = text.take(200)
                        )
                    )
                }
                paragraphBuffer.clear()
            }

            fun flushList() {
                if (listBuffer.isNotEmpty()) {
                    val title = currentHeading ?: "Key Points (p. ${page.pageNumber})"
                    val content = listBuffer.joinToString("\n") { "• $it" }
                    blocks.add(
                        TextBlock(
                            pageNumber = page.pageNumber,
                            type = BlockType.LIST,
                            title = title,
                            content = content,
                            items = listBuffer.toList(),
                            rawExcerpt = content.take(200)
                        )
                    )
                    listBuffer.clear()
                }
            }

            for (line in lines) {
                // Check if this line looks like a major heading
                if (isHeading(line)) {
                    flushParagraph()
                    flushList()
                    currentHeading = cleanHeading(line)
                    blocks.add(
                        TextBlock(
                            pageNumber = page.pageNumber,
                            type = BlockType.HEADING,
                            title = currentHeading,
                            content = line,
                            rawExcerpt = line
                        )
                    )
                    continue
                }

                // Check if line is a bullet or numbered list item
                if (isListItem(line)) {
                    flushParagraph()
                    listBuffer.add(cleanListItem(line))
                    continue
                } else if (listBuffer.isNotEmpty()) {
                    flushList()
                }

                // Check for single-line definition
                val defMatch = matchDefinition(line)
                if (defMatch != null) {
                    flushParagraph()
                    blocks.add(
                        TextBlock(
                            pageNumber = page.pageNumber,
                            type = BlockType.DEFINITION,
                            title = defMatch.first,
                            content = defMatch.second,
                            rawExcerpt = line.take(200)
                        )
                    )
                    continue
                }

                // Check for mathematical formula
                if (isFormula(line)) {
                    flushParagraph()
                    val title = currentHeading?.let { "$it: Formula" } ?: "Formula (p. ${page.pageNumber})"
                    blocks.add(
                        TextBlock(
                            pageNumber = page.pageNumber,
                            type = BlockType.FORMULA,
                            title = title,
                            content = line,
                            rawExcerpt = line
                        )
                    )
                    continue
                }

                // Accumulate paragraph text
                if (paragraphBuffer.isNotEmpty()) {
                    paragraphBuffer.append(" ")
                }
                paragraphBuffer.append(line)

                // If paragraph is long enough (e.g. > 300 chars) or ends with period, flush if next seems new
                if (paragraphBuffer.length > 350 && line.endsWith(".")) {
                    flushParagraph()
                }
            }

            flushParagraph()
            flushList()
        }

        return blocks
    }

    private fun isHeading(line: String): Boolean {
        if (line.length > 70 || line.endsWith(".")) return false
        return HEADING_REGEX.matches(line)
    }

    private fun cleanHeading(line: String): String {
        return line.replace(Regex("^[0-9]+(\\.[0-9]+)*\\s*"), "").trim()
    }

    private fun isListItem(line: String): Boolean {
        return line.startsWith("•") ||
                line.startsWith("-") ||
                line.startsWith("*") ||
                line.matches(Regex("^[0-9]+[.)]\\s+.*"))
    }

    private fun cleanListItem(line: String): String {
        return line.replace(Regex("^[•\\-*\\d+.)\\s]+"), "").trim()
    }

    private fun matchDefinition(line: String): Pair<String, String>? {
        if (line.length > 250) return null
        for (pattern in DEFINITION_PATTERNS) {
            val match = pattern.find(line)
            if (match != null && match.groupValues.size >= 3) {
                val term = match.groupValues[1].trim()
                val def = match.groupValues[2].trim()
                if (term.length in 3..50 && def.length in 15..250) {
                    return Pair(term.replaceFirstChar { it.uppercase() }, def)
                }
            }
        }
        return null
    }

    private fun isFormula(line: String): Boolean {
        return line.length in 4..100 && FORMULA_REGEX.matches(line) && !line.contains(" because ")
    }

    private fun classifyTextBlock(text: String): BlockType {
        val lower = text.lowercase()
        return when {
            COMPARISON_KEYWORDS.any { lower.contains(it) } -> BlockType.COMPARISON
            PROCESS_KEYWORDS.any { lower.contains(it) } -> BlockType.PROCESS
            FORMULA_REGEX.matches(text) && text.length < 150 -> BlockType.FORMULA
            else -> BlockType.PARAGRAPH
        }
    }

    private fun deriveTitle(text: String, currentHeading: String?): String {
        if (!currentHeading.isNullOrBlank()) {
            return currentHeading
        }
        // Take the first sentence or first 5-8 words
        val firstSentence = text.split(Regex("[.!?]")).firstOrNull()?.trim() ?: text
        val words = firstSentence.split(Regex("\\s+")).take(6).joinToString(" ")
        return words.ifBlank { "Micro-Lesson" }
    }
}
