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
import androidx.compose.runtime.*
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
    var showIrreversibleConfirmationDialog by remember { mutableStateOf(false) }

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
                    if (status.isPolicyPermanentlyLocked) GuardGreen.copy(alpha = 0.8f)
                    else if (status.isDeviceOwner) AccentBlue.copy(alpha = 0.5f)
                    else GuardRed.copy(alpha = 0.5f)
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
                        imageVector = if (status.isPolicyPermanentlyLocked) Icons.Default.Lock
                        else if (status.isDeviceOwner) Icons.Default.Shield
                        else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (status.isPolicyPermanentlyLocked) GuardGreen
                        else if (status.isDeviceOwner) AccentBlue
                        else GuardRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (status.isPolicyPermanentlyLocked) "Permanent Restriction Active"
                            else if (status.isDeviceOwner) "Device Owner Provisioned"
                            else "Device Owner Required",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (status.isPolicyPermanentlyLocked) "Irreversible • Reversal only via Factory Reset"
                            else if (status.isDeviceOwner) "Ready to lock down device"
                            else "Not provisioned as Device Owner",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (status.isPolicyPermanentlyLocked) GuardGreenText else TextSecondary
                            )
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
                } else if (status.isPolicyPermanentlyLocked) {
                    Text(
                        text = "Device restrictions are permanently locked. USB debugging (ADB) is disabled. Focus Guard cannot be turned off, modified, or uninstalled. Only a full device factory reset can restore unrestricted access.",
                        style = MaterialTheme.typography.bodySmall.copy(color = GuardGreenText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GuardGreenContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }

                Button(
                    onClick = {
                        if (status.isDeviceOwner) {
                            showIrreversibleConfirmationDialog = true
                        }
                    },
                    enabled = !isEnforcing && status.isDeviceOwner,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status.isPolicyPermanentlyLocked) GuardGreenContainer else GuardGreen,
                        contentColor = if (status.isPolicyPermanentlyLocked) GuardGreenText else DarkBackground,
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
                        Icon(
                            imageVector = if (status.isPolicyPermanentlyLocked) Icons.Default.Lock else Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (status.isPolicyPermanentlyLocked) "Re-Assert Permanent Policies"
                            else if (status.isDeviceOwner) "Enforce Irreversible Lock Now"
                            else "Device Owner Required",
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
            icon = Icons.Default.Terminal,
            title = "ADB & USB Debugging Access",
            subtitle = if (status.isAdbDebuggingBlocked) "Permanently Disabled (DISALLOW_DEBUGGING_FEATURES)" else "Active (Will be blocked upon enforcement)",
            isActive = status.isAdbDebuggingBlocked
        )

        SubsystemStatusItem(
            icon = Icons.Default.Fingerprint,
            title = "Cryptographic App Baseline",
            subtitle = if (status.baselinePackageCount > 0) "${status.baselinePackageCount} pre-existing packages baseline-verified (SHA-256)" else "Baseline ready to record upon enforcement",
            isActive = status.baselinePackageCount > 0
        )

        SubsystemStatusItem(
            icon = Icons.Default.Shop,
            title = "Samsung Galaxy Store & Updates",
            subtitle = "UI Browsing Isolated • Samsung Background Service Updates Active",
            isActive = status.isPolicyPermanentlyLocked
        )

        SubsystemStatusItem(
            icon = Icons.Default.SystemUpdate,
            title = "Samsung OTA & Security Updates",
            subtitle = "Automatic OTA Updates Enforced (System services preserved)",
            isActive = status.isPolicyPermanentlyLocked
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

    if (showIrreversibleConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showIrreversibleConfirmationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GuardRed)
                    Text("Permanent Restriction Lock", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "WARNING: THIS ACTION CANNOT BE REVERSED FROM THIS PHONE OR VIA A COMPUTER.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GuardRedText
                        )
                    )
                    Text(
                        "Once you confirm:\n" +
                                "• USB Debugging (ADB) will be permanently shut off.\n" +
                                "• Focus Guard cannot be uninstalled or turned off.\n" +
                                "• Browsers, social media, and unapproved apps will be hidden.\n" +
                                "• Private DNS will be enforced to high.kahfguard.com.\n" +
                                "• Sideloading APKs will be blocked.\n\n" +
                                "The ONLY escape route to remove these restrictions is a full device Factory Reset.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIrreversibleConfirmationDialog = false
                        onEnforceClicked()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuardRed,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Lock & Enforce Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIrreversibleConfirmationDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
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
