package com.bayg.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
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
import androidx.compose.material3.MaterialTheme

@Composable
fun ProfileSettings(navController: NavController) {
    var touchGrassMode by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

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

        // Profile card unchanged (single card)
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

        // ===== LIMITS: single card with rows and dividers =====
        SectionHeader("Limits")

        SectionCard {
            SettingItem(
                label = "Daily Limit",
                value = "45 min",
                onClick = { /* TODO */ }
            )
            Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
            SettingItem(
                label = "Block Duration",
                value = "30 min",
                onClick = { /* TODO */ }
            )
            Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
            SettingItem(
                label = "Pre-Event Limit (3 days)",
                value = "20 min/day",
                onClick = { /* TODO */ }
            )
        }

        // ===== BEHAVIOUR: single card with rows and dividers =====
        SectionHeader("Behaviour")

        SectionCard {
            SettingItemToggle(
                label = "Touch Grass Mode",
                checked = touchGrassMode,
                onCheckedChange = { touchGrassMode = it }
            )
            Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
            SettingItemToggle(
                label = "Location",
                checked = locationEnabled,
                onCheckedChange = { locationEnabled = it }
            )
            Divider(color = MaterialTheme.bayg.outline, thickness = 1.dp)
            SettingItemToggle(
                label = "Notifications",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
        }

        // ===== ACCOUNT: single card with rows and dividers =====
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

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 80.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.bayg.textGrey,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
    )
}

/** A card wrapper that applies the darker outline and internal padding, used per-section */
@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
        border = BorderStroke(1.dp, MaterialTheme.bayg.outline),
        shape = RoundedCornerShape(8.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // invoke the receiver lambda inside Column's scope
            content()
        }
    }
}

/** A single clickable setting row with optional value and right arrow */
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
            fontSize = 16.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(text = value, color = MaterialTheme.bayg.textGrey, fontSize = 13.sp)
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "Navigate",
                tint = if (destructive) MaterialTheme.bayg.lightRed else MaterialTheme.bayg.textGrey,
                modifier = Modifier.size(18.dp).padding(start = if (value != null) 8.dp else 0.dp)
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.bayg.white, fontSize = 16.sp)
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