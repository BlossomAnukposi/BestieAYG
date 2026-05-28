package com.bayg.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bayg
import com.bayg.widgets.Caption
import com.bayg.widgets.GreenArrowButton
import com.bayg.widgets.GreenButton
import com.bayg.widgets.GreyOutlinedCard
import com.bayg.widgets.Heading2
import com.bayg.widgets.Heading3
import com.bayg.widgets.Paragraph
import com.bayg.widgets.Subtitle
import com.bayg.widgets.LimitSlider
import kotlin.math.roundToInt

private const val DEFAULT_SCREEN_TIME_MINUTES = 60f
private const val MAX_SCREEN_TIME_MINUTES = 180f
private const val DEFAULT_BLOCK_THRESHOLD_MINUTES = 15f
private const val MAX_BLOCK_THRESHOLD_MINUTES = 120f

private fun formatMinutes(value: Float): String {
    val m = value.roundToInt()
    return if (m < 60) "$m min"
    else {
        val h = m / 60
        val rem = m % 60
        if (rem == 0) "$h h" else "$h h $rem min"
    }
}

@Composable
fun ProfileSettings(navController: NavController) {
    var username by remember { mutableStateOf("Your name") }
    var email by remember { mutableStateOf("you@example.com") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordMasked by remember { mutableStateOf("••••••••") }

    var screenTime by remember { mutableFloatStateOf(DEFAULT_SCREEN_TIME_MINUTES) }
    var blockThreshold by remember { mutableFloatStateOf(DEFAULT_BLOCK_THRESHOLD_MINUTES) }

    // Keywords state
    var keywords by remember { mutableStateOf(listOf("Exam", "Deadline")) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var newKeywordInput by remember { mutableStateOf("") }

    // Make content scrollable and leave space at bottom so Save button doesn't overlap
    val scrollState = rememberScrollState()
    val bottomContentPadding = 120.dp // leave space for fixed button

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.bayg.black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(40.dp, 50.dp)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top: back arrow + caption
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(334.dp).padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GreenArrowButton(navController, "dashboard") // go back to dashboard
                Caption("Account")
            }

            // Title
            Column {
                Heading2("profile\nsettings", MaterialTheme.bayg.white)
                Subtitle("Manage your account and limits. Changes here won't be saved yet.")
            }

            // Username card
            GreyOutlinedCard {
                Column {
                    Paragraph("Username", bold = true)
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text("Your name") },
                        singleLine = true
                    )
                    Caption("Displayed on leaderboards and social features", align = androidx.compose.ui.text.style.TextAlign.Start)
                }
            }

            // Email card
            GreyOutlinedCard {
                Column {
                    Paragraph("Email", bold = true)
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text("you@example.com") },
                        singleLine = true
                    )
                    Caption("Used for account recovery and notifications", align = androidx.compose.ui.text.style.TextAlign.Start)
                }
            }

            // Password card
            GreyOutlinedCard {
                Column {
                    Paragraph("Password", bold = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Heading3(passwordMasked, MaterialTheme.bayg.white)
                        OutlinedCard(
                            onClick = { showPasswordDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "Change",
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.bayg.green
                            )
                        }
                    }
                    Caption("Keep it strong — we won't show it here", 180.dp)
                }
            }

            // Screen time limit card (reusing LimitSlider to keep consistent vibe)
            GreyOutlinedCard {
                Column {
                    Paragraph("Daily Instagram Limit", bold = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Heading3(formatMinutes(screenTime), MaterialTheme.bayg.white)
                        Caption("per day", align = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    // LimitSlider matches other screens' UI — no logic tied yet
                    LimitSlider(MaterialTheme.bayg.green, screenTime, 5, 0f..MAX_SCREEN_TIME_MINUTES) {
                        screenTime = it
                    }
                }
            }

            // Block threshold card
            GreyOutlinedCard {
                Column {
                    Paragraph("Block threshold", bold = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Heading3(formatMinutes(blockThreshold), MaterialTheme.bayg.white)
                        Caption("time in one session before blocking", align = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    LimitSlider(MaterialTheme.bayg.lightRed, blockThreshold, 6, 0f..MAX_BLOCK_THRESHOLD_MINUTES) {
                        blockThreshold = it
                    }
                }
            }

            // Keywords section (add/remove)
            GreyOutlinedCard(minHeight = 200.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Paragraph("Event Keywords", bold = true)
                    Caption("Crashout watches for these in your Calendar", MaterialTheme.bayg.textGrey)

                    // Display keywords with better spacing and larger cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            keywords.forEach { kw ->
                                OutlinedCard(
                                    onClick = { /* maybe toggle selection later */ },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = kw,
                                            color = MaterialTheme.bayg.white,
                                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                                        )
                                        // Remove "×" button — easier to tap
                                        Text(
                                            text = "×",
                                            color = MaterialTheme.bayg.lightRed,
                                            modifier = Modifier
                                                .clickable {
                                                    keywords = keywords.filterNot { it == kw }
                                                }
                                                .padding(start = 4.dp),
                                            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                                        )
                                    }
                                }
                            }

                            // Add keyword card
                            OutlinedCard(
                                onClick = { showAddKeywordDialog = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = "+ Add",
                                    color = MaterialTheme.bayg.textGrey,
                                    modifier = Modifier.padding(12.dp),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Add bottom padding so the scrollable content can be scrolled above the fixed button
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = bottomContentPadding))
        } // end scrollable column
        // Fixed bottom Save button (overlaid visually at bottom)
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            GreenButton(navController, "dashboard", "Save changes")
        }
    }

    // Simple dialog to change password (UI only)
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change password") },
            text = {
                Column {
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = { Text("New password") },
                        singleLine = true
                    )
                    Caption("Password rules: at least 8 characters", align = androidx.compose.ui.text.style.TextAlign.Start)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPassword.isNotBlank()) {
                        passwordMasked = "••••••••"
                        // no persistence: just close dialog
                    }
                    newPassword = ""
                    showPasswordDialog = false
                }) { Text("Save", color = MaterialTheme.bayg.green) }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add keyword dialog
    if (showAddKeywordDialog) {
        AlertDialog(
            onDismissRequest = { showAddKeywordDialog = false },
            title = { Text("Add keyword") },
            text = {
                Column {
                    TextField(
                        value = newKeywordInput,
                        onValueChange = { newKeywordInput = it },
                        placeholder = { Text("e.g. Interview") },
                        singleLine = true
                    )
                    Caption("Crashout will watch for these words in your Calendar", align = androidx.compose.ui.text.style.TextAlign.Start)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newKeywordInput.trim()
                    if (trimmed.isNotEmpty() && !keywords.contains(trimmed)) {
                        keywords = keywords + trimmed
                    }
                    newKeywordInput = ""
                    showAddKeywordDialog = false
                }) { Text("Add", color = MaterialTheme.bayg.green) }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeywordDialog = false }) { Text("Cancel") }
            }
        )
    }
}