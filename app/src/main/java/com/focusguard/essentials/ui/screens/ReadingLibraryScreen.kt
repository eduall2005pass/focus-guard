package com.focusguard.essentials.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.essentials.data.model.Book
import com.focusguard.essentials.data.repository.DailyEssentialsRepository
import com.focusguard.essentials.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ReadingLibraryScreen(
    repository: DailyEssentialsRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var booksList by remember { mutableStateOf<List<Pair<Book, Int>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    fun refreshCatalog() {
        coroutineScope.launch {
            val catalog = repository.getBooksCatalog()
            val listWithPages = catalog.map { book ->
                val lifetimePages = repository.getLifetimePagesForBook(book.id)
                Pair(book, lifetimePages)
            }
            booksList = listWithPages
        }
    }

    LaunchedEffect(Unit) {
        refreshCatalog()
    }

    val filteredBooks = remember(booksList, searchQuery) {
        if (searchQuery.isBlank()) {
            booksList
        } else {
            booksList.filter { it.first.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvoryBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Reading Library",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = DarkForestText
                )
                Text(
                    text = "${booksList.size} books in your study library",
                    fontFamily = IslamicSansFont,
                    fontSize = 12.sp,
                    color = MutedSlateText
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Book", color = WarmIvorySurface)
            }
        }

        // Search Bar (Book Search Option)
        if (booksList.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = "Search books by title...",
                        fontFamily = IslamicSansFont,
                        fontSize = 13.sp,
                        color = InputPillPlaceholder
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SageGreen,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MutedSlateText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DarkForestText,
                    unfocusedTextColor = DarkForestText,
                    focusedContainerColor = WarmIvorySurface,
                    unfocusedContainerColor = WarmIvorySurface,
                    focusedBorderColor = DeepEmerald,
                    unfocusedBorderColor = WarmCardBorder,
                    cursorColor = DeepEmerald
                )
            )
        }

        if (booksList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_book_open),
                        contentDescription = null,
                        tint = SageGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No books added yet",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        color = DarkForestText
                    )
                    Text(
                        text = "Add books to easily track daily and monthly reading progress.",
                        fontFamily = IslamicSansFont,
                        fontSize = 12.sp,
                        color = MutedSlateText
                    )
                }
            }
        } else if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MutedSlateText,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No books found",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        color = DarkForestText
                    )
                    Text(
                        text = "No results matching \"$searchQuery\"",
                        fontFamily = IslamicSansFont,
                        fontSize = 12.sp,
                        color = MutedSlateText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBooks, key = { it.first.id }) { (book, lifetimePages) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(WarmIvorySurface)
                            .border(1.dp, WarmCardBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SageGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_book_open),
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = book.title,
                                        fontFamily = IslamicSansFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DarkForestText
                                    )
                                    val totalPagesLabel = if (book.totalPages > 0) " / ${book.totalPages} pages" else ""
                                    Text(
                                        text = "$lifetimePages lifetime pages$totalPagesLabel",
                                        fontFamily = IslamicSansFont,
                                        fontSize = 12.sp,
                                        color = MutedSlateText
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.deleteBook(book.id)
                                        refreshCatalog()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MutedSlateText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newTotalPages by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add New Book",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Book Title", color = MutedSlateText) },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTotalPages,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) newTotalPages = it },
                        label = { Text("Total Pages (Optional)", color = MutedSlateText) },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newTitle.trim()
                        if (trimmed.isNotBlank()) {
                            val pages = newTotalPages.toIntOrNull() ?: 0
                            coroutineScope.launch {
                                repository.getOrCreateBook(trimmed, pages)
                                refreshCatalog()
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                ) {
                    Text("Add Book", color = WarmIvorySurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MutedSlateText)
                }
            },
            containerColor = WarmIvorySurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
