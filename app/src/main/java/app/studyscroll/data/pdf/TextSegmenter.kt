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
        "^([0-9]+(\\.[0-9]+)*\\s+[A-Z].*|Chapter\\s+[0-9IVXLCDM]+.*|Section\\s+[0-9]+.*|[A-Z][A-Z0-9\\s,:-]{3,60})$"
    )

    private val DEFINITION_PATTERNS = listOf(
        Regex("(?i)^([^:]{2,50}):\\s+([A-Z0-9].{20,})"),
        Regex("(?i)^(.{2,50}?)\\s+(?:is defined as|refers to|means|is described as|is known as|is the process of)\\s+([a-z0-9].{20,})"),
        Regex("(?i)^(?:A|An|The)\\s+([a-zA-Z\\s-]{2,50})\\s+is\\s+(?:a|an|the)\\s+([a-z0-9].{20,})")
    )

    private val FORMULA_REGEX = Regex(".*[=≈≠≤≥∑∫√π∆±].*")
    private val PROCESS_KEYWORDS = listOf("step 1", "step 2", "first,", "secondly,", "then,", "finally,", "procedure", "algorithm", "cycle of", "stages of")
    private val COMPARISON_KEYWORDS = listOf("in contrast", "whereas", "on the other hand", "unlike", "compared to", "differences between", "differ from", "while ")

    fun segmentPages(pages: List<ExtractedPage>): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()

        for (page in pages) {
            val rawLines = page.text.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (rawLines.isEmpty()) continue

            // 1. Clean header/footer artifacts and merge hyphenated line wraps
            val cleanedLines = preprocessLines(rawLines)
            if (cleanedLines.isEmpty()) continue

            var currentHeading: String? = null
            var paragraphBuffer = StringBuilder()
            var listBuffer = mutableListOf<String>()

            fun flushParagraph() {
                val text = paragraphBuffer.toString().trim()
                if (text.length >= 35 && isValidSentence(text)) {
                    val blockType = classifyTextBlock(text)
                    val title = deriveTitle(text, currentHeading)
                    blocks.add(
                        TextBlock(
                            pageNumber = page.pageNumber,
                            type = blockType,
                            title = title,
                            content = text,
                            rawExcerpt = text.take(250)
                        )
                    )
                }
                paragraphBuffer.clear()
            }

            fun flushList() {
                if (listBuffer.size >= 2) {
                    val title = currentHeading ?: "Key Points (p. ${page.pageNumber})"
                    val content = listBuffer.joinToString("\n") { "• $it" }
                    blocks.add(
                        TextBlock(
                            pageNumber = page.pageNumber,
                            type = BlockType.LIST,
                            title = title,
                            content = content,
                            items = listBuffer.toList(),
                            rawExcerpt = content.take(250)
                        )
                    )
                }
                listBuffer.clear()
            }

            for (line in cleanedLines) {
                // Check if this line is a section heading
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
                    val cleanItem = cleanListItem(line)
                    if (cleanItem.length >= 10) {
                        listBuffer.add(cleanItem)
                    }
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
                            rawExcerpt = line.take(250)
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

                // Flush paragraph on sentence boundary if sufficiently formed
                if (paragraphBuffer.length >= 300 && (line.endsWith(".") || line.endsWith("!") || line.endsWith("?"))) {
                    flushParagraph()
                }
            }

            flushParagraph()
            flushList()
        }

        return blocks
    }

    private fun preprocessLines(rawLines: List<String>): List<String> {
        val filtered = mutableListOf<String>()
        var pendingHyphen: String? = null

        for (line in rawLines) {
            // Filter noise: standalone page numbers, copyright lines, URL lines
            if (isHeaderFooterNoise(line)) continue

            val currentLine: String
            if (pendingHyphen != null) {
                currentLine = pendingHyphen + line
                pendingHyphen = null
            } else {
                currentLine = line
            }

            // Check if line ends with a broken hyphenated word (e.g., "bio-")
            if (currentLine.endsWith("-") && currentLine.length > 2 && currentLine.lastIndex > 0 && currentLine[currentLine.lastIndex - 1].isLetter()) {
                pendingHyphen = currentLine.dropLast(1)
            } else {
                filtered.add(currentLine)
            }
        }

        if (pendingHyphen != null) {
            filtered.add(pendingHyphen)
        }

        return filtered
    }

    private fun isHeaderFooterNoise(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.matches(Regex("^[0-9]+$"))) return true // standalone page number
        if (trimmed.matches(Regex("(?i)^page\\s+[0-9]+(\\s+of\\s+[0-9]+)?$"))) return true
        if (trimmed.matches(Regex("(?i)^[ivxlcdm]+$"))) return true // roman numeral page
        if (trimmed.contains("©") || trimmed.matches(Regex("(?i).*all rights reserved.*"))) return true
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www.")) return true
        return false
    }

    private fun isValidSentence(text: String): Boolean {
        val words = text.split(Regex("\\s+"))
        if (words.size < 5) return false
        // Must start with uppercase or number, and contain letters
        val firstChar = text.firstOrNull { !it.isWhitespace() } ?: return false
        return firstChar.isUpperCase() || firstChar.isDigit()
    }

    private fun isHeading(line: String): Boolean {
        if (line.length > 80 || line.endsWith(".")) return false
        return HEADING_REGEX.matches(line)
    }

    fun cleanHeading(line: String): String {
        val cleaned = line.replace(
            Regex("^(?:Chapter|Section|[0-9]+(?:\\.[0-9]+)*)\\s*[0-9IVXLCDM]*[:\\s-]*", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        return cleaned.ifBlank { line.trim() }
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
        if (line.length > 300) return null
        for (pattern in DEFINITION_PATTERNS) {
            val match = pattern.find(line)
            if (match != null && match.groupValues.size >= 3) {
                val term = match.groupValues[1].trim()
                val def = match.groupValues[2].trim()
                if (term.length in 2..50 && def.length in 20..300 && !term.endsWith(".")) {
                    return Pair(term.replaceFirstChar { it.uppercase() }, def)
                }
            }
        }
        return null
    }

    private fun isFormula(line: String): Boolean {
        return line.length in 4..120 && FORMULA_REGEX.matches(line) && !line.contains(" because ")
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
        val firstSentence = text.split(Regex("[.!?]")).firstOrNull()?.trim() ?: text
        val words = firstSentence.split(Regex("\\s+")).take(6).joinToString(" ")
        return words.ifBlank { "Concept Note" }
    }
}
