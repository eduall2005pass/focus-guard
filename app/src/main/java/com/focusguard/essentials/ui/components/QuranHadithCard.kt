package com.focusguard.essentials.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.essentials.data.model.QuranHadithRecord
import com.focusguard.essentials.ui.theme.*

@Composable
fun QuranHadithCard(
    record: QuranHadithRecord,
    onUpdateRecord: (QuranHadithRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // If there is already saved data for this date, default to Saved view; otherwise start in Edit view
    var isEditing by remember(record.date) {
        mutableStateOf(!record.hasData)
    }

    // Local state for the 6 fields, synced when date or editing state changes
    var qStudyPages by remember(record.date, isEditing) {
        mutableStateOf(if (record.quranStudyPages > 0) record.quranStudyPages.toString() else "")
    }
    var qStudyAyahs by remember(record.date, isEditing) {
        mutableStateOf(if (record.quranStudyAyahs > 0) record.quranStudyAyahs.toString() else "")
    }
    var qMemoPages by remember(record.date, isEditing) {
        mutableStateOf(if (record.quranMemoPages > 0) record.quranMemoPages.toString() else "")
    }
    var qMemoAyahs by remember(record.date, isEditing) {
        mutableStateOf(if (record.quranMemoAyahs > 0) record.quranMemoAyahs.toString() else "")
    }
    var hStudyQty by remember(record.date, isEditing) {
        mutableStateOf(if (record.hadithStudyCount > 0) record.hadithStudyCount.toString() else "")
    }
    var hMemoQty by remember(record.date, isEditing) {
        mutableStateOf(if (record.hadithMemoCount > 0) record.hadithMemoCount.toString() else "")
    }

    fun saveAll() {
        val updated = record.copy(
            quranStudyPages = qStudyPages.toIntOrNull() ?: 0,
            quranStudyAyahs = qStudyAyahs.toIntOrNull() ?: 0,
            quranMemoPages = qMemoPages.toIntOrNull() ?: 0,
            quranMemoAyahs = qMemoAyahs.toIntOrNull() ?: 0,
            hadithStudyCount = hStudyQty.toIntOrNull() ?: 0,
            hadithMemoCount = hMemoQty.toIntOrNull() ?: 0,
            updatedAt = System.currentTimeMillis()
        )
        onUpdateRecord(updated)
        focusManager.clearFocus()
        Toast.makeText(context, "✓ Quran & Hadith saved!", Toast.LENGTH_SHORT).show()
        isEditing = false
    }

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
            // Header Row: Quran Icon | Title "Quran & Hadith" | [ Save ] or [ Edit ] on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_quran_rehal),
                    contentDescription = "Quran & Hadith",
                    tint = DeepEmerald,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Quran & Hadith",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkForestText
                    )
                    Text(
                        text = if (isEditing) "Enter daily progress" else if (record.hasData) "Saved for today" else "No progress logged",
                        fontFamily = IslamicSansFont,
                        fontSize = 11.sp,
                        color = if (!isEditing && record.hasData) DeepEmerald else MutedSlateText
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Right action button: [ Save ] in edit mode, [ Edit ] in saved mode
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepEmerald)
                            .clickable { saveAll() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Save",
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SageGreenLight)
                            .border(1.dp, SageGreenBorder, RoundedCornerShape(12.dp))
                            .clickable { isEditing = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = DeepEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Edit",
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = DeepEmerald
                            )
                        }
                    }
                }
            }

            if (isEditing) {
                // EDIT MODE: 2-Column Grid of editable inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Column 1: Quran Study & Hadith Study
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quran Study
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Quran Study",
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = DarkCharcoalText
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumericPillField(
                                    value = qStudyPages,
                                    onValueChange = { qStudyPages = it },
                                    placeholder = "Pages",
                                    onDone = { saveAll() },
                                    modifier = Modifier.weight(1f)
                                )
                                NumericPillField(
                                    value = qStudyAyahs,
                                    onValueChange = { qStudyAyahs = it },
                                    placeholder = "Ayahs",
                                    onDone = { saveAll() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Hadith Study
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Hadith Study",
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = DarkCharcoalText
                            )
                            NumericPillField(
                                value = hStudyQty,
                                onValueChange = { hStudyQty = it },
                                placeholder = "Qty (Ahadith)",
                                onDone = { saveAll() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Column 2: Quran Memorization & Hadith Memorization
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quran Memorization
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Quran Memorization",
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = DarkCharcoalText
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumericPillField(
                                    value = qMemoPages,
                                    onValueChange = { qMemoPages = it },
                                    placeholder = "Pages",
                                    onDone = { saveAll() },
                                    modifier = Modifier.weight(1f)
                                )
                                NumericPillField(
                                    value = qMemoAyahs,
                                    onValueChange = { qMemoAyahs = it },
                                    placeholder = "Ayahs",
                                    onDone = { saveAll() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Hadith Memorization
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Hadith Memorization",
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = DarkCharcoalText
                            )
                            NumericPillField(
                                value = hMemoQty,
                                onValueChange = { hMemoQty = it },
                                placeholder = "Qty (Ahadith)",
                                onDone = { saveAll() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Explicit Bottom "Save Progress" Pill for immediate reach
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepEmerald)
                        .clickable { saveAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Save Quran & Hadith",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                // SAVED MODE: Clean, elegant summary tiles displaying saved numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Column 1
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SavedSummaryTile(
                            label = "Quran Study",
                            valueText = formatPagesAyahs(record.quranStudyPages, record.quranStudyAyahs)
                        )
                        SavedSummaryTile(
                            label = "Hadith Study",
                            valueText = formatCount(record.hadithStudyCount, "Hadith", "Ahadith")
                        )
                    }

                    // Column 2
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SavedSummaryTile(
                            label = "Quran Memorization",
                            valueText = formatPagesAyahs(record.quranMemoPages, record.quranMemoAyahs)
                        )
                        SavedSummaryTile(
                            label = "Hadith Memorization",
                            valueText = formatCount(record.hadithMemoCount, "Hadith", "Ahadith")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSummaryTile(
    label: String,
    valueText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarmIvorySubtle)
            .border(1.dp, WarmCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                fontFamily = IslamicSansFont,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = MutedSlateText
            )
            Text(
                text = valueText,
                fontFamily = IslamicSansFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (valueText == "—") MutedSlateText else DarkForestText
            )
        }
    }
}

private fun formatPagesAyahs(pages: Int, ayahs: Int): String {
    val parts = mutableListOf<String>()
    if (pages > 0) parts.add("$pages ${if (pages == 1) "Page" else "Pages"}")
    if (ayahs > 0) parts.add("$ayahs ${if (ayahs == 1) "Ayah" else "Ayahs"}")
    return if (parts.isEmpty()) "—" else parts.joinToString(" · ")
}

private fun formatCount(count: Int, singular: String, plural: String): String {
    return if (count <= 0) "—" else "$count ${if (count == 1) singular else plural}"
}

@Composable
fun NumericPillField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(InputPillBackground)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontFamily = IslamicSansFont,
                fontSize = 12.sp,
                color = InputPillPlaceholder
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { text ->
                if (text.all { it.isDigit() }) {
                    onValueChange(text)
                }
            },
            textStyle = TextStyle(
                fontFamily = IslamicSansFont,
                fontSize = 13.sp,
                color = DarkForestText,
                fontWeight = FontWeight.SemiBold
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            cursorBrush = SolidColor(DeepEmerald),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
