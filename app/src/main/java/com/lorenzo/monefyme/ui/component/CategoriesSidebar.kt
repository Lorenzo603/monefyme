package com.lorenzo.monefyme.ui.component

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.ui.category.CategoryIconRegistry
import com.lorenzo.monefyme.ui.category.DefaultCategories
import kotlinx.coroutines.launch

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
        shadowElevation = 16.dp,
    ) {
        if (!showCategories) {
            MainMenuContent(
                onOpenCategories = { showCategories = true },
                onClose = onClose,
            )
        } else {
            CategoriesListContent(
                categories = categories,
                onBack = { showCategories = false },
                onAddCategory = { showAddDialog = true },
            )
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
private fun MainMenuContent(
    onOpenCategories: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close")
            }
        }
        Divider()
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
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF888888),
            )
        }
    }
}

@Composable
private fun CategoriesListContent(
    categories: List<CategoryItem>,
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Divider()
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(categories, key = { it.id }) { category ->
                CategoryListItem(category)
                Divider(color = Color(0xFFEEEEEE))
            }
            if (categories.isEmpty()) {
                item {
                    Text(
                        text = "No categories yet.",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF888888),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Divider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            OutlinedButton(onClick = onAddCategory) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Add Category")
            }
        }
    }
}

@Composable
private fun CategoryListItem(item: CategoryItem) {
    val icon = item.resolveIcon()
    val color = item.resolveColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyMedium,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
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
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) selectedColor else Color(0xFFF0F0F0),
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
                                    tint = if (isSelected) Color.White else Color(0xFF666666),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Color", style = MaterialTheme.typography.labelLarge)
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
                                            Modifier.border(2.5.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
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
        },
        confirmButton = {
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
