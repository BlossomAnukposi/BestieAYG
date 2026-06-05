package com.bayg.screens

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bayg.ui.viewmodel.ProfileSettingsViewModel
import com.bayg.ui.viewmodel.SettingsUiState
import androidx.compose.foundation.layout.Spacer

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bayg
import com.bayg.R


@Composable
fun ProfileSettings(
    navController: NavController,
    userId: String, // Pass this from your navigation
    context: Context = LocalContext.current
) {
    val viewModel: ProfileSettingsViewModel = viewModel(
        factory = remember {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ProfileSettingsViewModel(context, userId) as T
                }
            }
        }
    )

    val settingsState = viewModel.settingsState.collectAsState().value

    var showDailyLimitDialog by remember { mutableStateOf(false) }
    var showBlockDurationDialog by remember { mutableStateOf(false) }
    var inputValue by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    when (settingsState) {
        is SettingsUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        is SettingsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Error: ${settingsState.message}", color = MaterialTheme.bayg.white)
            }
        }
        is SettingsUiState.Success -> {
            val settings = settingsState.settings

            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                // Profile card (keep existing code)
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.bayg.green),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "B",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.bayg.black
                                )
                            }

                            Column {
                                Text(
                                    text = "Blossom A.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.bayg.white
                                )
                                Text(
                                    text = "blossom@student.mihistenden.com",
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
                        value = "${settings.dailyLimitMinutes} min",
                        onClick = {
                            inputValue = settings.dailyLimitMinutes.toString()
                            showDailyLimitDialog = true
                        }
                    )
                    Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                    SettingItem(
                        label = "Block Duration",
                        value = "${settings.blockDurationMinutes} min",
                        onClick = {
                            inputValue = settings.blockDurationMinutes.toString()
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
                        checked = settings.touchGrassModeEnabled,
                        onCheckedChange = { viewModel.updateTouchGrassMode(it) }
                    )
                    Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                    SettingItemToggle(
                        label = "Location",
                        checked = settings.locationEnabled,
                        onCheckedChange = { viewModel.updateLocation(it) }
                    )
                    Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                    SettingItemToggle(
                        label = "Notifications",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.updateNotifications(it) }
                    )
                }

                // ===== ACCOUNT SECTION =====
                SectionHeader("Account")

                SectionCard {
                    AccountSecurityInfo()
                    Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                    SettingItem(
                        label = "Delete All Data",
                        value = null,
                        destructive = true,
                        onClick = { /* TODO */ }
                    )
                    Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
                    SettingItem(
                        label = "Log Out",
                        value = null,
                        destructive = true,
                        onClick = { /* TODO */ }
                    )
                }

                Spacer(modifier = Modifier.padding(bottom = 80.dp))
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
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                inputValue.toIntOrNull()?.let {
                                    viewModel.updateDailyLimit(it)
                                }
                                showDailyLimitDialog = false
                            }
                        ) {
                            Text("Save")
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
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                inputValue.toIntOrNull()?.let {
                                    viewModel.updateBlockDuration(it)
                                }
                                showBlockDurationDialog = false
                            }
                        ) {
                            Text("Save")
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
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