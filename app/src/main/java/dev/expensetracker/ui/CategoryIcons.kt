package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.expensetracker.data.Category

/** Icon keys used by data/Taxonomy.kt. */
internal val ICONS: Map<String, ImageVector> = mapOf(
    "Restaurant" to Icons.Rounded.Restaurant, "LocalCafe" to Icons.Rounded.LocalCafe,
    "DeliveryDining" to Icons.Rounded.DeliveryDining, "Fastfood" to Icons.Rounded.Fastfood,
    "LocalBar" to Icons.Rounded.LocalBar, "BakeryDining" to Icons.Rounded.BakeryDining,
    "RamenDining" to Icons.Rounded.RamenDining, "LunchDining" to Icons.Rounded.LunchDining,
    "Icecream" to Icons.Rounded.Icecream, "ShoppingCart" to Icons.Rounded.ShoppingCart,
    "LocalGroceryStore" to Icons.Rounded.LocalGroceryStore, "Eco" to Icons.Rounded.Eco,
    "SetMeal" to Icons.Rounded.SetMeal, "LocalDrink" to Icons.Rounded.LocalDrink,
    "LocalShipping" to Icons.Rounded.LocalShipping, "Storefront" to Icons.Rounded.Storefront,
    "CleaningServices" to Icons.Rounded.CleaningServices, "DirectionsCar" to Icons.Rounded.DirectionsCar,
    "LocalGasStation" to Icons.Rounded.LocalGasStation, "LocalTaxi" to Icons.Rounded.LocalTaxi,
    "Subway" to Icons.Rounded.Subway, "DirectionsBus" to Icons.Rounded.DirectionsBus,
    "LocalParking" to Icons.Rounded.LocalParking, "Toll" to Icons.Rounded.Toll,
    "CarRepair" to Icons.Rounded.CarRepair, "TwoWheeler" to Icons.Rounded.TwoWheeler,
    "EvStation" to Icons.Rounded.EvStation, "Shield" to Icons.Rounded.Shield,
    "Flight" to Icons.Rounded.Flight, "Hotel" to Icons.Rounded.Hotel, "Train" to Icons.Rounded.Train,
    "BeachAccess" to Icons.Rounded.BeachAccess, "Public" to Icons.Rounded.Public,
    "CurrencyExchange" to Icons.Rounded.CurrencyExchange, "HealthAndSafety" to Icons.Rounded.HealthAndSafety,
    "Attractions" to Icons.Rounded.Attractions, "ShoppingBag" to Icons.Rounded.ShoppingBag,
    "Checkroom" to Icons.Rounded.Checkroom, "Devices" to Icons.Rounded.Devices,
    "PhoneAndroid" to Icons.Rounded.PhoneAndroid, "LocalMall" to Icons.Rounded.LocalMall,
    "Kitchen" to Icons.Rounded.Kitchen, "Chair" to Icons.Rounded.Chair, "Watch" to Icons.Rounded.Watch,
    "Face" to Icons.Rounded.Face, "MenuBook" to Icons.AutoMirrored.Rounded.MenuBook, "Toys" to Icons.Rounded.Toys,
    "Home" to Icons.Rounded.Home, "Apartment" to Icons.Rounded.Apartment, "Handyman" to Icons.Rounded.Handyman,
    "Weekend" to Icons.Rounded.Weekend, "AccountBalance" to Icons.Rounded.AccountBalance,
    "Lock" to Icons.Rounded.Lock, "Receipt" to Icons.Rounded.Receipt, "Bolt" to Icons.Rounded.Bolt,
    "WaterDrop" to Icons.Rounded.WaterDrop, "LocalFireDepartment" to Icons.Rounded.LocalFireDepartment,
    "Phone" to Icons.Rounded.Phone, "Wifi" to Icons.Rounded.Wifi, "Tv" to Icons.Rounded.Tv,
    "Subscriptions" to Icons.Rounded.Subscriptions, "LiveTv" to Icons.Rounded.LiveTv,
    "MusicNote" to Icons.Rounded.MusicNote, "Cloud" to Icons.Rounded.Cloud, "Apps" to Icons.Rounded.Apps,
    "AutoAwesome" to Icons.Rounded.AutoAwesome, "Newspaper" to Icons.Rounded.Newspaper,
    "SportsEsports" to Icons.Rounded.SportsEsports, "CardMembership" to Icons.Rounded.CardMembership,
    "LocalHospital" to Icons.Rounded.LocalHospital, "LocalPharmacy" to Icons.Rounded.LocalPharmacy,
    "MedicalServices" to Icons.Rounded.MedicalServices, "Biotech" to Icons.Rounded.Biotech,
    "Visibility" to Icons.Rounded.Visibility, "Psychology" to Icons.Rounded.Psychology,
    "FitnessCenter" to Icons.Rounded.FitnessCenter, "SelfImprovement" to Icons.Rounded.SelfImprovement,
    "SportsTennis" to Icons.Rounded.SportsTennis, "ContentCut" to Icons.Rounded.ContentCut,
    "Spa" to Icons.Rounded.Spa, "Medication" to Icons.Rounded.Medication,
    "TheaterComedy" to Icons.Rounded.TheaterComedy, "Movie" to Icons.Rounded.Movie,
    "Celebration" to Icons.Rounded.Celebration, "Palette" to Icons.Rounded.Palette,
    "PhotoCamera" to Icons.Rounded.PhotoCamera, "School" to Icons.Rounded.School,
    "CastForEducation" to Icons.Rounded.CastForEducation, "Quiz" to Icons.Rounded.Quiz,
    "Laptop" to Icons.Rounded.Laptop, "FamilyRestroom" to Icons.Rounded.FamilyRestroom,
    "ChildCare" to Icons.Rounded.ChildCare, "Pets" to Icons.Rounded.Pets, "Elderly" to Icons.Rounded.Elderly,
    "LocalLaundryService" to Icons.Rounded.LocalLaundryService, "Payments" to Icons.Rounded.Payments,
    "CreditCard" to Icons.Rounded.CreditCard, "TrendingUp" to Icons.AutoMirrored.Rounded.TrendingUp,
    "ShowChart" to Icons.AutoMirrored.Rounded.ShowChart, "Diamond" to Icons.Rounded.Diamond,
    "Gavel" to Icons.Rounded.Gavel, "Percent" to Icons.Rounded.Percent, "SwapHoriz" to Icons.Rounded.SwapHoriz,
    "People" to Icons.Rounded.People, "SyncAlt" to Icons.Rounded.SyncAlt,
    "CallSplit" to Icons.AutoMirrored.Rounded.CallSplit, "AccountBalanceWallet" to Icons.Rounded.AccountBalanceWallet,
    "LocalAtm" to Icons.Rounded.LocalAtm, "VolunteerActivism" to Icons.Rounded.VolunteerActivism,
    "TempleHindu" to Icons.Rounded.TempleHindu, "CardGiftcard" to Icons.Rounded.CardGiftcard,
    "Groups" to Icons.Rounded.Groups, "Work" to Icons.Rounded.Work, "Replay" to Icons.Rounded.Replay,
    "Loyalty" to Icons.Rounded.Loyalty, "Savings" to Icons.Rounded.Savings,
    "ReceiptLong" to Icons.AutoMirrored.Rounded.ReceiptLong, "Redeem" to Icons.Rounded.Redeem,
    "MoreHoriz" to Icons.Rounded.MoreHoriz, "HelpOutline" to Icons.AutoMirrored.Rounded.HelpOutline,
)

fun iconFor(c: Category?): ImageVector = c?.icon?.let(ICONS::get) ?: if (c == null) Icons.Outlined.Sell else Icons.Rounded.Category

/** Tinted circle with the category's icon; untagged shows a neutral tag icon. */
@Composable
fun CategoryAvatar(c: Category?, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val tint = c?.let { scheme.accent(it.color) } ?: scheme.onSurfaceVariant
    IconAvatar(iconFor(c), tint, size, modifier, background = if (c == null) scheme.surfaceContainerHighest else null)
}

@Composable
fun IconAvatar(icon: ImageVector, tint: Color, size: Dp = 44.dp, modifier: Modifier = Modifier, background: Color? = null) {
    Box(
        modifier.size(size).background(background ?: tint.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f)) }
}
