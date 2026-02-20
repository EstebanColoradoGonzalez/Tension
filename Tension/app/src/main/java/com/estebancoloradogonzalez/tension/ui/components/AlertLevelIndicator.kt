package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun AlertLevelIndicator(
    level: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val color = when (level) {
        "CRISIS" -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
        "HIGH_ALERT" -> if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
        "MEDIUM_ALERT" -> if (isDark) Color(0xFFFFD54F) else Color(0xFF8D6E00)
        else -> Color.Gray
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}
