package app.studyscroll.pdf

import app.studyscroll.data.pdf.BlockType
import app.studyscroll.data.pdf.ExtractedPage
import app.studyscroll.data.pdf.TextSegmenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSegmenterTest {

    @Test
    fun `segmentPages preserves 1-based page numbers across all blocks`() {
        val pages = listOf(
            ExtractedPage(pageNumber = 1, text = "Chapter 1 Introduction\nPhotosynthesis is defined as the biological process by which green plants convert light energy into chemical energy."),
            ExtractedPage(pageNumber = 2, text = "2.1 Cellular Respiration\nIn contrast to photosynthesis, cellular respiration breaks down glucose to release energy.")
        )

        val blocks = TextSegmenter.segmentPages(pages)
        assertTrue(blocks.isNotEmpty())

        val page1Blocks = blocks.filter { it.pageNumber == 1 }
        val page2Blocks = blocks.filter { it.pageNumber == 2 }

        assertTrue(page1Blocks.isNotEmpty())
        assertTrue(page2Blocks.isNotEmpty())
        assertTrue(blocks.none { it.pageNumber < 1 })
    }

    @Test
    fun `segmentPages detects headings and definitions`() {
        val pages = listOf(
            ExtractedPage(
                pageNumber = 12,
                text = """
                    Chapter 4 Genetics
                    Mitosis: The process in cell division by which a nucleus divides typically consisting of four stages.
                    Unlike meiosis, mitosis results in two genetically identical daughter cells.
                    E = mc^2
                """.trimIndent()
            )
        )

        val blocks = TextSegmenter.segmentPages(pages)

        val headingBlock = blocks.find { it.type == BlockType.HEADING }
        assertTrue("Heading block should be detected", headingBlock != null)
        assertEquals("Genetics", headingBlock?.title)

        val defBlock = blocks.find { it.type == BlockType.DEFINITION }
        assertTrue("Definition block should be detected", defBlock != null)
        assertEquals("Mitosis", defBlock?.title)
        assertEquals(12, defBlock?.pageNumber)

        val compBlock = blocks.find { it.type == BlockType.COMPARISON }
        assertTrue("Comparison block should be detected", compBlock != null)

        val formulaBlock = blocks.find { it.type == BlockType.FORMULA }
        assertTrue("Formula block should be detected", formulaBlock != null)
    }

    @Test
    fun `segmentPages captures bullet and numbered lists`() {
        val pages = listOf(
            ExtractedPage(
                pageNumber = 5,
                text = """
                    • First principle of thermodynamics: energy cannot be created or destroyed.
                    • Second principle of thermodynamics: entropy always increases.
                    • Third principle of thermodynamics: entropy approaches a constant at absolute zero.
                """.trimIndent()
            )
        )

        val blocks = TextSegmenter.segmentPages(pages)
        val listBlock = blocks.find { it.type == BlockType.LIST }

        assertTrue("List block should be found", listBlock != null)
        assertEquals(3, listBlock?.items?.size)
        assertEquals(5, listBlock?.pageNumber)
    }
}
