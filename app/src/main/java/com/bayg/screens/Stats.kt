package com.bayg.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import bayg
import com.bayg.ui.viewmodel.BlockEventUi
import com.bayg.ui.viewmodel.DayUsage
import com.bayg.ui.viewmodel.StatsPeriod
import com.bayg.ui.viewmodel.StatsUiState
import com.bayg.ui.viewmodel.StatsViewModel
import com.bayg.widgets.Heading3
import com.bayg.widgets.Heading4
import com.bayg.widgets.NavBar
import com.bayg.widgets.Paragraph
import com.bayg.services.storage.entities.BlockEventSeverity

/**
 * Factory for StatsViewModel — mirrors how ProfileSettingsViewModel is
 * expected to be constructed (context + firebase userId).
 *
 * TODO: replace "currentUserId" with however the app resolves the
 * logged-in user's Firebase UID elsewhere (e.g. FirebaseAuth.getInstance().uid).
 */
class StatsViewModelFactory(
    private val context: Context,
    private val userId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return StatsViewModel(context, userId) as T
    }
}

@Composable
fun Stats(navController: NavController) {
    val context = LocalContext.current
    val userId = remember { currentUserId(context) }

    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModelFactory(context, userId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEK) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.bayg.black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 54.dp, bottom = 110.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "stats",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.bayg.white,
            )
            Spacer(modifier = Modifier.height(24.dp))

            PeriodTabs(
                selected = selectedPeriod,
                onSelect = {
                    selectedPeriod = it
                    viewModel.loadStats(it)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                StatsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.bayg.green)
                    }
                }
                is StatsUiState.Error -> {
                    Paragraph(text = "Couldn't load stats: ${state.message}", color = MaterialTheme.bayg.lightRed)
                }
                is StatsUiState.Success -> {
                    SummaryCard(state)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.dailyUsage.isNotEmpty()) {
                        DailyUsageChart(state.dailyUsage, state.dailyLimitMinutes)
                        Spacer(modifier = Modifier.height(34.dp))
                    }

                    Text(
                        text = "Block Events",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.bayg.white,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.blockEvents.isEmpty()) {
                        Paragraph(
                            text = "No block events in this period.",
                            color = MaterialTheme.bayg.textGrey
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.blockEvents.forEach { event ->
                                BlockEventRow(event)
                            }
                        }
                    }
                }
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

/**
 * Resolves the currently logged-in user's Firebase UID.
 * Placeholder — wire up to your actual auth source.
 */
private fun currentUserId(context: Context): String {
    return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
}

// ── Period tabs ─────────────────────────────────────────────────────────

@Composable
private fun PeriodTabs(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.bayg.card)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TabButton("Week", StatsPeriod.WEEK, selected, onSelect, Modifier.weight(1f))
        TabButton("Month", StatsPeriod.MONTH, selected, onSelect, Modifier.weight(1f))
        TabButton("All Time", StatsPeriod.ALL_TIME, selected, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun TabButton(
    label: String,
    period: StatsPeriod,
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selected == period
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.bayg.green else Color.Transparent)
            .clickable { onSelect(period) }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.bayg.black else MaterialTheme.bayg.textGrey,
        )
    }
}

// ── Summary card ────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(state: StatsUiState.Success) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.bayg.card)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryItem("Total", state.summary.totalLabel, MaterialTheme.bayg.white)
        SummaryItem("Daily Avg", state.summary.dailyAvgLabel, MaterialTheme.bayg.white)
        SummaryItem("Blocks", state.summary.blockCountLabel, MaterialTheme.bayg.lightRed)
    }
}

@Composable
private fun SummaryItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.bayg.textGrey)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── Bar chart ───────────────────────────────────────────────────────────

@Composable
private fun DailyUsageChart(days: List<DayUsage>, dailyLimitMinutes: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.bayg.card)
            .padding(20.dp)
    ) {
        Text(
            text = "Daily Usage vs Limit",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.bayg.white,
        )
        Spacer(modifier = Modifier.height(20.dp))

        val maxValue = maxOf(dailyLimitMinutes, days.maxOfOrNull { it.minutes } ?: 0, 1)
        val chartHeight = 180.dp
        val barColorNormal = MaterialTheme.bayg.outline
        val barColorOver = MaterialTheme.bayg.lightRed
        val limitLineColor = MaterialTheme.bayg.lightRed

        Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            // Limit line + label, drawn behind the bars
            Canvas(modifier = Modifier.fillMaxSize()) {
                val limitY = size.height * (1f - dailyLimitMinutes.toFloat() / maxValue.toFloat())
                drawLine(
                    color = limitLineColor,
                    start = Offset(0f, limitY),
                    end = Offset(size.width, limitY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day ->
                    val isOverLimit = day.minutes > dailyLimitMinutes
                    val barFraction = (day.minutes.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                    val barHeight = chartHeight * barFraction
                    val minHeight = 28.dp

                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(maxOf(barHeight, minHeight))
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isOverLimit) barColorOver else barColorNormal)
                    )
                }
            }

            // "45m" style label for the limit line
            Text(
                text = "${dailyLimitMinutes}m",
                fontSize = 13.sp,
                color = limitLineColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = (-8).dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                Text(
                    text = day.label,
                    fontSize = 13.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.isToday) MaterialTheme.bayg.green else MaterialTheme.bayg.textGrey,
                    modifier = Modifier.width(28.dp)
                )
            }
        }
    }
}

// ── Block events list ──────────────────────────────────────────────────

@Composable
private fun BlockEventRow(event: BlockEventUi) {
    val dotColor = when (event.severity) {
        BlockEventSeverity.RED -> MaterialTheme.bayg.lightRed
        BlockEventSeverity.ORANGE -> MaterialTheme.bayg.lightOrange
    }
    val valueColor = dotColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.bayg.card)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${event.dateLabel} · ${event.timeLabel}",
                fontSize = 13.sp,
                color = MaterialTheme.bayg.textGrey,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.title,
                fontSize = 16.sp,
                color = MaterialTheme.bayg.white,
            )
        }

        Text(
            text = event.durationLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}
