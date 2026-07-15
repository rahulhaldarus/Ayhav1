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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbVisualizer(
    state: String, // idle, listening, thinking, speaking
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    // Idle Breathing Scale
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheScale"
    )
    
    // Core Pulse Alpha
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheAlpha"
    )

    // Thinking Rotation (Computational Loading)
    val thinkingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThinkingRotation"
    )

    // Outer Ring Rotation (Sci-fi Details)
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    // Listening Quick Pulse
    val listeningPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListeningPulse"
    )

    // Speaking Audio Wave Phase
    val speakingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
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
            val baseRadius = size.width * 0.28f

            // Color scheme based on state
            val coreColor = when (state) {
                "listening" -> Color(0xFFEF4444) // Neon Red
                "thinking" -> Color(0xFF3B82F6)  // Tech Blue
                "speaking" -> Color(0xFF10B981)  // Matrix Green
                else -> Color(0xFF7C4DFF)        // AYHA Purple
            }

            val secondaryColor = when (state) {
                "listening" -> Color(0xFFF87171)
                "thinking" -> Color(0xFF60A5FA)
                "speaking" -> Color(0xFF34D399)
                else -> Color(0xFFA78BFA)
            }

            val accentColor = when (state) {
                "listening" -> Color(0xFFFCA5A5)
                "thinking" -> Color(0xFF93C5FD)
                "speaking" -> Color(0xFF6EE7B7)
                else -> Color(0xFFC084FC)
            }

            // Scale & Alpha multipliers based on state
            val scale = when (state) {
                "listening" -> listeningPulse
                "thinking" -> 1.0f
                "speaking" -> 1.0f + sin(speakingPhase) * 0.04f
                else -> breatheScale
            }

            val alpha = when (state) {
                "listening" -> 0.85f
                "thinking" -> 0.70f
                "speaking" -> 0.90f
                else -> breatheAlpha
            }

            // 1. Draw Outer Ambient Glow Layer (Glassmorphic Backdrop Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = alpha * 0.5f),
                        secondaryColor.copy(alpha = alpha * 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.2f * scale
                ),
                radius = baseRadius * 2.2f * scale,
                center = center
            )

            // 2. Draw Outer Sci-Fi Rotating Energy Rings with Tic-Marks
            val ringRadius = baseRadius * 1.55f * (if (state == "listening") listeningPulse else 1.0f)
            val dashPattern = PathEffect.dashPathEffect(floatArrayOf(15f, 35f), 0f)
            val dotPattern = PathEffect.dashPathEffect(floatArrayOf(4f, 16f), 0f)

            // Clockwise Rotating Ring (Solid/Dash combo)
            drawArc(
                color = secondaryColor.copy(alpha = 0.3f),
                startAngle = ringRotation,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashPattern)
            )

            // Counter-Clockwise Rotating Ring (Dotted Outer Halo)
            drawArc(
                color = accentColor.copy(alpha = 0.45f),
                startAngle = -ringRotation * 1.2f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius * 1.15f, center.y - ringRadius * 1.15f),
                size = Size(ringRadius * 1.15f * 2f, ringRadius * 1.15f * 2f),
                style = Stroke(width = 2.dp.toPx(), pathEffect = dotPattern)
            )

            // 3. Draw State-Specific Components
            when (state) {
                "listening" -> {
                    // Outer expanding ripple rings
                    val rippleRadius = baseRadius * 1.35f * listeningPulse
                    drawCircle(
                        color = coreColor.copy(alpha = 0.2f),
                        radius = rippleRadius * 1.2f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.12f),
                        radius = rippleRadius * 1.4f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Core glowing orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                coreColor,
                                secondaryColor.copy(alpha = 0.7f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 0.95f
                        ),
                        radius = baseRadius * 0.95f,
                        center = center
                    )
                }
                "thinking" -> {
                    // Counter-rotating computation halos inside the orb
                    val thinkRadius = baseRadius * 1.25f
                    
                    drawArc(
                        color = coreColor,
                        startAngle = thinkingRotation,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(center.x - thinkRadius, center.y - thinkRadius),
                        size = Size(thinkRadius * 2f, thinkRadius * 2f),
                        style = Stroke(width = 3.5.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(4f))
                    )
                    
                    drawArc(
                        color = accentColor,
                        startAngle = thinkingRotation + 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(center.x - thinkRadius, center.y - thinkRadius),
                        size = Size(thinkRadius * 2f, thinkRadius * 2f),
                        style = Stroke(width = 3.5.dp.toPx())
                    )

                    // Inner spinning core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                coreColor.copy(alpha = 0.9f),
                                secondaryColor.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 0.85f
                        ),
                        radius = baseRadius * 0.85f,
                        center = center
                    )
                }
                "speaking" -> {
                    // Dynamic Triple Sine Wave Visualizer (Siri / Gemini style)
                    val wavePoints = 80
                    val waveWidth = baseRadius * 2.3f
                    val startX = center.x - waveWidth / 2f

                    // We draw three overlapping colored waveforms with phase shifts and different amplitudes
                    val waveConfigs = listOf(
                        Triple(coreColor, 0.45f, 0f),                  // Primary
                        Triple(secondaryColor.copy(alpha = 0.8f), 0.35f, 1.5f), // Shifted secondary
                        Triple(accentColor.copy(alpha = 0.6f), 0.25f, 3.0f)   // Accent high frequency
                    )

                    waveConfigs.forEach { (wColor, ampFactor, phaseShift) ->
                        val path = Path()
                        val maxAmplitude = baseRadius * ampFactor * (0.6f + 0.4f * sin(speakingPhase * 3f))

                        for (i in 0..wavePoints) {
                            val fraction = i.toFloat() / wavePoints
                            val x = startX + fraction * waveWidth
                            
                            // Envelope function to pinch waves at the left/right edges (sine envelope)
                            val envelope = sin(fraction * Math.PI).toFloat()
                            
                            // Sine wave equation with phase shifts
                            val angle = (fraction * 4.5f * Math.PI + speakingPhase * 4.5f + phaseShift).toFloat()
                            val y = center.y + sin(angle) * maxAmplitude * envelope

                            if (i == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = wColor,
                            style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(8f))
                        )
                    }

                    // Soft pulsing core behind the wave
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                coreColor.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 0.8f
                        ),
                        radius = baseRadius * 0.8f,
                        center = center
                    )
                }
                else -> {
                    // Idle Breathing State - Core glowing fluid gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                coreColor.copy(alpha = alpha),
                                secondaryColor.copy(alpha = alpha * 0.6f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * scale
                        ),
                        radius = baseRadius * scale,
                        center = center
                    )

                    // Inner bright white/magenta core highlight
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = alpha * 0.9f),
                                accentColor.copy(alpha = alpha * 0.3f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 0.5f * scale
                        ),
                        radius = baseRadius * 0.5f * scale,
                        center = center
                    )
                }
            }

            // 4. Procedural Floating Futuristic Particles (Drifting & Shimmering)
            val particleCount = 10
            for (i in 0 until particleCount) {
                // Calculate position using sin/cos based on particle index and animation phase
                val particlePhase = speakingPhase * 0.5f + (i * 123.45f)
                val angle = (i * (360f / particleCount) + (thinkingRotation * 0.4f)) * (Math.PI / 180f)
                
                // Drifting distance that expands and contracts
                val driftFactor = 1.1f + 0.35f * sin(particlePhase)
                val distance = baseRadius * driftFactor
                
                val pX = center.x + cos(angle).toFloat() * distance
                val pY = center.y + sin(angle).toFloat() * distance
                
                // Shimmering alpha and scale
                val pAlpha = ((0.3f + 0.5f * sin(particlePhase * 2f)) * alpha).coerceIn(0f, 1f)
                val pSize = 2.5.dp.toPx() * (0.6f + 0.4f * cos(particlePhase))

                drawCircle(
                    color = accentColor.copy(alpha = pAlpha),
                    radius = pSize,
                    center = Offset(pX, pY)
                )
            }
        }
    }
}
