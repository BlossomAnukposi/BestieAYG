package com.bayg.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bayg.services.NoAsAService

@Composable
fun Dashboard() {
    val messageState = produceState(initialValue = "Loading...") {
        value = try {
            NoAsAService.fetchMessage()
        } catch (e: Exception) {
            "Error: ${e.message ?: "unknown"}"
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val current = messageState.value
        if (current == "Loading...") {
            CircularProgressIndicator()
        } else {
            Text(
                text = current,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}