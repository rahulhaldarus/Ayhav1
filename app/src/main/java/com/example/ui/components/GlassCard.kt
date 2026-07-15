package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0x337C4DFF), // Purple Neon Glass border
    backgroundColor: Color = Color(0x1A0F172A), // Translucent deep background
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.05f),
                        borderColor,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Modifier that draws a beautiful, glowing, deep-colored ambient background.
 */
fun Modifier.neonAmbientBackground() = this.drawBehind {
    // Top-left soft purple neon glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x337C4DFF), Color.Transparent),
            radius = size.maxDimension * 0.5f
        ),
        radius = size.maxDimension * 0.5f,
        center = androidx.compose.ui.geometry.Offset(0f, 0f)
    )
    // Bottom-right soft purple neon glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x1F8B5CF6), Color.Transparent),
            radius = size.maxDimension * 0.6f
        ),
        radius = size.maxDimension * 0.6f,
        center = androidx.compose.ui.geometry.Offset(size.width, size.height)
    )
}
