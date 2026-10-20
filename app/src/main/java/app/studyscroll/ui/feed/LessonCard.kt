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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.studyscroll.domain.model.Lesson
import app.studyscroll.domain.model.LessonType
import app.studyscroll.narration.NarrationState
import app.studyscroll.ui.quiz.QuizCard

@Composable
fun LessonCard(
    lesson: Lesson,
    narrationState: NarrationState,
    onBookmarkToggle: () -> Unit,
    onUnderstoodToggle: () -> Unit,
    onSourceClick: () -> Unit,
    onNarrationToggle: () -> Unit,
    onQuizAnswer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = narrationState is NarrationState.Playing && narrationState.lessonId == lesson.id

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Scrollable Content Area (occupying ~85% of card)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Type Badge & Source line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LessonTypeBadge(type = lesson.type)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = lesson.formattedSourceLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // If Quiz, display QuizCard
                if (lesson.type == LessonType.QUIZ) {
                    QuizCard(
                        lesson = lesson,
                        onAnswerSubmitted = onQuizAnswer
                    )
                } else {
                    // Regular lesson body
                    Text(
                        text = lesson.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Bullets if present
                    if (lesson.bullets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        lesson.bullets.forEach { bullet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Example box if present
                    if (!lesson.example.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Example",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lesson.example!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Why it matters / memory cue
                    if (!lesson.whyItMatters.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Why this matters: ${lesson.whyItMatters}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Divider & Subtle Bottom Controls (touch targets ~48dp)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Source Evidence button
                IconButton(onClick = onSourceClick) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "View Source Evidence",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Narration button
                IconButton(onClick = onNarrationToggle) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop Narration" else "Narrate Lesson",
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Understood / Review Later toggle
                IconButton(onClick = onUnderstoodToggle) {
                    val (icon, tint) = when (lesson.understood) {
                        true -> Icons.Default.Check to Color(0xFF15803D)
                        false -> Icons.Default.Flag to Color(0xFFB45309)
                        null -> Icons.Default.Check to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Mark Understood / Review",
                        tint = tint
                    )
                }

                // Bookmark toggle
                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        imageVector = if (lesson.bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (lesson.bookmarked) "Remove Bookmark" else "Bookmark Lesson",
                        tint = if (lesson.bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LessonTypeBadge(type: LessonType) {
    val label = when (type) {
        LessonType.DEFINITION -> "Definition"
        LessonType.KEY_FACT -> "Key Fact"
        LessonType.PROCESS -> "Process / Steps"
        LessonType.COMPARISON -> "Comparison"
        LessonType.EXAMPLE -> "Example"
        LessonType.FORMULA -> "Formula"
        LessonType.DIAGRAM_NOTE -> "Diagram Note"
        LessonType.QUIZ -> "Quiz"
        LessonType.SUMMARY -> "Overview"
        LessonType.VOCABULARY -> "Vocabulary"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
