package com.bayg.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bayg
import com.bayg.widgets.NavBar

@Composable
fun Dashboard(navController: NavController) {
    Column (modifier = Modifier.fillMaxSize().background(MaterialTheme.bayg.black),
        ) {
        Text(
            text = "This is the dashboard page!",
            style = MaterialTheme.typography.bodyLarge
        )
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 0.dp)
    ) {
        NavBar(navController)
    }
}