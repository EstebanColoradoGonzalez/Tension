package com.estebancoloradogonzalez.tension

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.estebancoloradogonzalez.tension.ui.navigation.TensionNavHost
import com.estebancoloradogonzalez.tension.ui.theme.TensionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TensionTheme {
                TensionNavHost()
            }
        }
    }
}