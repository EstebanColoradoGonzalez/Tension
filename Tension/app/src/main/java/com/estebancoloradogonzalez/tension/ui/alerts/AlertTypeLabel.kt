package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.estebancoloradogonzalez.tension.R

/**
 * Title an alert shows in the alert center and in its detail. It reads as what happened
 * to the executant, not as the condition the engine evaluated, and it lives in a single
 * place so both screens cannot drift apart.
 */
@Composable
fun alertTypeDisplayName(type: String): String = when (type) {
    "PLATEAU" -> stringResource(R.string.alert_type_plateau)
    "LOW_PROGRESSION_RATE" -> stringResource(R.string.alert_type_low_progression)
    "RIR_OUT_OF_RANGE" -> stringResource(R.string.alert_type_rir_out_of_range)
    "LOW_ADHERENCE" -> stringResource(R.string.alert_type_low_adherence)
    "TONNAGE_DROP" -> stringResource(R.string.alert_type_tonnage_drop)
    "ROUTINE_INACTIVITY" -> stringResource(R.string.alert_type_routine_inactivity)
    "ROUTINE_REQUIRES_DELOAD" -> stringResource(R.string.alert_type_routine_requires_deload)
    else -> stringResource(R.string.alert_center_title)
}
