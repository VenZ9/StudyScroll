package app.studyscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.studyscroll.domain.model.ConfidenceLabel
import app.studyscroll.ui.theme.HighConfidenceColor
import app.studyscroll.ui.theme.MediumConfidenceColor
import app.studyscroll.ui.theme.UnverifiedConfidenceColor

@Composable
fun ConfidenceBadge(
    confidence: ConfidenceLabel,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (confidence) {
        ConfidenceLabel.HIGH -> "Verified Source" to HighConfidenceColor
        ConfidenceLabel.MEDIUM -> "Likely Grounded" to MediumConfidenceColor
        ConfidenceLabel.UNVERIFIED -> "Unverified" to UnverifiedConfidenceColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
