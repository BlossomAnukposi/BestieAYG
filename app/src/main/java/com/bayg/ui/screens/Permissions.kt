package com.bayg.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bayg
import com.bayg.R
import com.bayg.managers.PermissionManager
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
import com.bayg.widgets.PermissionToggle
import com.bayg.widgets.PermissionsCard
import kotlinx.coroutines.delay

private const val PROGRESS_BAR_67_PERCENT = 0.67f
private const val PERMISSION_CHECK_INTERVAL_MS = 1000L

@Composable
fun Permissions(navController: NavController, permissionManager: PermissionManager) {
    val locationGranted = remember { mutableStateOf(permissionManager.hasLocationPermission()) }
    val usageStatsGranted = remember { mutableStateOf(permissionManager.hasUsageStatsPermission()) }
    val notificationsGranted = remember { mutableStateOf(permissionManager.hasNotificationsPermission()) }
    val accessibilityGranted = remember { mutableStateOf(permissionManager.hasAccessibilityPermission()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(PERMISSION_CHECK_INTERVAL_MS)
            locationGranted.value = permissionManager.hasLocationPermission()
            usageStatsGranted.value = permissionManager.hasUsageStatsPermission()
            notificationsGranted.value = permissionManager.hasNotificationsPermission()
            accessibilityGranted.value = permissionManager.hasAccessibilityPermission()
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

        Column {
            Heading2("one-time\nsetup", MaterialTheme.bayg.white)
            Subtitle("Crashout needs these to work. We never sell your data. Ever.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())) {
            PermissionsCard(
                isGranted = usageStatsGranted.value,
                onToggle = { permissionManager.requestUsageStatsPermission() },
                contentDescription = "Track time in Instagram. Required for blocking.",
                painterResource = painterResource(id = R.drawable.phone),
                title = "Usage Stats",
                caption = "Track time in Instagram. Required for blocking.",
                tagCard = { RedTagCard("REQUIRED") }
            )
            PermissionsCard(
                isGranted = locationGranted.value,
                onToggle = { permissionManager.requestLocationPermissions() },
                contentDescription = "Find nearby parks for touch-grass alerts.",
                painterResource = painterResource(id = R.drawable.location),
                title = "Location",
                caption = "Find nearby parks for touch-grass alerts.",
                tagCard = { RedTagCard("REQUIRED") }
            )
            PermissionsCard(
                isGranted = notificationsGranted.value,
                onToggle = { permissionManager.requestNotificationsPermission() },
                contentDescription = "Alert you when you've crashed out. Intentionally loud.",
                painterResource = painterResource(id = R.drawable.phone),
                title = "Notifications",
                caption = "Alert you when you've crashed out. Intentionally loud.",
                tagCard = { OrangeTagCard("RECOMMENDED") }
            )
            PermissionsCard(
                isGranted = accessibilityGranted.value,
                onToggle = { permissionManager.requestAccessibilityPermission() },
                contentDescription = "Allow Crashout to block Instagram usage",
                painterResource = painterResource(id = R.drawable.settings),
                title = "Accessibility",
                caption = "Allow Crashout to block Instagram usage",
                tagCard = { OrangeTagCard("RECOMMENDED") }
            )
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