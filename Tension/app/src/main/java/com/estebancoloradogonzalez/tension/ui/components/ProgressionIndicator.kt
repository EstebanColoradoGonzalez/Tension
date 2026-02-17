package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estebancoloradogonzalez.tension.domain.model.ProgressionClassification
import com.estebancoloradogonzalez.tension.ui.theme.LocalTensionSemanticColors

@Composable
fun ProgressionIndicator(classification: ProgressionClassification?) {
    val semanticColors = LocalTensionSemanticColors.current

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (classification) {
            ProgressionClassification.POSITIVE_PROGRESSION -> {
                Text(
                    text = "↑",
                    fontSize = 24.sp,
                    color = semanticColors.progressionPositive,
                )
            }
            ProgressionClassification.MAINTENANCE -> {
                Text(
                    text = "=",
                    fontSize = 24.sp,
                    color = semanticColors.maintenance,
                )
            }
            ProgressionClassification.REGRESSION -> {
                Text(
                    text = "↓",
                    fontSize = 24.sp,
                    color = semanticColors.regression,
                )
            }
            null -> { /* No icon for first session */ }
        }
    }
}
