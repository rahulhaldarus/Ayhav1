package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.GlassCard
import com.example.ui.components.OrbVisualizer
import com.example.ui.components.neonAmbientBackground
import com.example.viewmodel.AyhaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos

// ==========================================
// PROCEDURAL ART: CYBER AVATAR & CITY RENDERERS
// ==========================================

@Composable
fun AyhaAvatar(
    modifier: Modifier = Modifier,
    size: Int = 100,
    state: String = "idle" // idle, listening, thinking, speaking
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AyhaAvatarTransition")

    // Idle Breathing
    val breatheOffset by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathe"
    )

    // Eye Blinking Animation (blinks every 4 seconds)
    val blinkScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                1.0f at 0
                1.0f at 3800
                0.1f at 3900 // closed
                1.0f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "Blink"
    )

    // Hair sway
    val hairSway by infiniteTransition.animateFloat(
        initialValue = -1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HairSway"
    )

    // Mouth animation for speaking
    val mouthSpeak by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Speak"
    )

    // Eye glow pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EyeGlow"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .offset(y = breatheOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.width / 2f

            // 1. Holographic Glow & Ambient background
            val glowColor = when (state) {
                "listening" -> Color(0xFFC084FC) // elegant light purple for listening
                "thinking" -> Color(0xFF818CF8) // indigo
                "speaking" -> Color(0xFF34D399) // soft emerald
                else -> Color(0xFFA78BFA) // purple
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.35f), glowColor.copy(alpha = 0.08f), Color.Transparent),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )

            // Dynamic background circle (holographic glass ring)
            drawCircle(
                color = glowColor.copy(alpha = 0.15f * glowPulse),
                radius = r * 0.9f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Draw floating cyber particles
            for (i in 0 until 6) {
                val angle = (i * 60f + breatheOffset * 5f) * (Math.PI / 180f)
                val distance = r * (0.65f + 0.15f * sin(angle.toFloat()))
                val px = center.x + Math.cos(angle).toFloat() * distance
                val py = center.y + Math.sin(angle).toFloat() * distance - (breatheOffset * 1.5f)
                drawCircle(
                    color = glowColor.copy(alpha = 0.4f * glowPulse),
                    radius = 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // 2. Body / Elegant Outfit (Futuristic Elegant Black Outfit with white/silver borders)
            val bodyPath = Path().apply {
                moveTo(center.x - r * 0.5f, center.y + r * 0.4f)
                // shoulders
                quadraticTo(center.x - r * 0.7f, center.y + r * 0.65f, center.x - r * 0.55f, center.y + r * 0.9f)
                lineTo(center.x + r * 0.55f, center.y + r * 0.9f)
                quadraticTo(center.x + r * 0.7f, center.y + r * 0.65f, center.x + r * 0.5f, center.y + r * 0.4f)
                close()
            }
            // Outfit base: Deep professional black
            drawPath(path = bodyPath, color = Color(0xFF0F172A))
            // Outfit trim / silver-white borders
            drawPath(path = bodyPath, color = Color(0xFFE2E8F0), style = Stroke(width = 2.dp.toPx()))
            // Inner glowing purple cyber strip
            drawPath(path = bodyPath, color = glowColor.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))

            // Elegant white shirt collar
            val collarPath = Path().apply {
                moveTo(center.x - r * 0.2f, center.y + r * 0.42f)
                lineTo(center.x, center.y + r * 0.58f)
                lineTo(center.x + r * 0.2f, center.y + r * 0.42f)
                lineTo(center.x + r * 0.12f, center.y + r * 0.38f)
                lineTo(center.x, center.y + r * 0.44f)
                lineTo(center.x - r * 0.12f, center.y + r * 0.38f)
                close()
            }
            drawPath(path = collarPath, color = Color(0xFFF1F5F9))
            drawPath(path = collarPath, color = Color(0xFFCBD5E1), style = Stroke(width = 1.dp.toPx()))

            // Yellow Bell / Ornament on collar (from the original character!)
            drawCircle(
                color = Color(0xFFFBBF24), // Gold/yellow
                radius = r * 0.07f,
                center = Offset(center.x, center.y + r * 0.55f)
            )
            // Little ribbon behind the bell
            val ribbonL = Path().apply {
                moveTo(center.x - r * 0.04f, center.y + r * 0.55f)
                lineTo(center.x - r * 0.12f, center.y + r * 0.65f)
                lineTo(center.x - r * 0.02f, center.y + r * 0.62f)
                close()
            }
            val ribbonR = Path().apply {
                moveTo(center.x + r * 0.04f, center.y + r * 0.55f)
                lineTo(center.x + r * 0.12f, center.y + r * 0.65f)
                lineTo(center.x + r * 0.02f, center.y + r * 0.62f)
                close()
            }
            drawPath(path = ribbonL, color = Color(0xFFEF4444)) // Red bow ribbon
            drawPath(path = ribbonR, color = Color(0xFFEF4444))

            // 3. Cute face base
            drawCircle(color = Color(0xFFFFF1F2), radius = r * 0.42f, center = Offset(center.x, center.y - r * 0.04f))

            // 4. Cute Blush
            drawCircle(color = Color(0xFFFDA4AF).copy(alpha = 0.55f), radius = r * 0.07f, center = Offset(center.x - r * 0.2f, center.y + r * 0.06f))
            drawCircle(color = Color(0xFFFDA4AF).copy(alpha = 0.55f), radius = r * 0.07f, center = Offset(center.x + r * 0.2f, center.y + r * 0.06f))

            // 5. Soft Smile / Speaking animation
            val isSpeaking = state == "speaking"
            val smileY = center.y + r * 0.12f
            if (isSpeaking) {
                // Open and close mouth dynamically
                val currentOpenFactor = mouthSpeak
                drawOval(
                    color = Color(0xFFE11D48),
                    topLeft = Offset(center.x - r * 0.06f, smileY - (r * 0.04f * currentOpenFactor)),
                    size = androidx.compose.ui.geometry.Size(r * 0.12f, r * 0.08f * currentOpenFactor)
                )
                // Teeth
                drawOval(
                    color = Color.White,
                    topLeft = Offset(center.x - r * 0.04f, smileY - (r * 0.04f * currentOpenFactor)),
                    size = androidx.compose.ui.geometry.Size(r * 0.08f, r * 0.02f * currentOpenFactor)
                )
            } else {
                // Cute soft smile
                val mouth = Path().apply {
                    moveTo(center.x - r * 0.07f, smileY)
                    quadraticTo(center.x, smileY + r * 0.06f, center.x + r * 0.07f, smileY)
                }
                drawPath(path = mouth, color = Color(0xFFE11D48), style = Stroke(width = 2.dp.toPx()))
            }

            // 6. Soft Purple Glowing Eyes (stunning, glowing purple eyes from guidelines!)
            val eyeRadius = r * 0.07f
            val leftEyeCenter = Offset(center.x - r * 0.18f, center.y - r * 0.06f)
            val rightEyeCenter = Offset(center.x + r * 0.18f, center.y - r * 0.06f)

            // Draw glowing eye orbits
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA78BFA).copy(alpha = 0.7f * glowPulse), Color.Transparent),
                    center = leftEyeCenter,
                    radius = eyeRadius * 2.2f
                ),
                radius = eyeRadius * 2.2f,
                center = leftEyeCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA78BFA).copy(alpha = 0.7f * glowPulse), Color.Transparent),
                    center = rightEyeCenter,
                    radius = eyeRadius * 2.2f
                ),
                radius = eyeRadius * 2.2f,
                center = rightEyeCenter
            )

            // Draw eyes with blink scaling
            if (blinkScale > 0.15f) {
                // Outer purple iris
                drawCircle(color = Color(0xFF6D28D9), radius = eyeRadius, center = leftEyeCenter)
                drawCircle(color = Color(0xFF6D28D9), radius = eyeRadius, center = rightEyeCenter)

                // Glowing pupil (soft purple glow)
                drawCircle(color = Color(0xFFC084FC), radius = eyeRadius * 0.5f, center = leftEyeCenter)
                drawCircle(color = Color(0xFFC084FC), radius = eyeRadius * 0.5f, center = rightEyeCenter)

                // Eye highlights (shiny cute dots)
                drawCircle(color = Color.White, radius = eyeRadius * 0.22f, center = leftEyeCenter - Offset(eyeRadius * 0.3f, eyeRadius * 0.3f))
                drawCircle(color = Color.White, radius = eyeRadius * 0.22f, center = rightEyeCenter - Offset(eyeRadius * 0.3f, eyeRadius * 0.3f))
                
                // Cute upper eyelashes / eyeliner
                val lashL = Path().apply {
                    moveTo(leftEyeCenter.x - eyeRadius * 1.4f, leftEyeCenter.y - eyeRadius * 0.6f)
                    quadraticTo(leftEyeCenter.x, leftEyeCenter.y - eyeRadius * 1.1f, leftEyeCenter.x + eyeRadius * 1.3f, leftEyeCenter.y - eyeRadius * 0.4f)
                }
                val lashR = Path().apply {
                    moveTo(rightEyeCenter.x - eyeRadius * 1.3f, rightEyeCenter.y - eyeRadius * 0.4f)
                    quadraticTo(rightEyeCenter.x, rightEyeCenter.y - eyeRadius * 1.1f, rightEyeCenter.x + eyeRadius * 1.4f, rightEyeCenter.y - eyeRadius * 0.6f)
                }
                drawPath(path = lashL, color = Color(0xFF1E293B), style = Stroke(width = 2.5.dp.toPx()))
                drawPath(path = lashR, color = Color(0xFF1E293B), style = Stroke(width = 2.5.dp.toPx()))
            } else {
                // Closed/Blinking eye lines
                val closedEyeL = Path().apply {
                    moveTo(leftEyeCenter.x - eyeRadius * 1.3f, leftEyeCenter.y)
                    quadraticTo(leftEyeCenter.x, leftEyeCenter.y + eyeRadius * 0.2f, leftEyeCenter.x + eyeRadius * 1.3f, leftEyeCenter.y)
                }
                val closedEyeR = Path().apply {
                    moveTo(rightEyeCenter.x - eyeRadius * 1.3f, rightEyeCenter.y)
                    quadraticTo(rightEyeCenter.x, rightEyeCenter.y + eyeRadius * 0.2f, rightEyeCenter.x + eyeRadius * 1.3f, rightEyeCenter.y)
                }
                drawPath(path = closedEyeL, color = Color(0xFF1E293B), style = Stroke(width = 3.dp.toPx()))
                drawPath(path = closedEyeR, color = Color(0xFF1E293B), style = Stroke(width = 3.dp.toPx()))
            }

            // 7. White/Silver hair with light purple highlights (from the guidelines!)
            // Behind hair back layer
            val hairBack = Path().apply {
                moveTo(center.x - r * 0.45f, center.y - r * 0.1f)
                // Left hair locks hanging down
                quadraticTo(center.x - r * 0.52f + (hairSway * 2f), center.y + r * 0.35f, center.x - r * 0.45f + (hairSway * 3f), center.y + r * 0.65f)
                lineTo(center.x - r * 0.32f + (hairSway * 2f), center.y + r * 0.5f)
                quadraticTo(center.x - r * 0.38f, center.y + r * 0.25f, center.x - r * 0.35f, center.y - r * 0.1f)
                // Right hair locks hanging down
                moveTo(center.x + r * 0.45f, center.y - r * 0.1f)
                quadraticTo(center.x + r * 0.52f + (hairSway * 2f), center.y + r * 0.35f, center.x + r * 0.45f + (hairSway * 3f), center.y + r * 0.65f)
                lineTo(center.x + r * 0.32f + (hairSway * 2f), center.y + r * 0.5f)
                quadraticTo(center.x + r * 0.38f, center.y + r * 0.25f, center.x + r * 0.35f, center.y - r * 0.1f)
            }
            drawPath(path = hairBack, color = Color(0xFFE2E8F0)) // Silver/white back hair
            drawPath(path = hairBack, color = Color(0xFFC084FC), style = Stroke(width = 1.dp.toPx())) // Light purple highlight edges

            // Main hair head cap and cute bangs
            val hairBangs = Path().apply {
                // Head top cap
                moveTo(center.x - r * 0.45f, center.y - r * 0.1f)
                quadraticTo(center.x, center.y - r * 0.55f, center.x + r * 0.45f, center.y - r * 0.1f)
                // Right side bangs
                quadraticTo(center.x + r * 0.38f, center.y - r * 0.25f, center.x + r * 0.25f + (hairSway * 1.5f), center.y - r * 0.05f)
                // Center bangs (cute uneven look)
                quadraticTo(center.x + r * 0.1f, center.y - r * 0.18f, center.x + (hairSway * 2f), center.y)
                quadraticTo(center.x - r * 0.1f, center.y - r * 0.18f, center.x - r * 0.22f + (hairSway * 1.5f), center.y - r * 0.02f)
                // Left side bangs
                quadraticTo(center.x - r * 0.38f, center.y - r * 0.25f, center.x - r * 0.45f, center.y - r * 0.1f)
                close()
            }
            drawPath(path = hairBangs, color = Color(0xFFF1F5F9)) // Clean Silver/White main hair
            
            // Draw bangs details / shading strands
            val strand1 = Path().apply {
                moveTo(center.x, center.y - r * 0.45f)
                quadraticTo(center.x + r * 0.05f + (hairSway * 1f), center.y - r * 0.2f, center.x + r * 0.08f + (hairSway * 1f), center.y - r * 0.05f)
            }
            val strand2 = Path().apply {
                moveTo(center.x - r * 0.15f, center.y - r * 0.42f)
                quadraticTo(center.x - r * 0.2f + (hairSway * 1f), center.y - r * 0.2f, center.x - r * 0.15f + (hairSway * 1f), center.y - r * 0.02f)
            }
            drawPath(path = strand1, color = Color(0xFFC084FC), style = Stroke(width = 1.5.dp.toPx())) // Beautiful Purple highlights!
            drawPath(path = strand2, color = Color(0xFFC084FC), style = Stroke(width = 1.5.dp.toPx()))

            // Hair outline for definition
            drawPath(path = hairBangs, color = Color(0xFFCBD5E1), style = Stroke(width = 1.dp.toPx()))

            // Elegant high-tech hair clip or headband (cyber element)
            val clipPath = Path().apply {
                moveTo(center.x - r * 0.25f, center.y - r * 0.35f)
                lineTo(center.x - r * 0.18f, center.y - r * 0.38f)
                lineTo(center.x + r * 0.18f, center.y - r * 0.38f)
                lineTo(center.x + r * 0.25f, center.y - r * 0.35f)
            }
            drawPath(path = clipPath, color = glowColor, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun ProceduralCyberCity(modifier: Modifier = Modifier, colorSeed: Int = 0) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Dark sky
        drawRect(
            brush = Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))),
            size = size
        )

        // Multiple glowing futuristic buildings
        val towers = 5
        for (i in 0 until towers) {
            val tw = w / 4.5f
            val th = h * (0.35f + (0.4f * ((i * 17) % 5) / 5f))
            val tx = i * (w / 5.5f) - (tw * 0.1f)
            val ty = h - th

            drawRect(
                color = Color(0xFF020617),
                topLeft = Offset(tx, ty),
                size = androidx.compose.ui.geometry.Size(tw, th)
            )

            val glow = if ((i + colorSeed) % 2 == 0) Color(0xFF7C4DFF) else Color(0xFF06B6D4)
            drawLine(
                color = glow.copy(alpha = 0.6f),
                start = Offset(tx, ty),
                end = Offset(tx, h),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = glow.copy(alpha = 0.6f),
                start = Offset(tx + tw, ty),
                end = Offset(tx + tw, h),
                strokeWidth = 2.dp.toPx()
            )

            // Neon window dots
            for (row in 0..8) {
                for (col in 0..2) {
                    if ((row + col + i) % 3 == 0) continue
                    val wx = tx + (tw * (col + 1) / 4f)
                    val wy = ty + (th * (row + 1) / 10f)
                    drawCircle(color = glow.copy(alpha = 0.8f), radius = 1.dp.toPx(), center = Offset(wx, wy))
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(onNavigateNext: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateNext()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AyhaAvatar(size = 140)
            Spacer(modifier = Modifier.height(16.dp))
            Text("AYHA", style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp))
            Text("Personal AI Companion", style = TextStyle(fontSize = 15.sp, color = Color(0xFFA78BFA)))
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = Color(0xFF7C4DFF), modifier = Modifier.size(32.dp))
        }
    }
}

