package com.bayg

import BAYGTheme
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.bayg.services.storage.AppDatabase
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.BlockEventSeverity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BlockedActivity
 *
 * Shown as a fullscreen takeover when the user opens Instagram
 * after exceeding their daily usage limit.
 *
 * The user cannot get back to Instagram from here — pressing Back
 * sends them to the device home screen instead.
 *
 * They can optionally open TouchGrassActivity to find a nearby park.
 */
class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val usedMs = UsageTracker.getTodayUsageMs(this)
        val limitMs = UsageTracker.getDailyLimitMs(this)
        val usedFormatted = UsageTracker.formatDuration(usedMs)
        val limitFormatted = UsageTracker.formatDuration(limitMs)
        recordBlockEvent(usedMs, limitMs)

        setContent {
            BAYGTheme {
                BlockedScreen(
                    usedFormatted = usedFormatted,
                    limitFormatted = limitFormatted,
                    onGoTouchGrass = { openTouchGrass() },
                    onGoHome = { goHome() }
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })
    }

    private fun recordBlockEvent(usedMs: Long, limitMs: Long) {
        lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                val db = AppDatabase.getInstance(this@BlockedActivity)
                val user = db.userDao().getByFirebaseUid(uid) ?: return@launch
                val settings = db.userSettingsDao().getByUserId(user.id)
                val blockDuration = settings?.blockDurationMinutes ?: 30
                val event = BlockEvent(
                    userId = uid,
                    triggeredAt = System.currentTimeMillis(),
                    blockDurationMinutes = blockDuration,
                    label = "Daily limit exceeded",
                    severity = BlockEventSeverity.RED,
                    detail = "Used ${UsageTracker.formatDuration(usedMs)} of ${UsageTracker.formatDuration(limitMs)}",
                    firebaseId = null,
                    syncedAt = null,
                )
                withContext(Dispatchers.IO) { db.blockEventDao().insert(event) }
                Log.i("BlockedActivity", "blockEvent inserted uid=$uid dur=${blockDuration}m")
            } catch (e: Exception) {
                Log.e("BlockedActivity", "blockEvent insert failed", e)
            }
        }
    }
    
    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun openTouchGrass() {
        startActivity(Intent(this, TouchGrassActivity::class.java))
        finish()
    }
}

@Composable
private fun BlockedScreen(
    usedFormatted: String,
    limitFormatted: String,
    onGoTouchGrass: () -> Unit,
    onGoHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B4332),
                        Color(0xFF2D6A4F),
                        Color(0xFF40916C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Text(
                text = "🌿",
                fontSize = 80.sp
            )

            Text(
                text = "Put the phone down.",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "You've spent $usedFormatted on Instagram today.\nYour limit is $limitFormatted.\n\nThere's a whole world out there.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onGoTouchGrass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1B4332)
                )
            ) {
                Text(
                    text = "🌳  Find a nearby park",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, Color.White.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "Go home",
                    fontSize = 16.sp
                )
            }
        }
    }
}
