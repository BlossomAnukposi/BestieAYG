package com.bayg.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bayg.services.storage.UserSettingsViewModel
import com.bayg.services.storage.Authenticator
import com.bayg.services.storage.entities.UserSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bayg
import com.bayg.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import com.bayg.widgets.NavBar

@Composable
fun ProfileSettings(
    navController: NavController
) {
    val viewModel: UserSettingsViewModel = viewModel()
    val settings = viewModel.settings
    val isSaving = viewModel.isSaving
    val displayName = viewModel.displayName
    val email = viewModel.email

    var editedSettings by remember(settings) {
        mutableStateOf(settings?.copy() ?: null)
    }

    var showDailyLimitDialog by remember { mutableStateOf(false) }
    var showBlockDurationDialog by remember { mutableStateOf(false) }
    var inputValue by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    if (settings == null || editedSettings == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.bayg.black)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.bayg.white,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Profile card
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
                border = BorderStroke(1.dp, MaterialTheme.bayg.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.bayg.green),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.bayg.black
                            )
                        }

                        Column {
                            Text(
                                text = displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.bayg.white
                            )
                            Text(
                                text = email,
                                fontSize = 12.sp,
                                color = MaterialTheme.bayg.textGrey
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_edit),
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.bayg.white,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { /* TODO */ }
                    )
                }
            }

            // ===== LIMITS SECTION =====
            SectionHeader("Limits")

            SectionCard {
                SettingItem(
                    label = "Daily Limit",
                    value = "${editedSettings!!.dailyLimitMinutes} min",
                    onClick = {
                        inputValue = editedSettings!!.dailyLimitMinutes.toString()
                        showDailyLimitDialog = true
                    }
                )
                Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                SettingItem(
                    label = "Block Duration",
                    value = "${editedSettings!!.blockDurationMinutes} min",
                    onClick = {
                        inputValue = editedSettings!!.blockDurationMinutes.toString()
                        showBlockDurationDialog = true
                    }
                )
                Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
            }

            // ===== BEHAVIOUR SECTION =====
            SectionHeader("Behaviour")

            SectionCard {
                SettingItemToggle(
                    label = "Touch Grass Mode",
                    checked = editedSettings!!.touchGrassModeEnabled,
                    onCheckedChange = {
                        editedSettings = editedSettings!!.copy(touchGrassModeEnabled = it)
                    }
                )
                Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                SettingItemToggle(
                    label = "Location",
                    checked = editedSettings!!.locationEnabled,
                    onCheckedChange = {
                        editedSettings = editedSettings!!.copy(locationEnabled = it)
                    }
                )
                Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                SettingItemToggle(
                    label = "Notifications",
                    checked = editedSettings!!.notificationsEnabled,
                    onCheckedChange = {
                        editedSettings = editedSettings!!.copy(notificationsEnabled = it)
                    }
                )
            }

            // ===== ACCOUNT SECTION =====
            SectionHeader("Account")

            SectionCard {
                AccountSecurityInfo()
                Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                SettingItem(
                    label = "Reset to Defaults",
                    value = null,
                    destructive = true,
                    onClick = {
                        // Reset settings to defaults
                        editedSettings = editedSettings!!.copy(
                            dailyLimitMinutes = 45,
                            blockDurationMinutes = 30
                        )
                    }
                )
                Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                SettingItem(
                    label = "Log Out",
                    value = null,
                    destructive = true,
                    onClick = {
                        // Sign out user
                        Authenticator().signOut()
                        // Navigate back to signup
                        navController.navigate("signUp") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.padding(bottom = 24.dp))

            // ===== SAVE / DISCARD BUTTONS =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Revert to saved settings
                        editedSettings = settings.copy()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    enabled = !isSaving && editedSettings != settings,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.bayg.outline,
                        contentColor = MaterialTheme.bayg.white
                    )
                ) {
                    Text("Discard")
                }

                Button(
                    onClick = {
                        // Save all changes
                        viewModel.saveLimits(
                            dailyLimitMinutes = editedSettings!!.dailyLimitMinutes,
                            blockDurationMinutes = editedSettings!!.blockDurationMinutes
                        )
                        viewModel.updateToggle(
                            touchGrass = editedSettings!!.touchGrassModeEnabled,
                            location = editedSettings!!.locationEnabled,
                            notifications = editedSettings!!.notificationsEnabled
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    enabled = !isSaving && editedSettings != settings,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.bayg.green,
                        contentColor = MaterialTheme.bayg.black
                    )
                ) {
                    Text("Save")
                }
            }
        }

        NavBar(navController)
    }

    // Daily Limit Dialog
    if (showDailyLimitDialog) {
        AlertDialog(
            onDismissRequest = { showDailyLimitDialog = false },
            title = { Text("Set Daily Limit (minutes)") },
            text = {
                TextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        inputValue.toIntOrNull()?.let { newDaily ->
                            editedSettings = editedSettings!!.copy(dailyLimitMinutes = newDaily)
                        }
                        showDailyLimitDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { showDailyLimitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block Duration Dialog
    if (showBlockDurationDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDurationDialog = false },
            title = { Text("Set Block Duration (minutes)") },
            text = {
                TextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        inputValue.toIntOrNull()?.let { newBlock ->
                            editedSettings = editedSettings!!.copy(blockDurationMinutes = newBlock)
                        }
                        showBlockDurationDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { showBlockDurationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.bayg.textGrey,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
        border = BorderStroke(1.dp, MaterialTheme.bayg.outline),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: String?,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (destructive) MaterialTheme.bayg.lightRed else MaterialTheme.bayg.white,
            fontSize = 14.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(text = value, color = MaterialTheme.bayg.textGrey, fontSize = 13.sp)
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "Navigate",
                tint = if (destructive) MaterialTheme.bayg.lightRed else MaterialTheme.bayg.textGrey,
                modifier = Modifier
                    .size(18.dp)
                    .padding(start = if (value != null) 8.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun SettingItemToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.bayg.white, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.bayg.black,
                checkedTrackColor = MaterialTheme.bayg.green,
                uncheckedThumbColor = MaterialTheme.bayg.textGrey,
                uncheckedTrackColor = MaterialTheme.bayg.outline
            )
        )
    }
}

@Composable
private fun AccountSecurityInfo() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(id = R.drawable.security),
            contentDescription = "Lock icon",
            tint = MaterialTheme.bayg.green,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "All data encrypted on-device. Crashout never sees your raw data. We take security VERY seriously.",
            fontSize = 12.sp,
            color = MaterialTheme.bayg.textGrey,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}