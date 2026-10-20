package app.studyscroll.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studyscroll.data.repository.DocumentRepository
import app.studyscroll.data.repository.LessonRepository
import app.studyscroll.data.repository.SettingsRepository
import app.studyscroll.domain.model.Document
import app.studyscroll.domain.model.Lesson
import app.studyscroll.narration.NarrationManager
import app.studyscroll.narration.NarrationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StudySection(val label: String) {
    MICRO_LESSONS("Micro-Lessons"),
    SUMMARY("Chapter Summary"),
    SHORT_NOTES("Short Notes"),
    KEY_TOPICS("Key Topics"),
    MCQ_QUIZZES("MCQ Quizzes"),
    PRACTICE_TEST("Practice Test")
}

enum class MicroLessonFilter(val label: String) {
    ALL("All Cards"),
    BOOKMARKED("Bookmarked"),
    REVIEW_LATER("Review Later")
}

data class PracticeTestState(
    val selectedAnswers: Map<Int, String> = emptyMap(),
    val isSubmitted: Boolean = false,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val percentage: Int = 0
)

class FeedViewModel(
    val documentId: String,
    private val lessonRepository: LessonRepository,
    private val documentRepository: DocumentRepository,
    private val settingsRepository: SettingsRepository,
    val narrationManager: NarrationManager
) : ViewModel() {

    private val _currentSection = MutableStateFlow(StudySection.MICRO_LESSONS)
    val currentSection: StateFlow<StudySection> = _currentSection.asStateFlow()

    private val _microLessonFilter = MutableStateFlow(MicroLessonFilter.ALL)
    val microLessonFilter: StateFlow<MicroLessonFilter> = _microLessonFilter.asStateFlow()

    val summaryLessons: StateFlow<List<Lesson>> = lessonRepository.getSummaryLessons(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortNotes: StateFlow<List<Lesson>> = lessonRepository.getShortNotes(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val keyTopics: StateFlow<List<Lesson>> = lessonRepository.getKeyTopics(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizLessons: StateFlow<List<Lesson>> = lessonRepository.getQuizLessons(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawMicroLessons = lessonRepository.getMicroLessons(documentId)
    private val bookmarkedLessons = lessonRepository.getBookmarkedLessons(documentId)
    private val reviewLessons = lessonRepository.getReviewLessons(documentId)

    val displayedMicroLessons: StateFlow<List<Lesson>> = combine(
        _microLessonFilter,
        rawMicroLessons,
        bookmarkedLessons,
        reviewLessons
    ) { filter, all, bookmarked, review ->
        when (filter) {
            MicroLessonFilter.ALL -> all
            MicroLessonFilter.BOOKMARKED -> bookmarked
            MicroLessonFilter.REVIEW_LATER -> review
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _document = MutableStateFlow<Document?>(null)
    val document: StateFlow<Document?> = _document.asStateFlow()

    val narrationState: StateFlow<NarrationState> = narrationManager.state

    // Practice test state
    private val _practiceTestState = MutableStateFlow(PracticeTestState())
    val practiceTestState: StateFlow<PracticeTestState> = _practiceTestState.asStateFlow()

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            _document.value = documentRepository.getDocument(documentId)
        }
    }

    fun setSection(section: StudySection) {
        _currentSection.value = section
        if (section == StudySection.PRACTICE_TEST && !_practiceTestState.value.isSubmitted && _practiceTestState.value.selectedAnswers.isEmpty()) {
            val total = quizLessons.value.size
            _practiceTestState.value = PracticeTestState(totalQuestions = total)
        }
    }

    fun setMicroLessonFilter(filter: MicroLessonFilter) {
        _microLessonFilter.value = filter
    }

    fun updateCurrentPosition(position: Int) {
        viewModelScope.launch {
            documentRepository.updateCurrentPosition(documentId, position)
        }
    }

    fun toggleBookmark(lesson: Lesson) {
        viewModelScope.launch {
            lessonRepository.updateBookmark(lesson.id, !lesson.bookmarked)
        }
    }

    fun toggleUnderstood(lesson: Lesson) {
        viewModelScope.launch {
            val newStatus = when (lesson.understood) {
                true -> false
                false -> null
                null -> true
            }
            lessonRepository.updateUnderstood(lesson.id, newStatus)
        }
    }

    fun answerQuiz(lesson: Lesson, selectedAnswer: String) {
        viewModelScope.launch {
            val isCorrect = selectedAnswer.equals(lesson.answer?.trim(), ignoreCase = true)
            lessonRepository.recordQuizAttempt(lesson.id, selectedAnswer, isCorrect)
            lessonRepository.updateUnderstood(lesson.id, isCorrect)
        }
    }

    fun selectPracticeAnswer(questionIndex: Int, answer: String) {
        if (_practiceTestState.value.isSubmitted) return
        val currentAnswers = _practiceTestState.value.selectedAnswers.toMutableMap()
        currentAnswers[questionIndex] = answer
        _practiceTestState.value = _practiceTestState.value.copy(selectedAnswers = currentAnswers)
    }

    fun submitPracticeTest() {
        val questions = quizLessons.value
        if (questions.isEmpty()) return

        var score = 0
        for ((idx, question) in questions.withIndex()) {
            val selected = _practiceTestState.value.selectedAnswers[idx]
            val isCorrect = selected?.equals(question.answer?.trim(), ignoreCase = true) == true
            if (isCorrect) score++
            selected?.let { ans ->
                viewModelScope.launch {
                    lessonRepository.recordQuizAttempt(question.id, ans, isCorrect)
                }
            }
        }

        val percentage = if (questions.isNotEmpty()) (score * 100) / questions.size else 0
        _practiceTestState.value = _practiceTestState.value.copy(
            isSubmitted = true,
            score = score,
            totalQuestions = questions.size,
            percentage = percentage
        )
    }

    fun resetPracticeTest() {
        val total = quizLessons.value.size
        _practiceTestState.value = PracticeTestState(totalQuestions = total)
    }

    fun toggleNarration(lesson: Lesson) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val textToSpeak = buildString {
                append(lesson.title).append(". ")
                append(lesson.body).append(". ")
                if (lesson.bullets.isNotEmpty()) {
                    lesson.bullets.forEach { append(it).append(". ") }
                }
                lesson.whyItMatters?.let { append("Why this matters: ").append(it) }
            }
            narrationManager.speak(lesson.id, textToSpeak, settings.narrationSpeechRate)
        }
    }

    override fun onCleared() {
        super.onCleared()
        narrationManager.stop()
    }
}
