package com.lorenzo.monefyme.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.ui.category.CategoryIconRegistry
import com.lorenzo.monefyme.ui.category.DefaultCategories
import kotlinx.coroutines.launch

private val AppGreen = Color(0xFF74C792)
private val AppBackground = Color(0xFFEAF8F2)
private val AppTextPrimary = Color(0xFF2D3336)
private val AppTextSecondary = Color(0xFF4B5558)
private val AppTextMuted = Color(0xFF95A2A3)
private val AppDivider = Color(0xFFD4EDE0)

private val colorPalette = listOf(
    Color(0xFFC49B00), Color(0xFF99631E), Color(0xFF7788A3),
    Color(0xFFA665CA), Color(0xFFAFA0BE), Color(0xFF7EB7A4),
    Color(0xFFC28B47), Color(0xFFDE6CA5), Color(0xFFB5A8B8),
    Color(0xFFD95A5A), Color(0xFF6EA9E6), Color(0xFF67B68C),
    Color(0xFF6AB8B1), Color(0xFFC78A00), Color(0xFF5E7EA5),
    Color(0xFFDF6E75), Color(0xFF74C792), Color(0xFF8F9A9D),
    Color(0xFFE07B42), Color(0xFF5B8C5A),
)

private fun CategoryItem.resolveIcon(): ImageVector {
    iconName?.let { CategoryIconRegistry.findByName(it) }?.let { return it }
    return DefaultCategories.findByName(displayName)?.icon ?: DefaultCategories.fallbackIcon
}

private fun CategoryItem.resolveColor(): Color {
    colorArgb?.let { return Color(it.toLong() and 0xFFFFFFFFL) }
    return DefaultCategories.findByName(displayName)?.color ?: DefaultCategories.fallbackColor
}

@Composable
fun CategoriesSidebar(
    categories: List<CategoryItem>,
    onAddCategory: suspend (displayName: String, iconName: String, colorArgb: Int) -> Boolean,
    onClose: () -> Unit,
) {
    var showCategories by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showCategories) {
        showCategories = false
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp),
        color = AppBackground,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SidebarHeader(
                showingCategories = showCategories,
                onBack = { showCategories = false },
                onClose = onClose,
            )
            if (!showCategories) {
                MenuBody(onOpenCategories = { showCategories = true })
            } else {
                CategoriesBody(
                    categories = categories,
                    onAddCategory = { showAddDialog = true },
                )
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, iconName, argb ->
                val success = onAddCategory(name, iconName, argb)
                if (success) showAddDialog = false
                success
            },
        )
    }
}

@Composable
private fun SidebarHeader(
    showingCategories: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = AppGreen) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showingCategories) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Categories",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(48.dp))
            } else {
                Spacer(Modifier.width(48.dp))
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MenuBody(onOpenCategories: () -> Unit) {
    Column {
        Divider(color = AppDivider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCategories)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTextPrimary,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppGreen,
            )
        }
        Divider(color = AppDivider)
    }
}

@Composable
private fun CategoriesBody(
    categories: List<CategoryItem>,
    onAddCategory: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Divider(color = AppDivider)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(categories, key = { it.id }) { category ->
                CategoryRow(category)
                Divider(color = AppDivider)
            }
            if (categories.isEmpty()) {
                item {
                    Text(
                        text = "No categories yet.",
                        modifier = Modifier.padding(16.dp),
                        color = AppTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Divider(color = AppDivider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            OutlinedButton(
                onClick = onAddCategory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, AppGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AppBackground,
                    contentColor = AppTextSecondary,
                ),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Add Category")
            }
        }
    }
}

@Composable
private fun CategoryRow(item: CategoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(item.resolveColor()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.resolveIcon(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTextPrimary,
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: suspend (displayName: String, iconName: String, colorArgb: Int) -> Boolean,
) {
    var name by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf(CategoryIconRegistry.all.first().name) }
    var selectedColor by remember { mutableStateOf(colorPalette.first()) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppBackground,
        ) {
            Column {
                // Green header
                Surface(
                    color = AppGreen,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.width(48.dp))
                        Text(
                            text = "New Category",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Form
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Category name", color = AppTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = AppGreen,
                            unfocusedIndicatorColor = Color(0xFFA4B0B1),
                            focusedTextColor = AppTextPrimary,
                            unfocusedTextColor = AppTextPrimary,
                            cursorColor = AppGreen,
                        ),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Icon",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppTextSecondary,
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(CategoryIconRegistry.all, key = { it.name }) { option ->
                                val isSelected = option.name == selectedIconName
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) selectedColor else AppBackground,
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) selectedColor else AppDivider,
                                            shape = RoundedCornerShape(6.dp),
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { selectedIconName = option.name },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = option.vector,
                                        contentDescription = option.name,
                                        tint = if (isSelected) Color.White else AppTextSecondary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Color",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppTextSecondary,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(colorPalette) { color ->
                                val isSelected = color == selectedColor
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.5.dp, AppGreen, CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { selectedColor = color },
                                )
                            }
                        }
                    }
                }

                // Footer buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = AppTextMuted)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            scope.launch {
                                saving = true
                                onSave(name.trim(), selectedIconName, selectedColor.toArgb())
                                saving = false
                            }
                        },
                        enabled = name.isNotBlank() && !saving,
                    ) {
                        Text(
                            text = "Save",
                            color = if (name.isNotBlank() && !saving) AppGreen else AppTextMuted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
