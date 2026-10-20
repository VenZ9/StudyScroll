package app.studyscroll.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.studyscroll.domain.model.Lesson

@Composable
fun QuizCard(
    lesson: Lesson,
    onAnswerSubmitted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val correctAnswer = lesson.answer?.trim() ?: ""
    val hasAnswered = selectedOption != null

    val options = if (lesson.options.isNotEmpty()) lesson.options
    else if (!lesson.answer.isNullOrBlank()) listOf(lesson.answer!!, "False", "True").distinct().take(4)
    else listOf("True", "False")

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Active Recall Question",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = lesson.question ?: lesson.body,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        options.forEach { option ->
            val isThisSelected = selectedOption == option
            val isCorrect = option.equals(correctAnswer, ignoreCase = true)

            val backgroundColor = when {
                !hasAnswered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isCorrect -> Color(0xFF15803D).copy(alpha = 0.15f)
                isThisSelected -> Color(0xFFB91C1C).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }

            val borderColor = when {
                !hasAnswered -> Color.Transparent
                isCorrect -> Color(0xFF15803D)
                isThisSelected -> Color(0xFFB91C1C)
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable(enabled = !hasAnswered) {
                        selectedOption = option
                        onAnswerSubmitted(option)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasAnswered) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Correct",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isThisSelected) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Incorrect",
                                tint = Color(0xFFB91C1C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (hasAnswered && !lesson.explanation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lesson.explanation!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
