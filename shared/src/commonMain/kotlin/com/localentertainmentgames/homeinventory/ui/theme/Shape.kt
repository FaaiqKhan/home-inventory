package com.localentertainmentgames.homeinventory.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** --le-radius-* and --le-space-* (4-pt). */
object Radius {
    val xs = 6.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp // cards
    val xl = 32.dp // sheets, dialogs
}

object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp
    val s10 = 40.dp
    val s12 = 48.dp
}

/** --le-shadow-1/2/3 map to elevation 2 / 6 / 12 dp; CTA shadow is a neutral 4 dp. */
object Elevation {
    val e1 = 2.dp
    val e2 = 6.dp
    val e3 = 12.dp
    val cta = 4.dp
}

val PillShape = CircleShape

val LeShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)