// ==========================================
// 2. WELCOME SCREEN
// ==========================================
@Composable
fun WelcomeScreen(onNavigateNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                Text("Welcome to", style = TextStyle(fontSize = 16.sp, color = Color(0xFFA78BFA)))
                Text("AYHA", style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 3.sp))
                Text("Personal AI Companion", style = TextStyle(fontSize = 14.sp, color = Color(0xFF94A3B8)))
            }

            Box(contentAlignment = Alignment.Center) {
                OrbVisualizer(state = "idle")
                AyhaAvatar(size = 130)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                Text("I'm here to assist you\nanytime, anywhere.", textAlign = TextAlign.Center, style = TextStyle(fontSize = 15.sp, color = Color(0xFF94A3B8), lineHeight = 22.sp))
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("get_started_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Get Started", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
            }
        }
    }
}

// ==========================================
// 3. PERMISSION SCREEN
// ==========================================
@Composable
fun PermissionScreen(onNavigateNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Permission", style = TextStyle(fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F7C4DFF))
                        .border(1.dp, Color(0xFF7C4DFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Mic", tint = Color(0xFF7C4DFF), modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("To provide the best voice experience, AYHA needs access to your microphone.", textAlign = TextAlign.Center, style = TextStyle(fontSize = 15.sp, color = Color(0xFF94A3B8), lineHeight = 24.sp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("allow_microphone_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Allow Microphone", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onNavigateNext) {
                    Text("Not Now", style = TextStyle(color = Color(0xFF94A3B8)))
                }
            }
        }
    }
}

