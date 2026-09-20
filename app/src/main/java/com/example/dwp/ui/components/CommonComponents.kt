package com.example.dwp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwp.theme.*

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "done", "completed" -> Triple(TealSuccessBg, TealSuccess, "Completed")
        "behind" -> Triple(RedAlertBg, RedAlert, "Behind")
        "cancelled" -> Triple(ChipGreyBg, TextMuted, "Cancelled")
        else -> Triple(Color(0xFFE4EAF0), NavySecondary, "In progress")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PriorityChip(priority: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (priority.lowercase()) {
        "high" -> Triple(RedAlertBg, RedAlert, "High")
        "medium" -> Triple(AmberWarningBg, AmberWarning, "Medium")
        else -> Triple(ChipGreyBg, TextMuted, "Low")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProgressBarWithText(
    progress: Int,
    modifier: Modifier = Modifier,
    barHeight: Int = 8
) {
    val clamped = progress.coerceIn(0, 100)
    val barColor = when {
        clamped >= 100 -> TealSuccess
        clamped <= 25 -> RedAlert
        else -> NavySecondary
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(barHeight.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SteelLight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped / 100f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$clamped%",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = NavyPrimary
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = NavyPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun LiveStatusBadge(
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (bg, fg, text) = if (isLive) {
        Triple(TealSuccessBg, TealSuccess, "Live — SharePoint")
    } else {
        Triple(AmberWarningBg, AmberWarning, "Local Storage")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, bg, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isLive) fg else fg.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
