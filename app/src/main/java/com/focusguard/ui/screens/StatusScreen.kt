package com.focusguard.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.data.model.DeviceOwnerStatus
import com.focusguard.data.model.PolicyEnforcementResult
import com.focusguard.ui.theme.*

@Composable
fun StatusScreen(
    status: DeviceOwnerStatus,
    isEnforcing: Boolean,
    lastResult: PolicyEnforcementResult?,
    onEnforceClicked: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Protection Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (status.isDeviceOwner) DarkSurface else Color(0xFF1E1719)
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (status.isDeviceOwner) GuardGreen.copy(alpha = 0.5f) else GuardRed.copy(alpha = 0.5f)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (status.isDeviceOwner) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (status.isDeviceOwner) GuardGreen else GuardRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (status.isDeviceOwner) "Device Protection Active" else "Device Owner Required",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (status.isDeviceOwner) "Hardware-level restriction enforcement active" else "Not provisioned as Device Owner",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }

                if (!status.isDeviceOwner) {
                    Text(
                        text = "Focus Guard requires Device Owner provisioning via ADB or QR provisioning to enforce app allowlists and web restrictions. Visit the Admin tab for instructions.",
                        style = MaterialTheme.typography.bodySmall.copy(color = GuardAmberText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GuardAmberContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }

                Button(
                    onClick = onEnforceClicked,
                    enabled = !isEnforcing && status.isDeviceOwner,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuardGreen,
                        contentColor = DarkBackground,
                        disabledContainerColor = DarkSurfaceElevated,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isEnforcing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = DarkBackground,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enforcing Restrictions...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (status.isDeviceOwner) "Enforce All Policies Now" else "Device Owner Required to Enforce",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Last Enforcement Result Card
        if (lastResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Last Enforcement Execution",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Scanned", value = "${lastResult.totalPackagesScanned}")
                        StatItem(label = "Hidden", value = "${lastResult.packagesHidden}")
                        StatItem(label = "Protected", value = "${lastResult.packagesProtected}")
                    }

                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Private DNS: ${lastResult.privateDnsStatus}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )

                    if (lastResult.errors.isNotEmpty()) {
                        Text(
                            text = "Errors (${lastResult.errors.size}):",
                            style = MaterialTheme.typography.labelSmall.copy(color = GuardRedText)
                        )
                        lastResult.errors.take(3).forEach { err ->
                            Text(
                                text = "• $err",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GuardRedText,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Subsystems Status Section
        Text(
            text = "Protection Subsystems",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        )

        SubsystemStatusItem(
            icon = Icons.Default.AdminPanelSettings,
            title = "Device Policy Controller (DPC)",
            subtitle = if (status.isDeviceOwner) "Fully Managed Device Owner" else "Inactive (Requires ADB)",
            isActive = status.isDeviceOwner
        )

        SubsystemStatusItem(
            icon = Icons.Default.Dns,
            title = "Global Private DNS (DoT)",
            subtitle = if (status.privateDnsHost != null) "Locked to ${status.privateDnsHost}" else "high.kahfguard.com",
            isActive = status.isPrivateDnsLocked || status.privateDnsHost != null
        )

        SubsystemStatusItem(
            icon = Icons.Default.VpnKeyOff,
            title = "VPN & Proxy Restrictions",
            subtitle = if (status.isVpnRestricted) "VPN bypass blocked (DISALLOW_CONFIG_VPN)" else "Configurable",
            isActive = status.isVpnRestricted
        )

        SubsystemStatusItem(
            icon = Icons.Default.Lock,
            title = "App Tamper Protection",
            subtitle = if (status.isUninstallBlocked) "Focus Guard uninstall locked" else "Unlocked",
            isActive = status.isUninstallBlocked
        )

        SubsystemStatusItem(
            icon = Icons.Default.InstallMobile,
            title = "Sideloading Block",
            subtitle = if (status.isUnknownSourcesBlocked) "Unknown APK install disallowed" else "Allowed",
            isActive = status.isUnknownSourcesBlocked
        )

        SubsystemStatusItem(
            icon = Icons.Default.RestartAlt,
            title = "Safe Boot Restriction",
            subtitle = if (status.isSafeBootDisallowed) "Safe Mode bypass blocked" else "Allowed",
            isActive = status.isSafeBootDisallowed
        )

        SubsystemStatusItem(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "WhatsApp Communities Guard",
            subtitle = "Scoped Accessibility Service for community filtering",
            isActive = isAccessibilityServiceEnabled(context),
            actionText = if (!isAccessibilityServiceEnabled(context)) "Enable in Settings" else null,
            onAction = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = GuardGreenText
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
        )
    }
}

@Composable
private fun SubsystemStatusItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) GuardGreenContainer.copy(alpha = 0.5f) else DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) GuardGreenText else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    fontSize = 12.sp
                )
            }

            if (actionText != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(text = actionText, style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isActive) GuardGreen else TextMuted)
                )
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains("com.focusguard/.accessibility.WhatsAppCommunityAccessibilityService")
}