// ==========================================
// 4. MAIN HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(
    viewModel: AyhaViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val suggestionText by viewModel.voiceSuggestion.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
                Text("AYHA", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp))
                IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
            }

            // Greet
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Good Evening,", style = TextStyle(fontSize = 16.sp, color = Color(0xFFA78BFA)))
                Text("Mr.Rahul", style = TextStyle(fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.ExtraBold))
                Spacer(modifier = Modifier.height(12.dp))
                Text(suggestionText, textAlign = TextAlign.Center, style = TextStyle(fontSize = 14.sp, color = Color(0xFF94A3B8), lineHeight = 20.sp), modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Orb Visualizer
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clickable { onNavigateToVoice() },
                contentAlignment = Alignment.Center
            ) {
                OrbVisualizer(state = voiceState)
            }

            // Controls launcher bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToChat,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F7C4DFF))
                ) {
                    Icon(Icons.Default.Keyboard, "Keyboard Chat", tint = Color(0xFFA78BFA))
                }

                Button(
                    onClick = {
                        if (voiceState == "idle") {
                            viewModel.startVoiceListening()
                        } else if (voiceState == "listening") {
                            viewModel.stopVoiceAndSubmit("What's the weather today?")
                        } else {
                            viewModel.stopSpeaking()
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("microphone_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (voiceState == "listening") Color(0xFFEF4444) else Color(0xFF7C4DFF)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (voiceState == "listening") Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mic Trigger",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = onNavigateToDashboard,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F7C4DFF))
                ) {
                    Icon(Icons.Default.GridView, "Dashboard Grid", tint = Color(0xFFA78BFA))
                }
            }
        }
    }
}

