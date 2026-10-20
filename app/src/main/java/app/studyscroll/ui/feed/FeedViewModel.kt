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

enum class FeedFilter {
    ALL,
    BOOKMARKED,
    REVIEW_LATER,
    QUIZZES
}

class FeedViewModel(
    val documentId: String,
    private val lessonRepository: LessonRepository,
    private val documentRepository: DocumentRepository,
    private val settingsRepository: SettingsRepository,
    val narrationManager: NarrationManager
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(FeedFilter.ALL)
    val currentFilter: StateFlow<FeedFilter> = _currentFilter.asStateFlow()

    private val allLessons = lessonRepository.getLessonsForDocument(documentId)
    private val bookmarkedLessons = lessonRepository.getBookmarkedLessons(documentId)
    private val reviewLessons = lessonRepository.getReviewLessons(documentId)
    private val quizLessons = lessonRepository.getQuizLessons(documentId)

    val displayedLessons: StateFlow<List<Lesson>> = combine(
        _currentFilter,
        allLessons,
        bookmarkedLessons,
        reviewLessons,
        quizLessons
    ) { filter, all, bookmarked, review, quizzes ->
        when (filter) {
            FeedFilter.ALL -> all
            FeedFilter.BOOKMARKED -> bookmarked
            FeedFilter.REVIEW_LATER -> review
            FeedFilter.QUIZZES -> quizzes
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _document = MutableStateFlow<Document?>(null)
    val document: StateFlow<Document?> = _document.asStateFlow()

    val narrationState: StateFlow<NarrationState> = narrationManager.state

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            _document.value = documentRepository.getDocument(documentId)
        }
    }

    fun setFilter(filter: FeedFilter) {
        _currentFilter.value = filter
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
