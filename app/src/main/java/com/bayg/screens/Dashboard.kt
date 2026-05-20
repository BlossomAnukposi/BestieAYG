package com.bayg.screens


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Temporary blank dashboard screen used as the start destination.
 * Keep this minimal can be replaced with real Dashboard UI later.
 */
@Composable
fun Dashboard() {

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "This is the dashboard page!",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}