// ==========================================
// 5. DRAWER CONTENT
// ==========================================
@Composable
fun DrawerContent(
    activeRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val items = listOf(
        Pair("home", Pair(Icons.Default.Home, "Home")),
        Pair("chat", Pair(Icons.Default.Chat, "AI Chat")),
        Pair("voice", Pair(Icons.Default.Mic, "Voice Assistant")),
        Pair("image_gen", Pair(Icons.Default.Image, "Image Generator")),
        Pair("translator", Pair(Icons.Default.Translate, "Translator")),
        Pair("notes", Pair(Icons.Default.List, "Notes")),
        Pair("reminders", Pair(Icons.Default.CalendarToday, "Reminders")),
        Pair("history", Pair(Icons.Default.History, "History")),
        Pair("settings", Pair(Icons.Default.Settings, "Settings")),
        Pair("about", Pair(Icons.Default.Info, "About AYHA"))
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color(0xFF0F172A))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AyhaAvatar(size = 80)
            Spacer(modifier = Modifier.height(12.dp))
            Text("AYHA", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
            Text("Personal AI Companion", style = TextStyle(fontSize = 12.sp, color = Color(0xFFA78BFA)))
        }

        Divider(color = Color(0x1F7C4DFF), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(items) { (route, meta) ->
                val (icon, name) = meta
                val selected = activeRoute == route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color(0x2A7C4DFF) else Color.Transparent)
                        .clickable {
                            onNavigate(route)
                            onCloseDrawer()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, name, tint = if (selected) Color(0xFF7C4DFF) else Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(name, style = TextStyle(fontSize = 14.sp, color = if (selected) Color.White else Color(0xFF94A3B8)))
                }
            }
        }
    }
}

