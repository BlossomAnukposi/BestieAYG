package com.bayg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bayg
import com.bayg.widgets.Caption
import com.bayg.widgets.GreenButton
import com.bayg.widgets.Paragraph
import com.bayg.widgets.Subtitle
import com.bayg.widgets.Title

@Composable
fun OnboardingStart(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.bayg.black)
            .padding(40.dp, 50.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        // Section One
        Column(Modifier.padding(top = 50.dp).width(334.dp)) {
            Title("CRASH", MaterialTheme.bayg.green)
            Title("OUT.", MaterialTheme.bayg.white)

            Subtitle("your bestie that actually calls you out")
        }

        // Section Two
        Column(Modifier.padding(top = 50.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(2.dp)
                    .background(MaterialTheme.bayg.green)
            ) {}

            Paragraph(
                "instagram blocker · weather-aware · calendar-smart · cryptographically secure",
                MaterialTheme.bayg.textGrey
            )
        }
    }

    // Bottom Section
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GreenButton(navController, "signUp", "Get Started →")
            Caption("v1.0.0 · no cap, all accountability", align = TextAlign.Center)
        }
    }
}
