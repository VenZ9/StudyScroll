package app.studyscroll.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.studyscroll.domain.model.Lesson
import app.studyscroll.ui.components.SourceEvidenceSheet
import app.studyscroll.ui.components.StudyScrollTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateBack: () -> Unit
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val microLessonFilter by viewModel.microLessonFilter.collectAsState()
    val document by viewModel.document.collectAsState()
    val narrationState by viewModel.narrationState.collectAsState()
    val practiceTestState by viewModel.practiceTestState.collectAsState()

    val microLessons by viewModel.displayedMicroLessons.collectAsState()
    val summaryLessons by viewModel.summaryLessons.collectAsState()
    val shortNotes by viewModel.shortNotes.collectAsState()
    val keyTopics by viewModel.keyTopics.collectAsState()
    val quizLessons by viewModel.quizLessons.collectAsState()

    var activeSourceLesson by remember { mutableStateOf<Lesson?>(null) }

    // Active lesson list depending on the selected section
    val activeLessons = when (currentSection) {
        StudySection.MICRO_LESSONS -> microLessons
        StudySection.SUMMARY -> summaryLessons
        StudySection.SHORT_NOTES -> shortNotes
        StudySection.KEY_TOPICS -> keyTopics
        StudySection.MCQ_QUIZZES -> quizLessons
        StudySection.PRACTICE_TEST -> quizLessons
    }

    val initialMicroPage = document?.currentPosition?.coerceIn(0, (microLessons.size - 1).coerceAtLeast(0)) ?: 0

    Scaffold(
        topBar = {
            StudyScrollTopBar(
                title = document?.title ?: "StudyScroll",
                onBackClick = onNavigateBack,
                actions = {
                    if (currentSection == StudySection.PRACTICE_TEST) {
                        if (practiceTestState.isSubmitted) {
                            Text(
                                text = "Score: ${practiceTestState.score}/${practiceTestState.totalQuestions} (${practiceTestState.percentage}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        } else if (quizLessons.isNotEmpty()) {
                            Text(
                                text = "${quizLessons.size} Questions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable Section Tabs
            ScrollableTabRow(
                selectedTabIndex = currentSection.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                StudySection.values().forEach { section ->
                    Tab(
                        selected = currentSection == section,
                        onClick = { viewModel.setSection(section) },
                        text = {
                            Text(
                                text = section.label,
                                fontWeight = if (currentSection == section) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Sub-filter row for Micro-Lessons
            if (currentSection == StudySection.MICRO_LESSONS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MicroLessonFilter.values().forEach { filter ->
                        FilterChip(
                            selected = microLessonFilter == filter,
                            onClick = { viewModel.setMicroLessonFilter(filter) },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            // Content Area
            if (currentSection == StudySection.PRACTICE_TEST) {
                PracticeTestView(
                    questions = quizLessons,
                    state = practiceTestState,
                    onSelectAnswer = { index, answer -> viewModel.selectPracticeAnswer(index, answer) },
                    onSubmit = { viewModel.submitPracticeTest() },
                    onReset = { viewModel.resetPracticeTest() },
                    onViewSource = { activeSourceLesson = it }
                )
            } else {
                key(currentSection) {
                    val pagerState = rememberPagerState(
                        initialPage = if (currentSection == StudySection.MICRO_LESSONS) initialMicroPage else 0,
                        pageCount = { activeLessons.size }
                    )

                    // Track position for micro-lessons
                    if (currentSection == StudySection.MICRO_LESSONS) {
                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.currentPage }.collect { page ->
                                if (microLessons.isNotEmpty() && page in microLessons.indices) {
                                    viewModel.updateCurrentPosition(page)
                                }
                            }
                        }
                    }

                    if (activeLessons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyMessage = when (currentSection) {
                                StudySection.MICRO_LESSONS -> when (microLessonFilter) {
                                    MicroLessonFilter.ALL -> "No micro-lessons generated for this document yet."
                                    MicroLessonFilter.BOOKMARKED -> "No bookmarked lessons yet. Tap the bookmark icon on any lesson to save it here."
                                    MicroLessonFilter.REVIEW_LATER -> "No lessons marked for review."
                                }
                                StudySection.SUMMARY -> "No chapter summary available for this document."
                                StudySection.SHORT_NOTES -> "No exam-ready short notes generated yet."
                                StudySection.KEY_TOPICS -> "No key topics extracted for this chapter."
                                StudySection.MCQ_QUIZZES -> "No quiz cards generated for this chapter."
                                StudySection.PRACTICE_TEST -> "No practice test available."
                            }
                            Text(
                                text = emptyMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Section page indicator
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} of ${activeLessons.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            VerticalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                val lesson = activeLessons[page]
                                LessonCard(
                                    lesson = lesson,
                                    narrationState = narrationState,
                                    onBookmarkToggle = { viewModel.toggleBookmark(lesson) },
                                    onUnderstoodToggle = { viewModel.toggleUnderstood(lesson) },
                                    onSourceClick = { activeSourceLesson = lesson },
                                    onNarrationToggle = { viewModel.toggleNarration(lesson) },
                                    onQuizAnswer = { selectedAnswer -> viewModel.answerQuiz(lesson, selectedAnswer) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeSourceLesson?.let { lesson ->
        SourceEvidenceSheet(
            lesson = lesson,
            onDismiss = { activeSourceLesson = null }
        )
    }
}
