package com.focusguard.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.essentials.data.model.DailyQazaRecord
import com.focusguard.essentials.data.model.SalahType
import com.focusguard.essentials.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QazaSalahCard(
    record: DailyQazaRecord,
    onIncrementQaza: (SalahType, Int) -> Unit,
    onSetQazaCount: (SalahType, Int) -> Unit,
    onOpenMonthlySummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPrayer by remember { mutableStateOf<SalahType?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WarmIvorySurface)
            .border(1.dp, WarmCardBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Sync Icon | Title + Subtitle | "Monthly" Pill Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_qaza_sync),
                    contentDescription = "Qaza Salah",
                    tint = DeepEmerald,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Qaza Salah",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkForestText
                    )
                    Text(
                        text = "Today's completed (${record.totalCompletedToday})",
                        fontFamily = IslamicSansFont,
                        fontSize = 11.sp,
                        color = MutedSlateText
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // "Monthly" pill button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(InputPillBackground)
                        .clickable { onOpenMonthlySummary() }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Monthly",
                        fontFamily = IslamicSansFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = DarkCharcoalText
                    )
                }
            }

            // 5 Columns: Fajr, Dhuhr, Asr, Maghrib, Isha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SalahType.values().forEach { type ->
                    val count = record.completedFor(type)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            fontFamily = IslamicSansFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MutedSlateText,
                            maxLines = 1
                        )

                        // Pill number box: tap to +1, long-press to type directly
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (count > 0) SageGreenLight else InputPillBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (count > 0) SageGreenBorder else WarmCardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .combinedClickable(
                                    onClick = { onIncrementQaza(type, 1) },
                                    onLongClick = { editingPrayer = type }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (count > 0) SageGreen else DarkForestText
                            )
                        }

                        // Stepper row: [-] [+]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(InputPillBackground)
                                    .border(1.dp, WarmCardBorder, RoundedCornerShape(4.dp))
                                    .clickable { onIncrementQaza(type, -1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkForestText)
                            }

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DeepEmerald)
                                    .clickable { onIncrementQaza(type, 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct Count Editor Dialog when long-pressing any prayer box
    editingPrayer?.let { prayer ->
        val currentVal = record.completedFor(prayer)
        var tempCount by remember(prayer) { mutableStateOf(currentVal.toString()) }

        AlertDialog(
            onDismissRequest = { editingPrayer = null },
            title = {
                Text(
                    text = "${prayer.displayName} Qaza Offered Today",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestText
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter exact completed count for ${prayer.displayName}:",
                        fontFamily = IslamicSansFont,
                        fontSize = 13.sp,
                        color = MutedSlateText
                    )

                    OutlinedTextField(
                        value = tempCount,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) tempCount = it },
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
                        modifier = Modifier.width(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempCount.toIntOrNull() ?: 0
                        onSetQazaCount(prayer, parsed)
                        editingPrayer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = WarmIvorySurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPrayer = null }) {
                    Text("Cancel", color = MutedSlateText)
                }
            },
            containerColor = WarmIvorySurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
