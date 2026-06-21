package com.bayg.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bayg
import com.bayg.auth.OnboardingStore
import com.bayg.services.storage.UserSettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.bayg.widgets.Caption
import com.bayg.widgets.GreenArrowButton
import com.bayg.widgets.GreenButton
import com.bayg.widgets.GreyCard
import com.bayg.widgets.GreyOutlinedCard
import com.bayg.widgets.Heading2
import com.bayg.widgets.Heading3
import com.bayg.widgets.LimitSlider
import com.bayg.widgets.Paragraph
import com.bayg.widgets.ProgressBar
import com.bayg.widgets.SelectableCard
import com.bayg.widgets.Subtitle
import com.bayg.widgets.ToggleCard
import kotlin.math.roundToInt

private const val SECONDS_IN_MINUTE = 60

private fun formatMinutes(value: Float): String {
    val m = value.roundToInt()
    return if (m < SECONDS_IN_MINUTE) "$m min"
    else {
        val h = m / SECONDS_IN_MINUTE
        val rem = m % SECONDS_IN_MINUTE
        if (rem == 0) "$h h" else "$h h $rem min"
    }
}

private const val DEFAULT_APP_LIMIT = 60f
private const val DEFAULT_BLOCK_TIME = 30f

@Composable
fun AppSetup(
    navController: NavController,
    settingsViewModel: UserSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    var isPreset by remember { mutableStateOf(true) }
    var limitValue by remember { mutableFloatStateOf(DEFAULT_APP_LIMIT) }
    var blockValue by remember { mutableFloatStateOf(DEFAULT_BLOCK_TIME) }
    var customKeywords by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(settingsViewModel.settings) {
        settingsViewModel.settings?.let { settings ->
            limitValue = settings.dailyLimitMinutes.toFloat()
            blockValue = settings.blockDurationMinutes.toFloat()
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
        // TOP SECTION
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(334.dp).padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GreenArrowButton(navController, "permissions")
                Caption("Step 3 of 3")
            }
            ProgressBar(MaterialTheme.bayg.green, 1f)
        }

        // TITLE
        Column {
            Heading2("customise\nyour limits", MaterialTheme.bayg.white)
            Subtitle("Set how Crashout decides when to intervene. You can always change this later.")
        }

        // CARDS
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            AppLimitSection(isPreset, limitValue) { limitValue = it }
            AppBlockSection(isPreset, blockValue) { blockValue = it }
        }

        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            ModeSection(isPreset) { isPreset = it }
        }

    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp)
    ) {
        GreenButton(
            onClick = {
                settingsViewModel.saveLimits(
                    dailyLimitMinutes = limitValue.roundToInt(),
                    blockDurationMinutes = blockValue.roundToInt(),
                ) {
                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                        OnboardingStore.markComplete(context, uid)
                    }
                    navController.navigate("dashboard") {
                        popUpTo("onboardingStart") { inclusive = true }
                    }
                }
            },
            text = if (settingsViewModel.isSaving) "Saving..." else "All done! Let's go",
            enabled = !settingsViewModel.isSaving,
        )
    }
}

private const val APP_LIMIT_OPTIONS_COUNT = 5

private const val THREE_HOURS_LIMIT = 180f

@Composable
fun AppLimitSection(isPreset: Boolean, value: Float, onValueChange: (Float) -> Unit) {
    GreyOutlinedCard(120.dp) {
        Column {
            Subtitle("Daily Instagram Limit")
            Row(verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Heading3(formatMinutes(value), MaterialTheme.bayg.white)
                Paragraph("per day", MaterialTheme.bayg.textGrey)
            }
            if (!isPreset) {
                Column(Modifier.fillMaxWidth()) {
                    LimitSlider(MaterialTheme.bayg.green, value,
                        APP_LIMIT_OPTIONS_COUNT, 0f..THREE_HOURS_LIMIT, onValueChange)
                }
            }
        }
    }
}

private const val BLOCK_TIME_OPTIONS_COUNT = 47

private const val FOUR_HOURS_LIMIT = 240f

@Composable
fun AppBlockSection(isPreset: Boolean, value: Float, onValueChange: (Float) -> Unit) {
    GreyOutlinedCard(120.dp) {
        Column {
            Subtitle("Block Duration")
            Row(verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Heading3(formatMinutes(value), MaterialTheme.bayg.white)
                Paragraph("block time", MaterialTheme.bayg.textGrey)
            }
            if (!isPreset) {
                Column(Modifier.fillMaxWidth()) {
                    LimitSlider(MaterialTheme.bayg.lightRed, value,
                        BLOCK_TIME_OPTIONS_COUNT, 0f..FOUR_HOURS_LIMIT, onValueChange)
                }
            }
        }
    }
}

@Composable
fun ModeSection(isPreset: Boolean, onModeChange: (Boolean) -> Unit) {
    Column {
        Paragraph("Mode", bold = true)
        GreyCard {
            Row(horizontalArrangement = Arrangement.Center) {
                ToggleCard("Custom", selected = !isPreset) { onModeChange(false) }
                ToggleCard("Preset", selected = isPreset)  { onModeChange(true) }
            }
        }
    }
}