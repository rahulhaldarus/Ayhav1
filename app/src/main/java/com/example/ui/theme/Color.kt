package com.example.ui.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.darkColorScheme

val PrimaryBackground = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val AccentPurple = Color(0xFF7C4DFF)
val SecondaryLavender = Color(0xFFA78BFA)
val TextWhite = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val NeonGlow = Color(0xFF8B5CF6)
val GlassBg = Color(0x1F0F172A)
val GlassBorder = Color(0x337C4DFF)

val CustomDarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = SecondaryLavender,
    background = PrimaryBackground,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

