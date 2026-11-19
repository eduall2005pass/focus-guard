package com.focusguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.data.model.AppCategory
import com.focusguard.data.model.AppItem
import com.focusguard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedAppsScreen(
    installedApps: List<AppItem>,
    customAllowed: Set<String>,
    onAddCustomPackage: (String) -> Unit,
    onRemoveCustomPackage: (String) -> Unit,
    onReinstallPackage: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newPackageInput by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, searchQuery, selectedCategory) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isEmpty() ||
                    app.displayName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || app.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GuardGreen,
                contentColor = DarkBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add custom allowed package")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps or package name...", color = TextMuted) },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GuardGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GuardGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${filteredApps.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GuardGreenContainer,
                            selectedLabelColor = GuardGreenText,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }

                val categoriesWithApps = AppCategory.values().filter { cat ->
                    installedApps.any { it.category == cat }
                }

                items(categoriesWithApps) { category ->
                    val count = installedApps.count { it.category == category }
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text("${category.title} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (category.isApprovedByDefault) GuardGreenContainer else GuardRedContainer,
                            selectedLabelColor = if (category.isApprovedByDefault) GuardGreenText else GuardRedText,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            // Apps List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isCustom = customAllowed.contains(app.packageName)
                    AppItemRow(
                        app = app,
                        isCustom = isCustom,
                        onDeleteCustom = { onRemoveCustomPackage(app.packageName) },
                        onReinstall = { onReinstallPackage?.invoke(app.packageName) }
                    )
                }
            }
        }
    }

    // Add Custom Package Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newPackageInput = ""
            },
            title = {
                Text("Add Allowed Package", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the exact Android package name to approve (e.g., com.example.app):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newPackageInput,
                        onValueChange = { newPackageInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("com.company.app", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GuardGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pkg = newPackageInput.trim()
                        if (pkg.isNotEmpty()) {
                            onAddCustomPackage(pkg)
                        }
                        showAddDialog = false
                        newPackageInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GuardGreen, contentColor = DarkBackground)
                ) {
                    Text("Add to Allowlist")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newPackageInput = ""
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun AppItemRow(
    app: AppItem,
    isCustom: Boolean,
    onDeleteCustom: () -> Unit,
    onReinstall: (() -> Unit)? = null
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            app.isEssential -> GuardGreenContainer.copy(alpha = 0.5f)
                            app.isAllowed -> AccentBlueContainer.copy(alpha = 0.4f)
                            else -> GuardRedContainer.copy(alpha = 0.4f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        app.isEssential -> Icons.Default.Shield
                        app.isAllowed -> Icons.Default.CheckCircle
                        else -> Icons.Default.Block
                    },
                    contentDescription = null,
                    tint = when {
                        app.isEssential -> GuardGreenText
                        app.isAllowed -> AccentBlue
                        else -> GuardRedText
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = app.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        maxLines = 1
                    )

                    if (app.isEssential) {
                        Text(
                            text = "ESSENTIAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GuardGreenText
                            ),
                            modifier = Modifier
                                .background(GuardGreenContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (isCustom) {
                        Text(
                            text = "CUSTOM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            ),
                            modifier = Modifier
                                .background(AccentBlueContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (!app.isInstalled && app.isAllowed) {
                        Text(
                            text = "UNINSTALLED (APPROVED)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GuardAmberText
                            ),
                            modifier = Modifier
                                .background(GuardAmberContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextMuted
                    ),
                    maxLines = 1
                )

                if (app.note != null) {
                    Text(
                        text = "• ${app.note}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = GuardAmberText
                        ),
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!app.isInstalled && app.isAllowed) {
                    IconButton(
                        onClick = { onReinstall?.invoke() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Install / Reinstall from store",
                            tint = GuardGreenText
                        )
                    }
                }

                if (isCustom) {
                    IconButton(
                        onClick = onDeleteCustom,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove custom allowance",
                            tint = GuardRedText
                        )
                    }
                } else if (app.isInstalled) {
                    Text(
                        text = if (app.isAllowed) "Allowed" else "Blocked",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (app.isAllowed) GuardGreenText else GuardRedText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
