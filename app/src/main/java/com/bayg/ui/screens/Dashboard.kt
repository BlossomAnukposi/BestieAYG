package com.bayg.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bayg
import com.bayg.widgets.NavBar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.bayg.TouchGrassActivity
import com.bayg.location.DeviceLocationProvider
import com.bayg.services.NoAsAService
import com.bayg.services.storage.StreakViewModel
import com.bayg.services.storage.sync.SyncWorker
import com.bayg.ui.viewmodel.UserSettingsViewModel
import com.bayg.ui.viewmodel.NearestParkViewModel
import com.bayg.ui.viewmodel.StatsUiState
import com.bayg.ui.viewmodel.StatsViewModel
import com.bayg.ui.viewmodel.StatsViewModelFactory
import com.bayg.ui.viewmodel.NearestParkUiState
import com.bayg.ui.viewmodel.WeatherUiState
import com.bayg.ui.viewmodel.WeatherViewModel
import com.bayg.widgets.GreenButton
import com.bayg.widgets.Heading3
import com.bayg.widgets.Heading4
import com.bayg.widgets.Paragraph
import com.bayg.widgets.SmallInfoCard
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.get

@Composable
fun Dashboard(navController: NavController) {
    val context = LocalContext.current
    val viewModel: UserSettingsViewModel = viewModel()
    val displayName = viewModel.displayName

    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val statsViewModel: StatsViewModel = viewModel(
        factory = StatsViewModelFactory(context, userId)
    )
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()

    val messageState = produceState(initialValue = "Loading...") {
        value = try {
            NoAsAService.fetchMessage()
        } catch (e: Exception) {
            "Error: ${e.message ?: "unknown"}"
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.bayg.black)) {
        val current = messageState.value

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 54.dp, bottom = 110.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopUsageCard(navController, displayName, statsState)
            Spacer(modifier = Modifier.height(42.dp))

            InfoCardsRow()
            Spacer(modifier = Modifier.height(34.dp))

            Column (horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                if (current == "Loading...") {
                    CircularProgressIndicator(color = MaterialTheme.bayg.green)
                } else {
                    Text(
                        text = "“${current}”",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight(400),
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.bayg.green,
                            textAlign = TextAlign.Center,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(54.dp))

                GreenButton(
                    onClick = {
                        context.startActivity(Intent(context, TouchGrassActivity::class.java))
                    },
                    "🌿 Touch Grass", color = MaterialTheme.bayg.white
                )
            }
        }

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            NavBar(navController)
        }
    }
}

@Composable
private fun TopUsageCard(
    navController: NavController,
    displayName: String,
    statsState: StatsUiState,
    viewModel: StreakViewModel = viewModel()
) {
    val today = SimpleDateFormat("MMMM d", Locale.ENGLISH).format(Date())
    val day = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
    val streakCount = viewModel.streakCount
    val firstName = displayName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"

    val usageMinutes = (statsState as? StatsUiState.Success)
        ?.dailyUsage?.firstOrNull { it.isToday }?.minutes ?: 0
    val dailyLimitMinutes = (statsState as? StatsUiState.Success)?.dailyLimitMinutes ?: 45
    val isOver = usageMinutes > dailyLimitMinutes
    val overPercent = if (isOver && dailyLimitMinutes > 0)
        ((usageMinutes - dailyLimitMinutes) * 100 / dailyLimitMinutes) else 0

    Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.bayg.green)
                .padding(28.dp)
        ) {
            Column {
                Heading4("hey, $firstName 👋", MaterialTheme.bayg.black)
                Paragraph(text = "$day · $today", color = MaterialTheme.bayg.black)
                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    UsageCircle(usageMinutes, dailyLimitMinutes)  // ← fixed
                    Spacer(modifier = Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.End) {
                        Paragraph("Daily limit", color = MaterialTheme.bayg.black)
                        Heading3("$dailyLimitMinutes min", MaterialTheme.bayg.black)
                        if (isOver) {
                            Paragraph("⚠ ${overPercent}% over", color = MaterialTheme.bayg.lightRed)
                        } else {
                            Paragraph("✓ within limit", color = MaterialTheme.bayg.black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Paragraph("Current Streak", color = MaterialTheme.bayg.black)
                Heading4("$streakCount days", MaterialTheme.bayg.black)
            }
        }

        ProfileButton(Modifier.align(Alignment.TopEnd), navController, firstName)
        StreakDays(modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun ProfileButton(modifier: Modifier = Modifier, navController: NavController, firstName: String) {
    Box(
        modifier = modifier.size(width = 64.dp, height = 61.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp))
            .background(MaterialTheme.bayg.black)
            .padding(14.dp, 5.dp, 6.dp, 12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(MaterialTheme.bayg.green)
                .clickable{ navController.navigate("settings") },
            contentAlignment = Alignment.Center
        ) {
            Heading4(firstName.first().toString(), MaterialTheme.bayg.black)
        }
    }
}

@Composable
private fun StreakDays(modifier: Modifier = Modifier, viewModel: StreakViewModel = viewModel()) {
    val todayIdx = viewModel.todayIndex()

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.size(width = 235.dp, height = 50.dp)
            .clip(RoundedCornerShape(topEnd = 15.dp))
            .background(MaterialTheme.bayg.black)
            .padding(7.dp, 10.dp, 10.dp, 7.dp)
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, day ->
            if (index == todayIdx) {
                TodayDot(day)
            } else {
                DayDot(day, viewModel.activeStreakDays[index])
            }
        }
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
        hours > 0 -> "${hours}h"
        else -> "${minutes}min"
    }
}

@Composable
private fun UsageCircle(usage: Int, limit: Int) {
    val progress = (usage.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    val sweepAngle = 360f * progress

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val stroke = Stroke(
                width = 24.dp.toPx(),
                cap = StrokeCap.Butt
            )

            drawArc(
                color = Color.Black.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )

            drawArc(
                color = Color.Black,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Heading4(formatMinutes(usage), MaterialTheme.bayg.black)
            Text("today", fontSize = 21.sp, color = MaterialTheme.bayg.textGrey)
        }
    }
}

@Composable
private fun DayDot(text: String, active: Boolean) {
    Box(
        modifier = Modifier
            .size(27.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.bayg.green else MaterialTheme.bayg.darkGreen),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 1.sp,
            color = if (active) MaterialTheme.bayg.black else MaterialTheme.bayg.white
        )
    }
}

