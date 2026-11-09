package com.viniciusrio.tostao.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.ui.graphics.vector.ImageVector

/** Ícones disponíveis para contas e categorias, identificados por nome estável no banco. */
object AppIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "wallet" to Icons.Outlined.AccountBalanceWallet,
        "bank" to Icons.Outlined.AccountBalance,
        "card" to Icons.Outlined.CreditCard,
        "cash" to Icons.Outlined.Payments,
        "savings" to Icons.Outlined.Savings,
        "restaurant" to Icons.Outlined.Restaurant,
        "cart" to Icons.Outlined.ShoppingCart,
        "home" to Icons.Outlined.Home,
        "car" to Icons.Outlined.DirectionsCar,
        "health" to Icons.Outlined.LocalHospital,
        "school" to Icons.Outlined.School,
        "fun" to Icons.Outlined.SportsEsports,
        "bag" to Icons.Outlined.ShoppingBag,
        "bill" to Icons.Outlined.ReceiptLong,
        "work" to Icons.Outlined.Work,
        "laptop" to Icons.Outlined.Laptop,
        "trending" to Icons.Outlined.TrendingUp,
        "pet" to Icons.Outlined.Pets,
        "travel" to Icons.Outlined.Flight,
        "fitness" to Icons.Outlined.FitnessCenter,
        "gift" to Icons.Outlined.CardGiftcard,
        "dots" to Icons.Outlined.MoreHoriz,
    )
    private val map = all.toMap()
    fun of(name: String): ImageVector = map[name] ?: Icons.Outlined.MoreHoriz

    val palette: List<Long> = listOf(
        0xFF2F9E6E, 0xFF2C8FC9, 0xFF5B6BD5, 0xFF8A5CC7, 0xFFC2569B,
        0xFFD94F70, 0xFFE8743B, 0xFFE0A526, 0xFF5D7A8C, 0xFF7D7D85,
    )
}
