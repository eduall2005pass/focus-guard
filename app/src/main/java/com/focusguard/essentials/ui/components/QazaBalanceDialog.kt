package com.focusguard.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.essentials.data.model.MonthlyQazaSummary
import com.focusguard.essentials.data.model.QazaSettings
import com.focusguard.essentials.data.model.SalahType
import com.focusguard.essentials.ui.theme.*

@Composable
fun QazaBalanceDialog(
    settings: QazaSettings,
    monthlySummary: MonthlyQazaSummary?,
    onSaveSettings: (QazaSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var fajrInput by remember(settings.fajrInitial) { mutableStateOf(settings.fajrInitial.toString()) }
    var dhuhrInput by remember(settings.dhuhrInitial) { mutableStateOf(settings.dhuhrInitial.toString()) }
    var asrInput by remember(settings.asrInitial) { mutableStateOf(settings.asrInitial.toString()) }
    var maghribInput by remember(settings.maghribInitial) { mutableStateOf(settings.maghribInitial.toString()) }
    var ishaInput by remember(settings.ishaInitial) { mutableStateOf(settings.ishaInitial.toString()) }

    var isEditMode by remember { mutableStateOf(!settings.isConfigured) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Qaza Salah Overview",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkForestText
                )
                Text(
                    text = if (isEditMode) "Set initial outstanding balance" else "Monthly carry-forward balance",
                    fontFamily = IslamicSansFont,
                    fontSize = 12.sp,
                    color = MutedSlateText
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!settings.isConfigured) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MutedGoldLight)
                            .border(1.dp, MutedGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Starting balance not yet set. Enter the initial number of missed prayers to calculate your carry-forward correctly.",
                            fontFamily = IslamicSansFont,
                            fontSize = 12.sp,
                            color = DarkCharcoalText
                        )
                    }
                }

                if (isEditMode) {
                    Text(
                        text = "Initial Missed Prayers Balance:",
                        fontFamily = IslamicSansFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = DarkForestText
                    )

                    PrayerBalanceInputField(
                        label = "Fajr",
                        value = fajrInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) fajrInput = it }
                    )
                    PrayerBalanceInputField(
                        label = "Dhuhr",
                        value = dhuhrInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) dhuhrInput = it }
                    )
                    PrayerBalanceInputField(
                        label = "Asr",
                        value = asrInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) asrInput = it }
                    )
                    PrayerBalanceInputField(
                        label = "Maghrib",
                        value = maghribInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) maghribInput = it }
                    )
                    PrayerBalanceInputField(
                        label = "Isha",
                        value = ishaInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) ishaInput = it }
                    )
                } else if (monthlySummary != null) {
                    // Carry Forward Summary Table
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WarmIvorySubtle)
                            .border(1.dp, WarmCardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Prayer", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedSlateText)
                                Text("Prior", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedSlateText)
                                Text("Done", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedSlateText)
                                Text("Remaining", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepEmerald)
                            }
                            HorizontalDivider(color = WarmDivider)

                            monthlySummary.prayers.forEach { (type, stat) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(type.displayName, fontFamily = IslamicSansFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = DarkForestText)
                                    Text("${stat.priorOutstanding}", fontFamily = IslamicSansFont, fontSize = 13.sp, color = DarkCharcoalText)
                                    Text("-${stat.completedThisMonth}", fontFamily = IslamicSansFont, fontSize = 13.sp, color = AccentGreenHeart)
                                    Text("${stat.remaining}", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepEmerald)
                                }
                            }

                            HorizontalDivider(color = WarmDivider)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkForestText)
                                Text("", fontSize = 13.sp)
                                Text("-${monthlySummary.totalCompletedThisMonth}", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentGreenHeart)
                                Text("${monthlySummary.totalRemaining}", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepEmerald)
                            }
                        }
                    }

                    TextButton(
                        onClick = { isEditMode = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Edit Starting Balance", color = DeepEmerald, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (isEditMode) {
                Button(
                    onClick = {
                        val newSettings = settings.copy(
                            isConfigured = true,
                            fajrInitial = fajrInput.toIntOrNull() ?: 0,
                            dhuhrInitial = dhuhrInput.toIntOrNull() ?: 0,
                            asrInitial = asrInput.toIntOrNull() ?: 0,
                            maghribInitial = maghribInput.toIntOrNull() ?: 0,
                            ishaInitial = ishaInput.toIntOrNull() ?: 0,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSaveSettings(newSettings)
                        isEditMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Starting Balance", color = WarmIvorySurface)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = WarmIvorySurface)
                }
            }
        },
        dismissButton = {
            if (isEditMode && settings.isConfigured) {
                TextButton(onClick = { isEditMode = false }) {
                    Text("Cancel", color = MutedSlateText)
                }
            }
        },
        containerColor = WarmIvorySurface,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun PrayerBalanceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = IslamicSansFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = DarkCharcoalText
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DarkForestText,
                unfocusedTextColor = DarkForestText,
                focusedContainerColor = InputPillBackground,
                unfocusedContainerColor = InputPillBackground,
                focusedBorderColor = DeepEmerald,
                unfocusedBorderColor = WarmCardBorder,
                cursorColor = DeepEmerald
            ),
            modifier = Modifier.width(110.dp)
        )
    }
}
