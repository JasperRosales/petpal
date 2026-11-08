package com.petpal.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

const val LOW_STAT = 30.0

@Composable
fun StatBar(label: String, icon: ImageVector, value: Double, color: Color, modifier: Modifier = Modifier) {
    val barColor = if (value < LOW_STAT) MaterialTheme.colorScheme.error else color
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = barColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(84.dp))
        LinearProgressIndicator(
            progress = { (value / 100.0).toFloat() },
            modifier = Modifier.weight(1f).height(8.dp),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${value.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
    }
}
