package com.lorenzo.monefyme.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Train
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object DefaultCategories {

    val all: List<CategoryDef> = listOf(
        CategoryDef("Bills", Icons.Outlined.LocalActivity, Color(0xFFC49B00)),
        CategoryDef("Breakfast out", Icons.Outlined.BakeryDining, Color(0xFF99631E)),
        CategoryDef("Caffè offerti", Icons.Outlined.BakeryDining, Color(0xFF99631E)),
        CategoryDef("Car", Icons.Outlined.DirectionsCar, Color(0xFF7788A3)),
        CategoryDef("Clothes", Icons.Outlined.Checkroom, Color(0xFFA665CA)),
        CategoryDef("Communications", Icons.Outlined.Call, Color(0xFFAFA0BE)),
        CategoryDef("Eating out", Icons.Outlined.LocalBar, Color(0xFF7EB7A4)),
        CategoryDef("Entertainment", Icons.Outlined.SportsEsports, Color(0xFFC28B47)),
        CategoryDef("Food", Icons.Outlined.Restaurant, Color(0xFFDE6CA5)),
        CategoryDef("Gifts", Icons.Outlined.Redeem, Color(0xFFB5A8B8)),
        CategoryDef("Health", Icons.Outlined.FavoriteBorder, Color(0xFFD95A5A)),
        CategoryDef("House", Icons.Outlined.Home, Color(0xFF6EA9E6)),
        CategoryDef("Pets", Icons.Outlined.Pets, Color(0xFF67B68C)),
        CategoryDef("Sports", Icons.Outlined.SportsSoccer, Color(0xFF6AB8B1)),
        CategoryDef("Taxi", Icons.Outlined.LocalTaxi, Color(0xFFC78A00)),
        CategoryDef("Toiletry", Icons.Outlined.SelfImprovement, Color(0xFF5E7EA5)),
        CategoryDef("Transport", Icons.Outlined.Train, Color(0xFFDF6E75)),
    )

    val fallbackIcon: ImageVector = Icons.Outlined.AccountBalanceWallet
    val fallbackColor: Color = Color(0xFF8F9A9D)

    private val byNormalizedName: Map<String, CategoryDef> = all.associateBy { it.normalizedName }

    // Alternative spellings that map to a canonical entry in `all`.
    private val aliases: Map<String, String> = mapOf(
        "caffe offerti" to "caffè offerti",
        "home" to "house",
        "housing" to "house",
    )

    fun findByName(name: String): CategoryDef? {
        val key = name.trim().lowercase()
        return byNormalizedName[key] ?: byNormalizedName[aliases[key]]
    }
}
