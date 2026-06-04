package com.bayg.screens

import android.content.Intent
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import com.bayg.TouchGrassActivity
import com.bayg.services.NoAsAService
import com.bayg.widgets.GreenButton
import com.bayg.widgets.Heading3
import com.bayg.widgets.Heading4
import com.bayg.widgets.Paragraph
import com.bayg.widgets.SmallInfoCard
import com.bayg.widgets.Subtitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Dashboard(navController: NavController) {
    val context = LocalContext.current
    val messageState = produceState(initialValue = "Loading...") {
        value = try {
            NoAsAService.fetchMessage()
        } catch (e: Exception) {
            "Error: ${e.message ?: "unknown"}"
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.bayg.black)) {
        val current = messageState.value
        if (current == "Loading...") {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp)
                    .padding(top = 54.dp, bottom = 110.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TopUsageCard(navController)
                Spacer(modifier = Modifier.height(42.dp))

                InfoCardsRow()
                Spacer(modifier = Modifier.height(34.dp))

                Column (horizontalAlignment = Alignment.CenterHorizontally) {
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
}

@Composable
private fun TopUsageCard(navController: NavController) {
    val today = SimpleDateFormat("MMMM d", Locale.ENGLISH)
        .format(Date())
    val day = SimpleDateFormat("EEEE", Locale.ENGLISH)
        .format(Date())
    val streakCount = 2
    val firstName = "Blossom"

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
                    UsageCircle()
                    Spacer(modifier = Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.End) {
                        Paragraph("Daily limit", color = MaterialTheme.bayg.black)
                        Heading3("45 min",MaterialTheme.bayg.black)
                        Paragraph("⚠ 197% over", color = MaterialTheme.bayg.lightRed)
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Paragraph("Streak", color = MaterialTheme.bayg.black)
                Heading4("$streakCount days",MaterialTheme.bayg.black)
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
private fun StreakDays(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.size(width = 235.dp, height = 50.dp)
            .clip(RoundedCornerShape(topEnd = 15.dp))
            .background(MaterialTheme.bayg.black)
            .padding(7.dp, 10.dp, 10.dp, 7.dp)
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, day ->
            DayDot(day, active = index == 0 || index == 2 || index == 3)
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
private fun UsageCircle() {
    val limit: Int = 45
    val usage: Int = 20

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
            Heading4("${formatMinutes(usage)}", MaterialTheme.bayg.black)
            Text(
                "today",
                fontSize = 21.sp,
                color = MaterialTheme.bayg.textGrey
            )
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
            color = if (active) MaterialTheme.bayg.black else MaterialTheme.bayg.white
        )
    }
}

@Composable
private fun InfoCardsRow() {
    Row(
        modifier = Modifier.width(435.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SmallInfoCard(
            number = "5",
            title = "Parks\nnear you",
            body = listOf("Parc Sandur", "Emmen Centrum Park"),
            footer = "and 3 more..."
        )

        SmallInfoCard(
            number = "18",
            title = "Partly\nCloudy",
            body = listOf("You should take a walk today"),
            footer = "High: 23°C | Low: 18°C"
        )
    }
}