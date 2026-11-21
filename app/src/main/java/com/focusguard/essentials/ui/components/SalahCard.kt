package com.focusguard.essentials.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.essentials.data.model.DailySalahRecord
import com.focusguard.essentials.data.model.SalahType
import com.focusguard.essentials.ui.theme.*

@Composable
fun SalahCard(
    record: DailySalahRecord,
    onToggleSalah: (SalahType) -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Header Row: Mosque Icon | Divider | "Salah" + "0 / 5" | Badge "Keep going 💚"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mosque hero illustration
                Icon(
                    painter = painterResource(id = R.drawable.ic_mosque_hero),
                    contentDescription = "Salah Mosque",
                    tint = DeepEmerald,
                    modifier = Modifier.size(40.dp)
                )

                // Divider line
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(1.dp)
                        .height(34.dp)
                        .background(WarmDivider)
                )

                // Title & Counter
                Column {
                    Text(
                        text = "Salah",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkForestText
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${record.completedCount}",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = DarkForestText
                        )
                        Text(
                            text = " / 5",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MutedSlateText,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Pill Badge
                val isAllDone = record.completedCount == 5
                val badgeText = if (isAllDone) "Alhamdulillah ✨" else "Keep going 💚"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SageGreenLight)
                        .border(1.dp, SageGreenBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = IslamicSansFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = DeepEmerald
                    )
                }
            }

            // 5 Prayer Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SalahType.values().forEach { type ->
                    val isDone = record.isCompleted(type)
                    PrayerItem(
                        name = type.displayName,
                        isDone = isDone,
                        onToggle = { onToggleSalah(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerItem(
    name: String,
    isDone: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = if (isDone) DeepEmerald.copy(alpha = 0.5f) else WarmCardBorder,
        animationSpec = tween(200),
        label = "prayerBorder"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(WarmIvorySubtle)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Miniature Dome Icon
        Icon(
            painter = painterResource(id = R.drawable.ic_prayer_dome),
            contentDescription = null,
            tint = if (isDone) DeepEmerald else SageGreen,
            modifier = Modifier.size(18.dp)
        )

        // Prayer Name
        Text(
            text = name,
            fontFamily = IslamicSansFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = DarkCharcoalText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Circular Checkbox
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isDone) DeepEmerald else Color.Transparent)
                .border(
                    width = if (isDone) 0.dp else 1.5.dp,
                    color = if (isDone) Color.Transparent else CheckboxUncheckedRing,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
