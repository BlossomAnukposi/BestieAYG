package com.bayg.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bayg
import com.bayg.R
import com.bayg.widgets.Caption
import com.bayg.widgets.GreenArrowButton
import com.bayg.widgets.GreenButton
import com.bayg.widgets.GreyOutlinedCard
import com.bayg.widgets.Heading1
import com.bayg.widgets.Heading3
import com.bayg.widgets.Paragraph
import com.bayg.widgets.ProgressBar
import com.bayg.widgets.Subtitle

@Composable
fun SignIn(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.bayg.black)
            .padding(40.dp, 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        // Section One
        Column {
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(334.dp).padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                GreenArrowButton(navController, "onboardingStart")
                Caption("Step 1 of 3")
            }
            ProgressBar(MaterialTheme.bayg.green, 0.33f)
        }

        // Section Two
        Column {
            Heading1("CRASH\nOUT.", MaterialTheme.bayg.green)
            Heading3("sign in", MaterialTheme.bayg.white)
            Subtitle("we need to know who you are before we snitch on you")
        }

        // Security Card
        GreyOutlinedCard () {
            Row {
                Image(painter = painterResource(id = R.drawable.security),
                    contentDescription = "Security icon",
                    modifier = Modifier.size(60.dp))
                Column {
                    Paragraph("OAuth2 PKCE + E2E encrypted", MaterialTheme.bayg.white, true)
                    Caption("Your data stays on-device. We pinky promise.")
                }
            }
        }
    }

    // Bottom Section
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp)
    ) {
        GreenButton(navController, "permissions", "Create an Account")
    }
}