@Composable
private fun TodayDot(text: String) {
    Box(
        modifier = Modifier
            .size(27.dp)
            .background(MaterialTheme.bayg.black)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.bayg.green,
                shape = CircleShape
            )
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.bayg.darkGreen),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.bayg.green,
                lineHeight = 1.sp
            )
        }
    }
}

@Composable
private fun InfoCardsRow() {
    Row(
        modifier = Modifier.width(435.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NearestParkInfoCard()
        WeatherInfoCard()
    }
}

@Composable
private fun NearestParkInfoCard() {
    val context = LocalContext.current
    val parkViewModel: NearestParkViewModel = viewModel()
    val parkState by parkViewModel.parkState.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    if (!hasLocationPermission) {
        SmallInfoCard(
            number = "?",
            title = "Parks\nnear you",
            body = listOf("Tap to find parks nearby"),
            footer = "Location permission required",
            onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
        )
        return
    }

    LaunchedEffect(hasLocationPermission) {
        val coords = DeviceLocationProvider.getCurrentLocation(context)
            ?: return@LaunchedEffect
        parkViewModel.fetchNearestPark(coords.first, coords.second)
    }

    when (val state = parkState) {
        NearestParkUiState.Loading -> SmallInfoCard(
            number = "...",
            title = "Parks\nnear you",
            body = listOf("Looking for nearby parks"),
            footer = "Using your location",
        )
        is NearestParkUiState.Success -> SmallInfoCard(
            number = "${state.totalCount}",
            title = "Nearest\npark",
            body = listOf(state.park.name),
            footer = "📍 ${state.park.distanceLabel}",
        )
        is NearestParkUiState.Error -> SmallInfoCard(
            number = "!",
            title = "Parks\nnear you",
            body = listOf(state.message),
            footer = "Overpass API · OSM data",
        )
    }
}

@Composable
private fun WeatherInfoCard() {
    val context = LocalContext.current
    val weatherViewModel: WeatherViewModel = viewModel()
    val weatherState by weatherViewModel.weatherState.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val requestLocationPermissions = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect

        val coords = DeviceLocationProvider.getCurrentLocation(context) ?: run {
            weatherViewModel.fetchWeather(52.37, 4.89)
            return@LaunchedEffect
        }
        weatherViewModel.fetchWeather(coords.first, coords.second)
    }

    if (!hasLocationPermission) {
        SmallInfoCard(
            number = "?",
            title = "Enable\nlocation",
            body = listOf("Tap to fetch OpenWeather for your area"),
            footer = "Location permission required",
            onClick = requestLocationPermissions,
        )
        return
    }

    when (val state = weatherState) {
        WeatherUiState.Loading -> {
            SmallInfoCard(
                number = "...",
                title = "Loading\nweather",
                body = listOf("Fetching from OpenWeather"),
                footer = "Using your location",
            )
        }
        is WeatherUiState.Success -> {
            val temp = state.weather.main.temp.toInt()
            val description = state.weather.weather.firstOrNull()?.main ?: "Weather"
            val city = state.weather.name
            val country = state.weather.sys?.country?.takeIf { it.isNotBlank() }
            val locationLabel = if (country != null) "$city, $country" else city
            val humidity = state.weather.main.humidity
            SmallInfoCard(
                number = "$temp",
                title = description.replaceFirstChar { it.uppercase() },
                body = listOf("You should take a walk today"),
                footer = "📍 $locationLabel · ${humidity}% humidity · OpenWeather",
            )
        }
        is WeatherUiState.Error -> {
            SmallInfoCard(
                number = "!",
                title = "Weather\nunavailable",
                body = listOf(state.message),
                footer = "Check API key + network",
            )
        }
    }
}