// ==========================================
// 6. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("chat", Icons.Default.Chat, Pair("AI Chat", "Smart conversation")),
        Triple("voice", Icons.Default.Mic, Pair("Voice Assistant", "Voice commands")),
        Triple("image_gen", Icons.Default.Image, Pair("Image Generator", "Create with AI")),
        Triple("translator", Icons.Default.Translate, Pair("Translator", "Translate anything")),
        Triple("notes", Icons.Default.List, Pair("Notes", "Save your thoughts")),
        Triple("reminders", Icons.Default.CalendarToday, Pair("Reminders", "Set reminders"))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(12.dp))
                Text("AYHA Dashboard", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }

            Text("Hello, Mr.Rahul", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White))
            Text("What would you like to explore today?", style = TextStyle(fontSize = 14.sp, color = Color(0xFF94A3B8)))
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { (route, icon, meta) ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(route) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1A7C4DFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, "Icon", tint = Color(0xFF7C4DFF))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(meta.first, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                                Text(meta.second, style = TextStyle(color = Color(0xFF94A3B8), fontSize = 12.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    speedMillis: Long = 15,
    onAnimationComplete: () -> Unit = {}
) {
    var textToDisplay by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        textToDisplay = ""
        for (i in 1..text.length) {
            textToDisplay = text.substring(0, i)
            delay(speedMillis)
        }
        onAnimationComplete()
    }
    Text(
        text = textToDisplay,
        style = style,
        modifier = modifier
    )
}

// ==========================================
// 7. CHAT SCREEN
// ==========================================
@Composable
fun ChatScreen(viewModel: AyhaViewModel, onBack: () -> Unit) {
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val animatedMessageIds = remember { mutableStateOf(setOf<Int>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Chat", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
                IconButton(onClick = { viewModel.selectOrCreateSession(null) }) {
                    Icon(Icons.Default.AddComment, "New Chat", tint = Color(0xFFA78BFA))
                }
            }

            if (activeSessionId == null) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AyhaAvatar(size = 110)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No active chat sessions with AYHA", color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.selectOrCreateSession(null) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))) {
                        Text("Start Conversation")
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Recent Sessions", style = TextStyle(color = Color(0xFFA78BFA), fontSize = 13.sp, fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(sessions) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x1F1E293B))
                                    .clickable { viewModel.selectOrCreateSession(s.id) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Message, "Msg", tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(s.title, color = Color.White)
                                }
                                IconButton(onClick = { viewModel.deleteSession(s.id) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            } else {
                // Messages list logs
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(messages) { m ->
                        val isUser = m.role == "user"
                        val isLastMessage = m.id == messages.lastOrNull()?.id
                        val shouldAnimate = !isUser && isLastMessage && !animatedMessageIds.value.contains(m.id)

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            if (!isUser) {
                                AyhaAvatar(
                                    size = 40,
                                    state = if (isLastMessage && voiceState == "speaking") "speaking" else "idle",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            GlassCard(
                                modifier = Modifier.widthIn(max = 260.dp),
                                cornerRadius = 16.dp,
                                borderWidth = if (isUser) 1.dp else 0.dp,
                                borderColor = Color(0xFF7C4DFF),
                                backgroundColor = if (isUser) Color(0x337C4DFF) else Color(0x1F1E293B)
                            ) {
                                Column {
                                    Text(if (isUser) "You" else "AYHA", style = TextStyle(fontSize = 11.sp, color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (shouldAnimate) {
                                        TypewriterText(
                                            text = m.content,
                                            style = TextStyle(fontSize = 14.sp, color = Color.White),
                                            onAnimationComplete = {
                                                animatedMessageIds.value = animatedMessageIds.value + m.id
                                            }
                                        )
                                    } else {
                                        Text(m.content, style = TextStyle(fontSize = 14.sp, color = Color.White))
                                    }
                                }
                            }
                        }
                    }

                    // Loading/thinking/streaming UI
                    if (voiceState == "thinking") {
                        item {
                            val infiniteTransition = rememberInfiniteTransition(label = "ThinkingGlow")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 0.9f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "PulseAlpha"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                AyhaAvatar(
                                    size = 40,
                                    state = "thinking",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                GlassCard(
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .border(
                                            width = 1.5.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF7C4DFF).copy(alpha = pulseAlpha),
                                                    Color(0xFF06B6D4).copy(alpha = pulseAlpha * 0.5f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    cornerRadius = 16.dp,
                                    backgroundColor = Color(0x221E293B)
                                ) {
                                    Column(modifier = Modifier.padding(2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "AYHA",
                                                style = TextStyle(
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFA78BFA),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            // Glowing cyber pulse dot
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        color = Color(0xFF06B6D4).copy(alpha = pulseAlpha),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Analyzing neural pathways",
                                                style = TextStyle(fontSize = 13.sp, color = Color(0xFF94A3B8))
                                            )
                                            
                                            // 3 pulsing dots
                                            repeat(3) { index ->
                                                val dotAlpha by infiniteTransition.animateFloat(
                                                    initialValue = 0.2f,
                                                    targetValue = 1.0f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(
                                                            durationMillis = 600,
                                                            delayMillis = index * 200,
                                                            easing = LinearEasing
                                                        ),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "DotAlpha_$index"
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(
                                                            color = Color(0xFF7C4DFF).copy(alpha = dotAlpha),
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (streamingText.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                AyhaAvatar(
                                    size = 40,
                                    state = "speaking",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                GlassCard(modifier = Modifier.widthIn(max = 260.dp)) {
                                    Column {
                                        Text("AYHA", style = TextStyle(fontSize = 11.sp, color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(streamingText, style = TextStyle(fontSize = 14.sp, color = Color.White))
                                    }
                                }
                            }
                        }
                    }
                }

                // Input row bar
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).testTag("chat_input_field"),
                        placeholder = { Text("Type a message...", color = Color(0xFF94A3B8)) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF7C4DFF))
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. VOICE ASSISTANT SCREEN
// ==========================================
@Composable
fun VoiceAssistantScreen(viewModel: AyhaViewModel, onBack: () -> Unit) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val suggestion by viewModel.voiceSuggestion.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Voice Assistant", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // AYHA holographic assistant above the Orb animating together
                AyhaAvatar(
                    size = 145,
                    state = voiceState,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OrbVisualizer(state = voiceState)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when (voiceState) {
                        "listening" -> "I'm Listening..."
                        "thinking" -> "Analyzing neural patterns..."
                        "speaking" -> "Speaking..."
                        else -> "Idle"
                    },
                    style = TextStyle(fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(suggestion, textAlign = TextAlign.Center, style = TextStyle(color = Color(0xFFA78BFA), fontSize = 14.sp), modifier = Modifier.padding(horizontal = 24.dp))
            }

            Button(
                onClick = {
                    if (voiceState == "listening") {
                        viewModel.stopVoiceAndSubmit("Hello AYHA, how are you today?")
                    } else {
                        viewModel.startVoiceListening()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (voiceState == "listening") Color(0xFFEF4444) else Color(0xFF7C4DFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (voiceState == "listening") "Stop Listening" else "Speak Now", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ==========================================
// 9. IMAGE GENERATOR SCREEN
// ==========================================
@Composable
fun ImageGeneratorScreen(onBack: () -> Unit) {
    var prompt by remember { mutableStateOf("A futuristic city at night with neon lights") }
    var isGenerating by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Image Generator", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)) {
                Text("Describe your creative vision", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isGenerating) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1F1E293B)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF7C4DFF))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("AYHA is generating your artwork...", color = Color(0xFFA78BFA))
                        }
                    }
                } else {
                    ProceduralCyberCity(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)), colorSeed = seed)
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isGenerating = true
                        delay(1500)
                        isGenerating = false
                        seed += 1
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Generate AI Image", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ==========================================
// 10. TRANSLATOR SCREEN
// ==========================================
@Composable
fun TranslatorScreen(onBack: () -> Unit) {
    var sourceText by remember { mutableStateOf("How are you?") }
    var translatedText by remember { mutableStateOf("আপনি কেমন আছেন?") }
    var translating by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Translator", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Text("English", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.SwapHoriz, "Swap", tint = Color(0xFF7C4DFF))
                Text("Bengali", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                GlassCard(modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp)) {
                    Column {
                        Text("English Source", color = Color(0xFFA78BFA), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = sourceText,
                            onValueChange = { sourceText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bengali Translation", color = Color(0xFFA78BFA), fontSize = 11.sp)
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(translatedText))
                                Toast.makeText(context, "Translation copied!", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (translating) {
                            CircularProgressIndicator(color = Color(0xFF7C4DFF))
                        } else {
                            Text(translatedText, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        translating = true
                        delay(1000)
                        translating = false
                        translatedText = if (sourceText.contains("how", true)) "আপনি কেমন আছেন?" else "আমি খুব ভালো আছি, ধন্যবাদ Mr.Rahul!"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp, bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Translate Now", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ==========================================
// 10. NOTES SCREEN
// ==========================================
@Composable
fun NotesScreen(viewModel: AyhaViewModel, onBack: () -> Unit) {
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Notes Database", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add", tint = Color(0xFF7C4DFF)) }
            }

            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("Search notes...", color = Color(0xFF94A3B8)) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notes) { note ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(note.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (note.isPinned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.PushPin, "Pinned", tint = Color(0xFF7C4DFF), modifier = Modifier.size(12.dp))
                                    }
                                }
                                Text(note.folder, color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(note.content, color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            Row {
                                IconButton(onClick = { viewModel.togglePinNote(note) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.PushPin, "Pin", tint = if (note.isPinned) Color(0xFF7C4DFF) else Color(0xFF94A3B8))
                                }
                                IconButton(onClick = { viewModel.deleteNote(note) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                containerColor = Color(0xFF1E293B),
                title = { Text("Create Note", color = Color.White) },
                text = {
                    Column {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addNote(title, content, "Work")
                            title = ""
                            content = ""
                            showAdd = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))) {
                        Text("Save")
                    }
                }
            )
        }
    }
}

// ==========================================
// 11. REMINDERS SCREEN
// ==========================================
@Composable
fun RemindersScreen(viewModel: AyhaViewModel, onBack: () -> Unit) {
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var rTitle by remember { mutableStateOf("") }
    var rTime by remember { mutableStateOf("09:00 AM") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reminders Alerts", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(reminders) { item ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(item.title, color = if (item.isEnabled) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("${item.dateText}, ${item.timeText}", color = Color(0xFFA78BFA), fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(checked = item.isEnabled, onCheckedChange = { viewModel.toggleReminder(item) })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.deleteReminder(item) }) {
                                        Icon(Icons.Default.Delete, "Del", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+ Add Reminder", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                containerColor = Color(0xFF1E293B),
                title = { Text("Add Reminder", color = Color.White) },
                text = {
                    Column {
                        OutlinedTextField(value = rTitle, onValueChange = { rTitle = it }, label = { Text("Name") })
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = rTime, onValueChange = { rTime = it }, label = { Text("Time (e.g., 09:00 AM)") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (rTitle.isNotBlank()) {
                            viewModel.addReminder(rTitle, "Today", rTime, "None")
                            rTitle = ""
                            showAdd = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))) {
                        Text("Save")
                    }
                }
            )
        }
    }
}

// ==========================================
// 12. HISTORY SCREEN
// ==========================================
@Composable
fun HistoryScreen(viewModel: AyhaViewModel, onBack: () -> Unit) {
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Chat History", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
                TextButton(onClick = { viewModel.clearAllChatHistory() }) { Text("Clear All", color = Color(0xFFEF4444)) }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1F1E293B))
                            .clickable { viewModel.selectOrCreateSession(s.id) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(s.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(s.lastMessage.take(30) + "...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, "View", tint = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

// ==========================================
// 13. SETTINGS / PROVIDER SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: AyhaViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val providers by viewModel.allProviders.collectAsStateWithLifecycle()
    val activeProviderId by viewModel.activeProviderId.collectAsStateWithLifecycle()
    val voice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val language by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    // Hands-Free Service state
    val isServiceRunning by AyhaVoiceService.isServiceRunning.collectAsStateWithLifecycle()
    val assistantState by AyhaVoiceService.assistantState.collectAsStateWithLifecycle()
    val lastSpokenText by AyhaVoiceService.lastSpokenText.collectAsStateWithLifecycle()

    // Voice Manager instance for settings storage
    val voiceManager = remember { VoiceManager.getInstance(context) }
    var activeVoiceEngine by remember { mutableStateOf(voiceManager.getActiveEngine()) }

    var elKey by remember { mutableStateOf(voiceManager.getElevenLabsApiKey()) }
    var elVoice by remember { mutableStateOf(voiceManager.getElevenLabsVoiceId()) }

    var oaiKey by remember { mutableStateOf(voiceManager.getOpenAiApiKey()) }
    var oaiVoice by remember { mutableStateOf(voiceManager.getOpenAiVoice()) }

    var gKey by remember { mutableStateOf(voiceManager.getGoogleCloudApiKey()) }
    var gVoice by remember { mutableStateOf(voiceManager.getGoogleCloudVoiceName()) }

    var azKey by remember { mutableStateOf(voiceManager.getAzureApiKey()) }
    var azRegion by remember { mutableStateOf(voiceManager.getAzureRegion()) }
    var azVoice by remember { mutableStateOf(voiceManager.getAzureVoiceName()) }

    var showConfigProvider by remember { mutableStateOf<AiProvider?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Settings Config",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // --- CATEGORY 1: HANDS-FREE ASSISTANT SERVICE ---
                item {
                    Text(
                        "Hands-Free Assistant",
                        color = Color(0xFFA78BFA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x1F7C4DFF)),
                        border = BorderStroke(1.dp, Color(0x337C4DFF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Voice Wake-Up Service",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "Runs in background listening for 'Hey AYHA'",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = isServiceRunning,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            AyhaVoiceService.startService(context)
                                            Toast.makeText(context, "AYHA Hands-Free Assistant Activated", Toast.LENGTH_SHORT).show()
                                        } else {
                                            AyhaVoiceService.stopService(context)
                                            Toast.makeText(context, "AYHA Hands-Free Assistant Stopped", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF8B5CF6),
                                        checkedTrackColor = Color(0xFF312E81)
                                    )
                                )
                            }

                            if (isServiceRunning) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0x1AFFFFFF))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Service Status:", color = Color(0xFFA78BFA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    val statusLabel = when (assistantState) {
                                        "waiting_wake" -> "Waiting for 'Hey AYHA'..."
                                        "listening_query" -> "Listening to Command..."
                                        "thinking" -> "Generating response..."
                                        "speaking" -> "Speaking reply..."
                                        else -> "Ready"
                                    }
                                    Text(
                                        statusLabel,
                                        color = if (assistantState == "waiting_wake") Color(0xFF10B981) else Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (lastSpokenText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Heard: \"$lastSpokenText\"",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }

                // --- CATEGORY 2: MODULAR VOICE SYNTHESIS ENGINE ---
                item {
                    Text(
                        "Voice Synthesis Engine",
                        color = Color(0xFFA78BFA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x127C4DFF))
                            .border(BorderStroke(1.dp, Color(0x1A7C4DFF)), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Active Voice Engine Provider", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        // Select Voice Engine Row
                        val engines = listOf("device" to "Device Fallback TTS", "elevenlabs" to "ElevenLabs AI Voice", "openai" to "OpenAI TTS", "google" to "Google Cloud TTS", "azure" to "Azure Cognitive Speech")
                        engines.forEach { (id, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeVoiceEngine == id) Color(0x337C4DFF) else Color.Transparent)
                                    .clickable {
                                        activeVoiceEngine = id
                                        voiceManager.setActiveEngine(id)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = if (activeVoiceEngine == id) Color.White else Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = if (activeVoiceEngine == id) FontWeight.Bold else FontWeight.Normal)
                                if (activeVoiceEngine == id) {
                                    Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0x1AFFFFFF))
                        Spacer(modifier = Modifier.height(4.dp))

                        // Dynamic Input Fields depending on engine selection
                        when (activeVoiceEngine) {
                            "elevenlabs" -> {
                                Text("ElevenLabs Credentials", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = elKey,
                                    onValueChange = { elKey = it; voiceManager.setElevenLabsApiKey(it) },
                                    label = { Text("ElevenLabs API Key", color = Color(0xFF94A3B8)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = elVoice,
                                    onValueChange = { elVoice = it; voiceManager.setElevenLabsVoiceId(it) },
                                    label = { Text("Voice ID", color = Color(0xFF94A3B8)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "openai" -> {
                                Text("OpenAI Speech Credentials", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = oaiKey,
                                    onValueChange = { oaiKey = it; voiceManager.setOpenAiApiKey(it) },
                                    label = { Text("OpenAI API Key", color = Color(0xFF94A3B8)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = oaiVoice,
                                    onValueChange = { oaiVoice = it; voiceManager.setOpenAiVoice(it) },
                                    label = { Text("Voice Name (alloy, echo, fable, onyx, nova, shimmer)", color = Color(0xFF94A3B8)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "google" -> {
                                Text("Google Cloud Speech Credentials", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = gKey,
                                    onValueChange = { gKey = it; voiceManager.setGoogleCloudApiKey(it) },
                                    label = { Text("Google Cloud API Key", color = Color(0xFF94A3B8)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = gVoice,
                                    onValueChange = { gVoice = it; voiceManager.setGoogleCloudVoiceName(it) },
                                    label = { Text("Voice Name (e.g. en-US-Neural2-F)", color = Color(0xFF94A3B8)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "azure" -> {
                                Text("Microsoft Azure Speech Credentials", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = azKey,
                                    onValueChange = { azKey = it; voiceManager.setAzureApiKey(it) },
                                    label = { Text("Azure Subscription Key", color = Color(0xFF94A3B8)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = azRegion,
                                    onValueChange = { azRegion = it; voiceManager.setAzureRegion(it) },
                                    label = { Text("Azure Region (e.g. eastus)", color = Color(0xFF94A3B8)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = azVoice,
                                    onValueChange = { azVoice = it; voiceManager.setAzureVoiceName(it) },
                                    label = { Text("Voice Name (e.g. en-US-JennyNeural)", color = Color(0xFF94A3B8)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {
                                Text(
                                    "No extra keys required for device fallback. Text-to-speech uses your phone's built-in Google TTS engines.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    style = TextStyle(lineHeight = 16.sp)
                                )
                            }
                        }
                    }
                }

                // --- CATEGORY 3: MULTI-AI PROVIDER MANAGER ---
                item {
                    Text(
                        "Multi-AI Provider Settings",
                        color = Color(0xFFA78BFA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        providers.forEach { provider ->
                            val active = provider.id == activeProviderId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) Color(0x2A7C4DFF) else Color(0x127C4DFF))
                                    .clickable { viewModel.selectProvider(provider.id) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        provider.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "Model: ${provider.selectedModel.ifBlank { "Default" }} | Temp: ${provider.temperature} | Timeout: ${provider.timeout}s",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showConfigProvider = provider }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            "Edit",
                                            tint = Color(0xFFA78BFA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    if (active) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            "Active",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG FOR CONFIGURING A PROVIDER ---
        showConfigProvider?.let { p ->
            var key by remember { mutableStateOf(p.apiKey) }
            var base by remember { mutableStateOf(p.baseUrl) }
            var modelName by remember { mutableStateOf(p.selectedModel) }
            var temp by remember { mutableStateOf(p.temperature) }
            var topP by remember { mutableStateOf(p.topP) }
            var topK by remember { mutableStateOf(p.topK) }
            var maxTokensStr by remember { mutableStateOf(p.maxTokens.toString()) }
            var stream by remember { mutableStateOf(p.isStreaming) }
            var timeoutStr by remember { mutableStateOf(p.timeout.toString()) }

            AlertDialog(
                onDismissRequest = { showConfigProvider = null },
                containerColor = Color(0xFF1E293B),
                title = { Text("Configure ${p.name}", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.heightIn(max = 420.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = key,
                                onValueChange = { key = it },
                                label = { Text("API Key", color = Color(0xFF94A3B8)) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = base,
                                onValueChange = { base = it },
                                label = { Text("Base URL (Leave blank for default)", color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = modelName,
                                onValueChange = { modelName = it },
                                label = { Text("Model Name (Leave blank for default)", color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Temperature: ${String.format("%.2f", temp)}", color = Color.White, fontSize = 12.sp)
                                    Text("Creativity", color = Color(0xFFA78BFA), fontSize = 11.sp)
                                }
                                Slider(
                                    value = temp,
                                    onValueChange = { temp = it },
                                    valueRange = 0.0f..1.5f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF7C4DFF), activeTrackColor = Color(0xFF7C4DFF))
                                )
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Top P: ${String.format("%.2f", topP)}", color = Color.White, fontSize = 12.sp)
                                    Text("Nucleus Sampling", color = Color(0xFFA78BFA), fontSize = 11.sp)
                                }
                                Slider(
                                    value = topP,
                                    onValueChange = { topP = it },
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF7C4DFF), activeTrackColor = Color(0xFF7C4DFF))
                                )
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Top K: $topK", color = Color.White, fontSize = 12.sp)
                                }
                                Slider(
                                    value = topK.toFloat(),
                                    onValueChange = { topK = it.toInt() },
                                    valueRange = 1.0f..100.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF7C4DFF), activeTrackColor = Color(0xFF7C4DFF))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = maxTokensStr,
                                    onValueChange = { maxTokensStr = it },
                                    label = { Text("Max Tokens", color = Color(0xFF94A3B8)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = timeoutStr,
                                    onValueChange = { timeoutStr = it },
                                    label = { Text("Timeout (sec)", color = Color(0xFF94A3B8)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Streaming", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = stream,
                                    onCheckedChange = { stream = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF7C4DFF))
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val maxTokens = maxTokensStr.toIntOrNull() ?: 2048
                            val timeout = timeoutStr.toIntOrNull() ?: 30
                            viewModel.updateProvider(
                                p.copy(
                                    apiKey = key,
                                    baseUrl = base,
                                    selectedModel = modelName,
                                    temperature = temp,
                                    topP = topP,
                                    topK = topK,
                                    maxTokens = maxTokens,
                                    isStreaming = stream,
                                    timeout = timeout
                                )
                            )
                            showConfigProvider = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfigProvider = null }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

// ==========================================
// 14. ABOUT SCREEN
// ==========================================
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground(),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(12.dp))
                Text("About Companion", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AyhaAvatar(size = 130)
                Spacer(modifier = Modifier.height(16.dp))
                Text("AYHA", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp))
                Text("Version 1.0.0", color = Color(0xFFA78BFA), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("AYHA is an emotionally intelligent, cute, caring AI female companion built specifically for Mr.Rahul. Equipped with a custom voice engine, SQLite persistence, and direct Gemini integration.", textAlign = TextAlign.Center, color = Color(0xFF94A3B8), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }

            Text("Made with ❤️ for Mr.Rahul", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

// ==========================================
// 15. TAP TO WAKE / LOCK SCREEN
// ==========================================
@Composable
fun TapToWakeScreen(onWake: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .neonAmbientBackground()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onWake() },
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text("AYHA", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 3.sp), modifier = Modifier.padding(top = 40.dp))
            Box(contentAlignment = Alignment.Center) {
                OrbVisualizer(state = "idle")
                AyhaAvatar(size = 130)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 48.dp)) {
                Icon(Icons.Default.LockOpen, "Lock", tint = Color(0xFFA78BFA), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap to Wake", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
    }
}
