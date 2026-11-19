package com.focusguard.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.ui.theme.*

@Composable
fun AdminProvisioningScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val adbCommand = "adb shell dpm set-device-owner com.focusguard/.receiver.FocusGuardDeviceAdminReceiver"

    val qrJsonPayload = """
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.focusguard/.receiver.FocusGuardDeviceAdminReceiver",
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true
}
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: ADB Setup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = AccentBlue)
                    Text(
                        text = "Device Owner Provisioning (ADB)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = "To grant persistent Device Owner privileges without rooting, run this command from your computer:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = adbCommand,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = GuardGreenText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
                                Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy command", tint = TextSecondary)
                        }
                    }
                }

                Text(
                    text = "Prerequisites:\n" +
                            "1. Remove all Google, WhatsApp, and OEM accounts from Settings > Accounts before running the command (or run on a newly factory-reset phone).\n" +
                            "2. Enable Developer Options > USB Debugging.\n" +
                            "3. Execute the command above.\n" +
                            "4. Return to Focus Guard and tap 'Enforce All Policies Now'.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }
        }

        // Section: Factory Reset QR Provisioning
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = GuardGreenText)
                    Text(
                        text = "Factory Reset QR Provisioning",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = "On the first screen of a factory-reset Android device, tap the screen 6 times to open the camera, then scan a QR code containing this JSON configuration:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Text(
                        text = qrJsonPayload,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    )
                }
            }
        }

        // Section: System Safety & Transparency
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = GuardAmberText)
                    Text(
                        text = "Security Architecture & Escape Route",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                DisclosureItem(
                    title = "Irreversible Once Enforced & ADB Disabled",
                    description = "When policies are enforced, USB debugging (ADB) is immediately disabled via DISALLOW_DEBUGGING_FEATURES. Focus Guard intentionally provides zero reverse or deactivation controls. The only way to reverse restrictions is a full device Factory Reset."
                )

                DisclosureItem(
                    title = "Factory Reset Is The Only Escape Route",
                    description = "Focus Guard never attempts to brick recovery, lock the bootloader, or block factory reset. Hardware recovery factory reset remains accessible as the sole intentional escape route."
                )

                DisclosureItem(
                    title = "System & Play Updates Preserved",
                    description = "Google Play Store (com.android.vending) and System Update services remain enabled so your approved apps and OS receive continuous security updates."
                )

                DisclosureItem(
                    title = "In-App WebViews Preserved",
                    description = "While standalone browsers (Chrome, Firefox, etc.) are blocked, the core Android System WebView (com.google.android.webview) is protected to ensure banking and login flows never break."
                )

                DisclosureItem(
                    title = "WhatsApp Communities Limitation",
                    description = "Android Device Owner APIs operate at the package and device level and cannot directly inspect intra-app UI. Focus Guard uses a scoped Accessibility Service (active only inside WhatsApp) to detect and exit the Communities tab."
                )
            }
        }
    }
}

@Composable
private fun DisclosureItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 12.sp
            )
        )
    }
}
