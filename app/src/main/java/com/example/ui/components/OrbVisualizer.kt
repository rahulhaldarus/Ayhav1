package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun OrbVisualizer(
    state: String, // idle, listening, thinking, speaking
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheScale"
    )
    
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheAlpha"
    )

    val thinkingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThinkingRotation"
    )

    val listeningPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListeningPulse"
    )

    val speakingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpeakingPhase"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width * 0.35f

            val auraColor = when (state) {
                "listening" -> Color(0xFFEF4444)
                "thinking" -> Color(0xFF3B82F6)
                "speaking" -> Color(0xFF10B981)
                else -> Color(0xFF7C4DFF)
            }

            val scale = when (state) {
                "listening" -> listeningPulse
                "thinking" -> 1.0f
                "speaking" -> 1.0f + sin(speakingPhase) * 0.05f
                else -> breatheScale
            }

            val alpha = when (state) {
                "listening" -> 0.7f
                "thinking" -> 0.6f
                "speaking" -> 0.8f
                else -> breatheAlpha
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraColor.copy(alpha = alpha * 0.6f),
                        auraColor.copy(alpha = alpha * 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.5f * scale
                ),
                radius = baseRadius * 1.5f * scale,
                center = center
            )

            when (state) {
                "thinking" -> {
                    drawCircle(
                        color = Color(0xFFA78BFA).copy(alpha = 0.4f),
                        radius = baseRadius * 1.1f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    drawArc(
                        color = Color(0xFF7C4DFF),
                        startAngle = thinkingRotation,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius * 1.25f, center.y - baseRadius * 1.25f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.5f, baseRadius * 2.5f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFF8B5CF6),
                        startAngle = thinkingRotation + 180f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius * 1.25f, center.y - baseRadius * 1.25f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.5f, baseRadius * 2.5f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                "listening" -> {
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.3f),
                        radius = baseRadius * 1.3f * listeningPulse,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFF87171).copy(alpha = 0.15f),
                        radius = baseRadius * 1.6f * listeningPulse,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = baseRadius * scale,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                "speaking" -> {
                    val points = 60
                    val waveHeight = baseRadius * 0.4f
                    val wavePath = androidx.compose.ui.graphics.Path()

                    for (i in 0..points) {
                        val x = (size.width - baseRadius * 2f) / 2f + (baseRadius * 2f * i / points)
                        val relativeX = (i.toFloat() / points) * 2f - 1f
                        val envelop = sin(Math.PI * (relativeX + 1f) / 2f)
                        
                        val y = center.y + sin(relativeX * 12f + speakingPhase * 4f) * waveHeight * envelop.toFloat()

                        if (i == 0) {
                            wavePath.moveTo(x, y)
                        } else {
                            wavePath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = wavePath,
                        color = Color(0xFF10B981),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawCircle(
                        color = Color(0xFF34D399).copy(alpha = 0.4f),
                        radius = baseRadius * 1.15f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                else -> {
                    drawCircle(
                        color = Color(0xFF7C4DFF).copy(alpha = breatheAlpha * 0.5f),
                        radius = baseRadius * scale,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFA78BFA).copy(alpha = breatheAlpha * 0.25f),
                        radius = baseRadius * 1.2f * scale,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}
