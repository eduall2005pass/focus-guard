package com.focusguard.essentials.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.essentials.ui.theme.*
import com.focusguard.study.filter.AddFilterResult
import com.focusguard.study.filter.CustomBlockedFilterRepository

@Composable
fun ExplicitFilterCard(
    modifier: Modifier = Modifier,
    customFilterRepository: CustomBlockedFilterRepository? = null
) {
    val context = LocalContext.current
    val actualRepo = customFilterRepository ?: remember { CustomBlockedFilterRepository(context) }
    var blockedKeywordCount by remember { mutableIntStateOf(actualRepo.getTotalKeywordCount()) }
    var blockedDomainCount by remember { mutableIntStateOf(actualRepo.getTotalDomainCount()) }
    var customKeywordCount by remember { mutableIntStateOf(actualRepo.getCustomKeywordCount()) }
    var customDomainCount by remember { mutableIntStateOf(actualRepo.getCustomDomainCount()) }

    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmIvorySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(WarmCardBorder)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF381522), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Explicit Filter & Blocklist",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkForestText
                    )
                    Text(
                        text = "গোপনীয়তার স্বার্থে ফিল্টারকৃত শব্দের তালিকা সম্পূর্ণ লুকায়িত রাখা হয়েছে",
                        fontFamily = IslamicSansFont,
                        fontSize = 11.sp,
                        color = MutedSlateText
                    )
                }
            }

            // Metric Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Blocked Domains Metric
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmIvorySubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🚫 ব্লকড ডোমেইন",
                            fontFamily = IslamicSansFont,
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlateText)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$blockedDomainCount টি",
                            fontFamily = IslamicSansFont,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                        )
                        if (customDomainCount > 0) {
                            Text(
                                text = "কাস্টম: $customDomainCount টি",
                                fontFamily = IslamicSansFont,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = SubtleGrayText)
                            )
                        }
                    }
                }

                // Blocked Keywords Metric
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmIvorySubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🛑 ব্লকড কি-ওয়ার্ড",
                            fontFamily = IslamicSansFont,
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSlateText)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$blockedKeywordCount টি",
                            fontFamily = IslamicSansFont,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80)
                            )
                        )
                        if (customKeywordCount > 0) {
                            Text(
                                text = "কাস্টম: $customKeywordCount টি",
                                fontFamily = IslamicSansFont,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = SubtleGrayText)
                            )
                        }
                    }
                }
            }

            // Action Buttons: + কি-ওয়ার্ড & + ডোমেইন (Zero PIN required!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddKeywordDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkForestText),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SageGreenBorder)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF8A80))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ কি-ওয়ার্ড", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { showAddDomainDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkForestText),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SageGreenBorder)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepEmerald)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ ডোমেইন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Add Blocked Keyword Dialog
    if (showAddKeywordDialog) {
        var newKeyword by remember { mutableStateOf("") }
        var keywordError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddKeywordDialog = false },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFF8A80)) },
            title = {
                Text(
                    text = "Add Blocked Keyword",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "টাইপিং বা সার্চ থেকে নিষিদ্ধ করতে কি-ওয়ার্ড লিখুন। যোগ হলে এটি স্থায়ীভাবে ফিল্টার হবে এবং অ্যাপ থেকে মুছতে পারবেন না।",
                        fontFamily = IslamicSansFont,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlateText
                    )
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = {
                            newKeyword = it
                            keywordError = null
                        },
                        label = { Text("অবাঞ্ছিত কি-ওয়ার্ড") },
                        placeholder = { Text("e.g. offensive word") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (keywordError != null) {
                        Text(
                            text = keywordError!!,
                            color = if (keywordError!!.contains("ইতিমধ্যে") || keywordError!!.contains("Already")) Color(0xFFFFB74D) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = actualRepo.addKeyword(newKeyword)
                        when (result) {
                            is AddFilterResult.AlreadyExists -> {
                                keywordError = result.message
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            }
                            is AddFilterResult.Error -> {
                                keywordError = result.message
                            }
                            is AddFilterResult.Success -> {
                                blockedKeywordCount = actualRepo.getTotalKeywordCount()
                                customKeywordCount = actualRepo.getCustomKeywordCount()
                                showAddKeywordDialog = false
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("স্থায়ীভাবে ব্লক করুন", color = WarmIvorySurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeywordDialog = false }) {
                    Text("বাতিল", color = MutedSlateText)
                }
            },
            containerColor = WarmIvorySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add Blocked Domain Dialog
    if (showAddDomainDialog) {
        var newDomain by remember { mutableStateOf("") }
        var domainError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            icon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF8A80)) },
            title = {
                Text(
                    text = "Add Blocked Domain",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "স্টাডি পোর্টাল থেকে ব্রাউজিং নিষিদ্ধ করতে ডোমেইন লিখুন। যোগ হলে এটি স্থায়ীভাবে ব্লক হবে।",
                        fontFamily = IslamicSansFont,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlateText
                    )
                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = {
                            newDomain = it
                            domainError = null
                        },
                        label = { Text("ডোমেইন বা ওয়েবসাইট ঠিকানা") },
                        placeholder = { Text("e.g. badsite.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (domainError != null) {
                        Text(
                            text = domainError!!,
                            color = if (domainError!!.contains("ইতিমধ্যে") || domainError!!.contains("Already")) Color(0xFFFFB74D) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = actualRepo.addDomain(newDomain)
                        when (result) {
                            is AddFilterResult.AlreadyExists -> {
                                domainError = result.message
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            }
                            is AddFilterResult.Error -> {
                                domainError = result.message
                            }
                            is AddFilterResult.Success -> {
                                blockedDomainCount = actualRepo.getTotalDomainCount()
                                customDomainCount = actualRepo.getCustomDomainCount()
                                showAddDomainDialog = false
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("স্থায়ীভাবে ব্লক করুন", color = WarmIvorySurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) {
                    Text("বাতিল", color = MutedSlateText)
                }
            },
            containerColor = WarmIvorySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
