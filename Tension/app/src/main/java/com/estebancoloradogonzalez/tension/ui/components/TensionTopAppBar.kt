package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.estebancoloradogonzalez.tension.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TensionTopAppBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            when {
                onNavigateBack != null -> {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                }
                onClose != null -> {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                }
            }
        },
    )
}
