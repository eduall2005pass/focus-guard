package com.focusguard.essentials.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.essentials.ui.theme.*
import com.focusguard.study.model.ApprovedWebsite
import com.focusguard.study.repository.StudyWebsiteRegistry

@Composable
fun StudyResourcesCard(
    modifier: Modifier = Modifier,
    websiteRegistry: StudyWebsiteRegistry? = null,
    onOpenWebsite: (ApprovedWebsite) -> Unit,
    onOpenAllSites: () -> Unit
) {
    val context = LocalContext.current
    val actualRegistry = websiteRegistry ?: remember { StudyWebsiteRegistry(context) }
    var websitesList by remember { mutableStateOf(actualRegistry.loadWebsites()) }
    var showAddDialog by remember { mutableStateOf(false) }

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
                        .background(SageGreenLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = DeepEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Study Resources & Approved Sites",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkForestText
                    )
                    Text(
                        text = "${websitesList.size}টি অনুমোদিত অধ্যয়ন পোর্টাল · DU Services, BoiMate, GTC",
                        fontFamily = IslamicSansFont,
                        fontSize = 11.sp,
                        color = MutedSlateText
                    )
                }
            }

            // Quick Resource Grid (Top 4 resources)
            val previewWebsites = websitesList.take(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                previewWebsites.chunked(2).forEach { rowSites ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSites.forEach { site ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarmIvorySubtle)
                                    .border(0.8.dp, SageGreenBorder, RoundedCornerShape(10.dp))
                                    .clickable { onOpenWebsite(site) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = getSiteIcon(site.iconName),
                                        contentDescription = null,
                                        tint = SageGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = site.title,
                                            fontFamily = IslamicSansFont,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = DarkForestText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = site.domain,
                                            fontFamily = IslamicSansFont,
                                            fontSize = 10.sp,
                                            color = SubtleGrayText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (rowSites.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Action Buttons (Open All Sites & Add Site)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenAllSites,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সকল সাইট দেখুন", color = WarmIvorySurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkForestText),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SageGreenBorder)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepEmerald)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ সাইট যোগ", fontSize = 12.sp)
                }
            }
        }
    }

    if (showAddDialog) {
        AddStudySiteDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, url, note ->
                val result = actualRegistry.addWebsite(title, url, note)
                if (result.isSuccess) {
                    websitesList = actualRegistry.loadWebsites()
                    showAddDialog = false
                    Toast.makeText(context, "$title সাইটটি সফলভাবে যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "সাইটটি যুক্ত করা সম্ভব হয়নি"
                    Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun AddStudySiteDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Educational Website",
                fontFamily = IslamicSerifFont,
                fontWeight = FontWeight.Bold,
                color = DarkForestText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "অনুমোদিত নতুন স্টাডি ওয়েবসাইট যুক্ত করুন। সোশ্যাল মিডিয়া বা ক্ষতিকর সাইট স্বয়ংক্রিয়ভাবে ব্লক থাকবে।",
                    fontFamily = IslamicSansFont,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlateText
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Website Title") },
                    placeholder = { Text("e.g. BoiMate or Medscape") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL / Domain") },
                    placeholder = { Text("e.g. boimate.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Category (Optional)") },
                    placeholder = { Text("e.g. Islamic & Academic Books") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        onAdd(title.trim(), url.trim(), note.trim().ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("যুক্ত করুন", color = WarmIvorySurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = MutedSlateText)
            }
        },
        containerColor = WarmIvorySurface,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun getSiteIcon(iconName: String): ImageVector {
    return when (iconName) {
        "account_balance" -> Icons.Default.AccountBalance
        "menu_book" -> Icons.Default.MenuBook
        "medical_services" -> Icons.Default.MedicalServices
        "psychology" -> Icons.Default.Psychology
        "folder_shared" -> Icons.Default.FolderShared
        "auto_stories" -> Icons.Default.AutoStories
        "quiz" -> Icons.Default.Quiz
        "science" -> Icons.Default.Science
        "biotech" -> Icons.Default.Biotech
        else -> Icons.Default.Public
    }
}
