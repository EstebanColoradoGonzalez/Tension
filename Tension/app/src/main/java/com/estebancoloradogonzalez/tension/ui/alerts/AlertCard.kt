package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.tension.domain.model.AlertItem
import com.estebancoloradogonzalez.tension.ui.components.AlertLevelIndicator

@Composable
fun AlertCard(
    alert: AlertItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = when (alert.level) {
        "CRISIS" -> if (isDark) Color(0xFF930009) else Color(0xFFFFDAD6)
        "HIGH_ALERT" -> if (isDark) Color(0xFF4E2600) else Color(0xFFFFF3E0)
        "MEDIUM_ALERT" -> if (isDark) Color(0xFF4A3800) else Color(0xFFFFF8E1)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when (alert.level) {
        "CRISIS" -> if (isDark) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.onErrorContainer
        else -> if (isDark) Color(0xFFE8E0DC) else Color(0xFF1C1B1B)
    }
    val elevation = if (alert.level == "CRISIS") {
        CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    } else {
        CardDefaults.cardElevation()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = elevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlertLevelIndicator(level = alert.level, size = 12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alertTypeDisplayName(alert.type),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                Text(
                    text = alert.entityName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                )
            }
        }
    }
}

private fun alertTypeDisplayName(type: String): String = when (type) {
    "PLATEAU" -> "Meseta detectada"
    "LOW_PROGRESSION_RATE" -> "Tasa de progresión baja"
    "RIR_OUT_OF_RANGE" -> "RIR fuera de rango"
    "LOW_ADHERENCE" -> "Adherencia baja"
    "TONNAGE_DROP" -> "Caída de tonelaje"
    "MODULE_INACTIVITY" -> "Inactividad por módulo"
    "MODULE_REQUIRES_DELOAD" -> "Módulo requiere descarga"
    else -> type
}
