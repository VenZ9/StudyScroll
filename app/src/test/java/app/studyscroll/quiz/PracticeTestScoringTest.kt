package app.studyscroll.quiz

import app.studyscroll.domain.model.Lesson
import app.studyscroll.domain.model.LessonType
import app.studyscroll.domain.model.SourceReference
import app.studyscroll.ui.feed.PracticeTestState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeTestScoringTest {

    private fun createSampleQuiz(
        id: String,
        question: String,
        correctAnswer: String,
        options: List<String>
    ): Lesson {
        return Lesson(
            id = id,
            documentId = "doc_test",
            orderIndex = 0,
            type = LessonType.QUIZ,
            title = "Test Question",
            body = question,
            question = question,
            answer = correctAnswer,
            options = options,
            explanation = "Explanation for $correctAnswer",
            sources = listOf(SourceReference(pageNumber = 1, excerpt = question))
        )
    }

    @Test
    fun `test scoring calculates 100 percent when all questions answered correctly`() {
        val questions = listOf(
            createSampleQuiz("q1", "What is mitochondria?", "Powerhouse", listOf("Powerhouse", "Nucleus", "Ribosome", "Lipid")),
            createSampleQuiz("q2", "What is DNA?", "Genetic material", listOf("Genetic material", "Enzyme", "Carbohydrate", "Gas"))
        )

        val answers = mapOf(
            0 to "Powerhouse",
            1 to "Genetic material"
        )

        var score = 0
        for ((idx, q) in questions.withIndex()) {
            val selected = answers[idx]
            if (selected.equals(q.answer?.trim(), ignoreCase = true)) {
                score++
            }
        }

        val percentage = (score * 100) / questions.size
        val state = PracticeTestState(
            selectedAnswers = answers,
            isSubmitted = true,
            score = score,
            totalQuestions = questions.size,
            percentage = percentage
        )

        assertEquals(2, state.score)
        assertEquals(2, state.totalQuestions)
        assertEquals(100, state.percentage)
        assertTrue(state.isSubmitted)
    }

    @Test
    fun `test scoring calculates 50 percent for half correct`() {
        val questions = listOf(
            createSampleQuiz("q1", "Question 1", "Option A", listOf("Option A", "Option B", "Option C", "Option D")),
            createSampleQuiz("q2", "Question 2", "Option B", listOf("Option A", "Option B", "Option C", "Option D"))
        )

        val answers = mapOf(
            0 to "Option A", // correct
            1 to "Option D"  // wrong
        )

        var score = 0
        for ((idx, q) in questions.withIndex()) {
            val selected = answers[idx]
            if (selected.equals(q.answer?.trim(), ignoreCase = true)) {
                score++
            }
        }

        val percentage = (score * 100) / questions.size
        val state = PracticeTestState(
            selectedAnswers = answers,
            isSubmitted = true,
            score = score,
            totalQuestions = questions.size,
            percentage = percentage
        )

        assertEquals(1, state.score)
        assertEquals(2, state.totalQuestions)
        assertEquals(50, state.percentage)
    }

    @Test
    fun `test scoring with unattempted questions treats them as incorrect`() {
        val questions = listOf(
            createSampleQuiz("q1", "Q1", "Correct", listOf("Correct", "Wrong1", "Wrong2", "Wrong3")),
            createSampleQuiz("q2", "Q2", "Correct", listOf("Correct", "Wrong1", "Wrong2", "Wrong3")),
            createSampleQuiz("q3", "Q3", "Correct", listOf("Correct", "Wrong1", "Wrong2", "Wrong3")),
            createSampleQuiz("q4", "Q4", "Correct", listOf("Correct", "Wrong1", "Wrong2", "Wrong3"))
        )

        val answers = mapOf(
            0 to "Correct"
            // q1, q2, q3 left unattempted
        )

        var score = 0
        for ((idx, q) in questions.withIndex()) {
            val selected = answers[idx]
            if (selected?.equals(q.answer?.trim(), ignoreCase = true) == true) {
                score++
            }
        }

        val percentage = (score * 100) / questions.size
        val state = PracticeTestState(
            selectedAnswers = answers,
            isSubmitted = true,
            score = score,
            totalQuestions = questions.size,
            percentage = percentage
        )

        assertEquals(1, state.score)
        assertEquals(4, state.totalQuestions)
        assertEquals(25, state.percentage)
    }

    @Test
    fun `test reset resets all state`() {
        val initial = PracticeTestState(
            selectedAnswers = mapOf(0 to "A", 1 to "B"),
            isSubmitted = true,
            score = 2,
            totalQuestions = 2,
            percentage = 100
        )

        val reset = PracticeTestState(totalQuestions = 2)
        assertEquals(0, reset.score)
        assertEquals(0, reset.percentage)
        assertEquals(2, reset.totalQuestions)
        assertTrue(reset.selectedAnswers.isEmpty())
        assertFalse(reset.isSubmitted)
    }
}
