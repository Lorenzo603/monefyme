package com.lorenzo.monefyme.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconRegistry {
    data class IconOption(val name: String, val vector: ImageVector)

    val all: List<IconOption> = listOf(
        IconOption("AccountBalanceWallet", Icons.Outlined.AccountBalanceWallet),
        IconOption("BakeryDining", Icons.Outlined.BakeryDining),
        IconOption("Book", Icons.Outlined.Book),
        IconOption("Call", Icons.Outlined.Call),
        IconOption("Checkroom", Icons.Outlined.Checkroom),
        IconOption("CreditCard", Icons.Outlined.CreditCard),
        IconOption("DirectionsBike", Icons.Outlined.DirectionsBike),
        IconOption("DirectionsCar", Icons.Outlined.DirectionsCar),
        IconOption("FavoriteBorder", Icons.Outlined.FavoriteBorder),
        IconOption("Fastfood", Icons.Outlined.Fastfood),
        IconOption("FitnessCenter", Icons.Outlined.FitnessCenter),
        IconOption("Flight", Icons.Outlined.Flight),
        IconOption("Home", Icons.Outlined.Home),
        IconOption("Hotel", Icons.Outlined.Hotel),
        IconOption("LocalActivity", Icons.Outlined.LocalActivity),
        IconOption("LocalAtm", Icons.Outlined.LocalAtm),
        IconOption("LocalBar", Icons.Outlined.LocalBar),
        IconOption("LocalCafe", Icons.Outlined.LocalCafe),
        IconOption("LocalHospital", Icons.Outlined.LocalHospital),
        IconOption("LocalTaxi", Icons.Outlined.LocalTaxi),
        IconOption("Movie", Icons.Outlined.Movie),
        IconOption("MusicNote", Icons.Outlined.MusicNote),
        IconOption("Pets", Icons.Outlined.Pets),
        IconOption("Redeem", Icons.Outlined.Redeem),
        IconOption("Restaurant", Icons.Outlined.Restaurant),
        IconOption("Savings", Icons.Outlined.Savings),
        IconOption("School", Icons.Outlined.School),
        IconOption("SelfImprovement", Icons.Outlined.SelfImprovement),
        IconOption("ShoppingCart", Icons.Outlined.ShoppingCart),
        IconOption("Spa", Icons.Outlined.Spa),
        IconOption("SportsEsports", Icons.Outlined.SportsEsports),
        IconOption("SportsSoccer", Icons.Outlined.SportsSoccer),
        IconOption("Star", Icons.Outlined.Star),
        IconOption("Train", Icons.Outlined.Train),
        IconOption("Work", Icons.Outlined.Work),
    )

    val default: ImageVector = Icons.Outlined.AccountBalanceWallet

    fun findByName(name: String): ImageVector? = all.firstOrNull { it.name == name }?.vector
}
