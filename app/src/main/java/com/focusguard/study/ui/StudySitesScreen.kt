package com.focusguard.study.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.model.ApprovedWebsite
import com.focusguard.study.repository.StudyWebsiteRegistry
import com.focusguard.ui.theme.*

@Composable
fun StudySitesScreen(
    websiteRegistry: StudyWebsiteRegistry,
    onOpenWebsite: (ApprovedWebsite) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var websitesList by remember { mutableStateOf(websiteRegistry.loadWebsites()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val filteredWebsites = remember(websitesList, searchQuery) {
        if (searchQuery.isBlank()) websitesList
        else websitesList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.domain.contains(searchQuery, ignoreCase = true) ||
            (it.note?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Study Resources",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "BoiMate, Medical Contents ও অনুমোদিত সাইটস",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuardGreenContainer,
                    contentColor = GuardGreenText
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Site", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { input ->
                val hit = FocusInputFilter.explicitHit(input)
                if (hit != null) {
                    val coolingOff = com.focusguard.study.filter.StudyCoolingOffManager(context)
                    val result = coolingOff.recordViolation(hit)
                    searchQuery = ""
                    when (result) {
                        is com.focusguard.study.filter.CoolingOffEvent.Warning -> {
                            Toast.makeText(context, "⚠️ আপত্তিকর শব্দ ব্লক ও মুছে ফেলা হয়েছে! ($hit)\nসতর্কতা: ${result.currentAttempts}/10 (বাকি ${result.remainingUntilLock}টি)", Toast.LENGTH_SHORT).show()
                        }
                        is com.focusguard.study.filter.CoolingOffEvent.Triggered -> {
                            Toast.makeText(context, "🚨 ১০ বার আপত্তিকর শব্দ শনাক্ত হওয়ায় ১ ঘণ্টার জন্য ইমার্জেন্সি রেস্ট্রিকশন চালু হলো!", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    searchQuery = input
                }
            },
            placeholder = { Text("Search study sites...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GuardGreen,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Websites Grid (2 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredWebsites, key = { it.id }) { site ->
                StudySiteCard(
                    website = site,
                    onClick = { onOpenWebsite(site) },
                    onDelete = if (!site.isPredefined) {
                        {
                            websiteRegistry.removeWebsite(site.id)
                            websitesList = websiteRegistry.loadWebsites()
                            Toast.makeText(context, "Removed ${site.title}", Toast.LENGTH_SHORT).show()
                        }
                    } else null
                )
            }
        }
    }

    if (showAddDialog) {
        AddStudySiteDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, url, note ->
                val result = websiteRegistry.addWebsite(title, url, note)
                if (result.isSuccess) {
                    websitesList = websiteRegistry.loadWebsites()
                    showAddDialog = false
                    Toast.makeText(context, "Added ${title.trim()} successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed to add website", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun StudySiteCard(
    website: ApprovedWebsite,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            when (website.id) {
                                "du_portal" -> Color(0xFF064E3B)
                                "boimate" -> Color(0xFF1E3A8A)
                                "medical_contents" -> Color(0xFF065F46)
                                "gtc" -> Color(0xFF7C2D12)
                                "notebooklm" -> Color(0xFF4C1D95)
                                "gemini" -> Color(0xFF1E293B)
                                else -> GuardGreenContainer
                            },
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getSiteIcon(website.iconName, website.id),
                        contentDescription = null,
                        tint = when (website.id) {
                            "du_portal" -> Color(0xFFA7F3D0)
                            "boimate" -> Color(0xFF93C5FD)
                            "medical_contents" -> Color(0xFF6EE7B7)
                            "gtc" -> Color(0xFFFDBA74)
                            "notebooklm" -> Color(0xFFD8B4FE)
                            "gemini" -> Color(0xFF93C5FD)
                            else -> GuardGreenText
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (website.isPredefined) {
                    Box(
                        modifier = Modifier
                            .background(GuardGreenContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFICIAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GuardGreenText
                            )
                        )
                    }
                } else if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = website.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                )

                Text(
                    text = website.domain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GuardGreenText,
                        fontSize = 10.sp
                    )
                )

                if (website.note != null) {
                    Text(
                        text = website.note,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    )
                }
            }
        }
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
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "অনুমোদিত নতুন স্টাডি ওয়েবসাইট বা পোর্টাল যুক্ত করুন। সার্চ ইঞ্জিন, সোশ্যাল মিডিয়া বা ক্ষতিকর সাইট স্বয়ংক্রিয়ভাবে প্রত্যাখ্যাত হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Website Title") },
                    placeholder = { Text("e.g. Medscape or Osmosis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL / Domain") },
                    placeholder = { Text("e.g. medscape.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Description / Note (Optional)") },
                    placeholder = { Text("e.g. Clinical references") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        onAdd(title, url, note.takeIf { it.isNotBlank() })
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuardGreen,
                    contentColor = DarkBackground
                ),
                enabled = title.isNotBlank() && url.isNotBlank()
            ) {
                Text("Add Website", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkSurface,
        textContentColor = TextPrimary
    )
}

private fun getSiteIcon(iconName: String, siteId: String): ImageVector {
    return when {
        siteId == "du_portal" || iconName == "account_balance" -> Icons.Default.AccountBalance
        siteId == "boimate" -> Icons.Default.MenuBook
        siteId == "medical_contents" -> Icons.Default.FolderShared
        siteId == "gtc" -> Icons.Default.MedicalServices
        siteId == "notebooklm" || siteId == "gemini" -> Icons.Default.Psychology
        siteId == "livemcq" -> Icons.Default.Quiz
        siteId == "muslimbangla" -> Icons.Default.Mosque
        siteId == "ihadis" -> Icons.Default.AutoStories
        iconName == "medical_services" -> Icons.Default.MedicalServices
        iconName == "folder_shared" -> Icons.Default.FolderShared
        iconName == "menu_book" -> Icons.Default.MenuBook
        iconName == "psychology" -> Icons.Default.Psychology
        iconName == "quiz" -> Icons.Default.Quiz
        iconName == "mosque" -> Icons.Default.Mosque
        iconName == "auto_stories" -> Icons.Default.AutoStories
        else -> Icons.Default.Language
    }
}
