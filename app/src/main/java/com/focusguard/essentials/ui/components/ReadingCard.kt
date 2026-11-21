package com.focusguard.essentials.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.essentials.data.model.Book
import com.focusguard.essentials.data.model.DailyReadingRecord
import com.focusguard.essentials.ui.theme.*

@Composable
fun ReadingCard(
    readingsToday: List<DailyReadingRecord>,
    booksCatalog: List<Book>,
    onAddReading: (title: String, pages: Int, totalPages: Int) -> Unit,
    onDeleteReading: (readingId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bookNameInput by remember { mutableStateOf("") }
    var pagesInput by remember { mutableStateOf("") }
    var totalPagesInput by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(true) }
    var showSuggestions by remember { mutableStateOf(false) }

    // If books are already logged today, default to saved list view (isAddingNew = false).
    // If no books logged yet today, default to input mode (isAddingNew = true).
    var isAddingNew by remember(readingsToday.isEmpty()) {
        mutableStateOf(readingsToday.isEmpty())
    }

    var editingRecord by remember { mutableStateOf<DailyReadingRecord?>(null) }

    val filteredSuggestions = remember(bookNameInput, booksCatalog) {
        if (bookNameInput.isBlank()) {
            booksCatalog.map { it.title }.take(5)
        } else {
            booksCatalog
                .filter { it.title.contains(bookNameInput, ignoreCase = true) }
                .map { it.title }
                .take(5)
        }
    }

    fun submitNewReading() {
        val trimmedTitle = bookNameInput.trim()
        if (trimmedTitle.isEmpty()) {
            Toast.makeText(context, "Please enter a book name", Toast.LENGTH_SHORT).show()
            return
        }
        val pages = pagesInput.toIntOrNull() ?: 1
        val totalPages = totalPagesInput.toIntOrNull() ?: 0

        onAddReading(trimmedTitle, pages, totalPages)
        Toast.makeText(context, "✓ Reading saved for '$trimmedTitle'!", Toast.LENGTH_SHORT).show()
        bookNameInput = ""
        pagesInput = ""
        totalPagesInput = ""
        showSuggestions = false
        isExpanded = true
        isAddingNew = false
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
            // Header Row: Book Icon | Title + Subtitle | [ Edit / Add ] or [ Done ] + Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_book_open),
                    contentDescription = "Reading",
                    tint = DeepEmerald,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Reading",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkForestText
                    )
                    Text(
                        text = if (readingsToday.isEmpty()) "Log today's reading" else "${readingsToday.size} book${if (readingsToday.size > 1) "s" else ""} logged today",
                        fontFamily = IslamicSansFont,
                        fontSize = 11.sp,
                        color = if (readingsToday.isNotEmpty()) DeepEmerald else MutedSlateText
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Right action pill button when readings exist
                if (readingsToday.isNotEmpty()) {
                    if (!isAddingNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SageGreenLight)
                                .border(1.dp, SageGreenBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    isAddingNew = true
                                    isExpanded = true
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit or Add",
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Edit / Add",
                                    fontFamily = IslamicSansFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = DeepEmerald
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmIvorySubtle)
                                .border(1.dp, WarmCardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    isAddingNew = false
                                    bookNameInput = ""
                                    pagesInput = ""
                                    showSuggestions = false
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Done",
                                fontFamily = IslamicSansFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = MutedSlateText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Toggle Expand",
                    tint = DarkCharcoalText,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (isExpanded) 90f else 0f)
                )
            }

            // Input Row: [ Book name ] [ Pages ] [ 💾 Save ]
            AnimatedVisibility(visible = isExpanded && (isAddingNew || readingsToday.isEmpty())) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Book name Pill Input
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(InputPillBackground)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (bookNameInput.isEmpty()) {
                                Text(
                                    text = "Book name",
                                    fontFamily = IslamicSansFont,
                                    fontSize = 13.sp,
                                    color = InputPillPlaceholder
                                )
                            }
                            BasicTextField(
                                value = bookNameInput,
                                onValueChange = {
                                    bookNameInput = it
                                    showSuggestions = true
                                },
                                textStyle = TextStyle(
                                    fontFamily = IslamicSansFont,
                                    fontSize = 13.sp,
                                    color = DarkCharcoalText,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(DeepEmerald),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Pages Pill Input
                        Box(
                            modifier = Modifier
                                .weight(0.85f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(InputPillBackground)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (pagesInput.isEmpty()) {
                                Text(
                                    text = "Pages",
                                    fontFamily = IslamicSansFont,
                                    fontSize = 13.sp,
                                    color = InputPillPlaceholder
                                )
                            }
                            BasicTextField(
                                value = pagesInput,
                                onValueChange = {
                                    if (it.all { ch -> ch.isDigit() }) pagesInput = it
                                },
                                textStyle = TextStyle(
                                    fontFamily = IslamicSansFont,
                                    fontSize = 13.sp,
                                    color = DarkCharcoalText,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { submitNewReading() }),
                                cursorBrush = SolidColor(DeepEmerald),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Prominent [ Save ] Pill Button on Right
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DeepEmerald)
                                .clickable { submitNewReading() }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save Reading",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Save",
                                    fontFamily = IslamicSansFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Suggestions dropdown when user types or taps book name
                    if (showSuggestions && filteredSuggestions.isNotEmpty() && bookNameInput.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WarmIvorySubtle),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text(
                                    text = "Select from Catalog:",
                                    fontFamily = IslamicSansFont,
                                    fontSize = 11.sp,
                                    color = MutedSlateText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                filteredSuggestions.forEach { suggestion ->
                                    Text(
                                        text = suggestion,
                                        fontFamily = IslamicSansFont,
                                        fontSize = 13.sp,
                                        color = DarkForestText,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                bookNameInput = suggestion
                                                showSuggestions = false
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Logged books list for today (Saved State)
            AnimatedVisibility(visible = isExpanded && readingsToday.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(color = WarmDivider, thickness = 0.8.dp)

                    readingsToday.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmIvorySubtle)
                                .border(1.dp, WarmCardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.bookTitle,
                                    fontFamily = IslamicSansFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = DarkForestText
                                )
                                Text(
                                    text = "+${item.pagesRead} pages read today",
                                    fontFamily = IslamicSansFont,
                                    fontSize = 12.sp,
                                    color = DeepEmerald
                                )
                            }

                            // Explicit [ Edit ] Pill Button on each logged item
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SageGreenLight)
                                    .border(1.dp, SageGreenBorder, RoundedCornerShape(8.dp))
                                    .clickable { editingRecord = item }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Edit",
                                        fontFamily = IslamicSansFont,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = DeepEmerald
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    onDeleteReading(item.id)
                                    Toast.makeText(context, "Removed '${item.bookTitle}'", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MutedSlateText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Reading Record Dialog
    editingRecord?.let { record ->
        var editPages by remember(record.id) { mutableStateOf(record.pagesRead.toString()) }

        AlertDialog(
            onDismissRequest = { editingRecord = null },
            title = {
                Text(
                    text = "Edit Reading: ${record.bookTitle}",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pages read today for this book:",
                        fontFamily = IslamicSansFont,
                        fontSize = 13.sp,
                        color = MutedSlateText
                    )
                    OutlinedTextField(
                        value = editPages,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) editPages = it },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = editPages.toIntOrNull() ?: record.pagesRead
                        onAddReading(record.bookTitle, parsed, record.totalPages)
                        Toast.makeText(context, "✓ Updated reading for '${record.bookTitle}'!", Toast.LENGTH_SHORT).show()
                        editingRecord = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = WarmIvorySurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRecord = null }) {
                    Text("Cancel", color = MutedSlateText)
                }
            },
            containerColor = WarmIvorySurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
