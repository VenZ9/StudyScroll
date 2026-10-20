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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val lessons by viewModel.displayedLessons.collectAsState()
    val document by viewModel.document.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val narrationState by viewModel.narrationState.collectAsState()

    var activeSourceLesson by remember { mutableStateOf<Lesson?>(null) }

    // Restore initial pager page from document.currentPosition
    val initialPage = document?.currentPosition?.coerceIn(0, (lessons.size - 1).coerceAtLeast(0)) ?: 0
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { lessons.size }
    )

    // Save position changes as user swipes through cards
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (lessons.isNotEmpty() && page in lessons.indices) {
                viewModel.updateCurrentPosition(page)
            }
        }
    }

    Scaffold(
        topBar = {
            StudyScrollTopBar(
                title = document?.title ?: "Lessons",
                onBackClick = onNavigateBack,
                actions = {
                    if (lessons.isNotEmpty()) {
                        Text(
                            text = "${pagerState.currentPage + 1} of ${lessons.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
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
            // Filter Tabs (All, Bookmarked, Review, Quizzes)
            val filters = listOf(
                FeedFilter.ALL to "All",
                FeedFilter.BOOKMARKED to "Bookmarked",
                FeedFilter.REVIEW_LATER to "Review Later",
                FeedFilter.QUIZZES to "Quizzes"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (filter, label) ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(label) }
                    )
                }
            }

            if (lessons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (currentFilter) {
                            FeedFilter.ALL -> "No lessons generated for this document yet."
                            FeedFilter.BOOKMARKED -> "No bookmarked lessons yet. Tap the bookmark icon on any lesson to save it here."
                            FeedFilter.REVIEW_LATER -> "No lessons marked for review."
                            FeedFilter.QUIZZES -> "No quiz cards generated for this chapter."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val lesson = lessons[page]
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

    activeSourceLesson?.let { lesson ->
        SourceEvidenceSheet(
            lesson = lesson,
            onDismiss = { activeSourceLesson = null }
        )
    }
}
