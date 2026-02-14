package com.estebancoloradogonzalez.tension.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.components.TensionTopAppBar

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
) {
    Scaffold(
        topBar = {
            TensionTopAppBar(
                title = stringResource(R.string.settings_title),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_edit_profile)) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable { onNavigateToProfile() },
            )
            HorizontalDivider()
        }
    }
}
