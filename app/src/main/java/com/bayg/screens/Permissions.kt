package com.bayg.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bayg
import com.bayg.R
import com.bayg.permissions.PermissionManager
import com.bayg.widgets.Caption
import com.bayg.widgets.GreenArrowButton
import com.bayg.widgets.GreenButton
import com.bayg.widgets.GreyOutlinedCard
import com.bayg.widgets.Heading2
import com.bayg.widgets.OrangeTagCard
import com.bayg.widgets.Paragraph
import com.bayg.widgets.ProgressBar
import com.bayg.widgets.RedTagCard
import com.bayg.widgets.Subtitle
import com.bayg.widgets.Toggle
import com.bayg.widgets.PermissionToggle
import kotlinx.coroutines.delay

private const val PROGRESS_BAR_67_PERCENT = 0.67f
private const val PERMISSION_CHECK_INTERVAL_MS = 1000L

@Composable
fun Permissions(navController: NavController, permissionManager: PermissionManager) {
    // Track permission states
    val locationGranted = remember { mutableStateOf(permissionManager.hasLocationPermission()) }
    val usageStatsGranted = remember { mutableStateOf(permissionManager.hasUsageStatsPermission()) }

    // Periodically re-check permissions (in case user granted them externally)
    LaunchedEffect(Unit) {
        while (true) {
            delay(PERMISSION_CHECK_INTERVAL_MS)
            locationGranted.value = permissionManager.hasLocationPermission()
            usageStatsGranted.value = permissionManager.hasUsageStatsPermission()
        }
    }

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
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .width(334.dp)
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GreenArrowButton(navController, "verifyEmail")
                Caption("Step 2 of 3")
            }
            ProgressBar(MaterialTheme.bayg.green, PROGRESS_BAR_67_PERCENT)
        }

        // SECTION TWO
        Column {
            Heading2("one-time\nsetup", MaterialTheme.bayg.white)
            Subtitle("Crashout needs these to work. We never sell your data. Ever.")
        }

        // SETUP CARDS
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            InstagramUsageCard(
                isGranted = usageStatsGranted.value,
                onToggle = { permissionManager.requestUsageStatsPermission() }
            )
            LocationCard(
                isGranted = locationGranted.value,
                onToggle = { permissionManager.requestLocationPermissions() }
            )
            CalendarCard()
            NotificationsCard()
        }
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp)
    ) {
        GreenButton(navController, "appSetup", "Grant & continue")
    }
}

@Composable
fun InstagramUsageCard(isGranted: Boolean, onToggle: () -> Unit) {
    GreyOutlinedCard() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.phone),
                contentDescription = "Phone icon",
                modifier = Modifier.size(40.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.width(300.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Paragraph("Usage Access", bold = true)
                    RedTagCard("REQUIRED")
                }

                Row(
                    modifier = Modifier.width(250.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Caption("Track time in Instagram. Required for blocking.", 180.dp)
                    PermissionToggle(isGranted = isGranted, onToggle = { onToggle() })
                }
            }
        }
    }
}

@Composable
fun LocationCard(isGranted: Boolean, onToggle: () -> Unit) {
    GreyOutlinedCard() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.location),
                contentDescription = "Location icon",
                modifier = Modifier.size(40.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.width(300.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Paragraph("Location", bold = true)
                    RedTagCard("REQUIRED")
                }

                Row(
                    modifier = Modifier.width(250.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Caption("Find nearby parks for touch-grass alerts.", 180.dp)
                    PermissionToggle(isGranted = isGranted, onToggle = { onToggle() })
                }
            }
        }
    }
}

@Composable
fun CalendarCard() {
    GreyOutlinedCard() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.calendar),
                contentDescription = "Calendar icon",
                modifier = Modifier.size(40.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.width(300.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Paragraph("Google Calendar", bold = true)
                    OrangeTagCard("RECOMMENDED")
                }

                Row(
                    modifier = Modifier.width(250.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Caption("Detect exams, deadlines & projects to tighten limits.", 180.dp)
                    Toggle(false)
                }
            }
        }
    }
}

@Composable
fun NotificationsCard() {
    GreyOutlinedCard() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.phone),
                contentDescription = "Phone icon",
                modifier = Modifier.size(40.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.width(300.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Paragraph("Notifications", bold = true)
                    RedTagCard("REQUIRED")
                }

                Row(
                    modifier = Modifier.width(250.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Caption("Alert you when you've crashed out. Intentionally loud.", 180.dp)
                    Toggle(true)
                }
            }
        }
    }
}