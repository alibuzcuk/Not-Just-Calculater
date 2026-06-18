package com.example

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CalcTheme
import com.example.ui.theme.ThemeStyle

/**
 * Sparkle and particle generator view that draws flying elements at 60fps.
 */
@Composable
fun SparkleCanvas(
    particles: List<Particle>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val alphaColor = particle.color.copy(alpha = particle.alpha)
            
            withTransform({
                translate(particle.x, particle.y)
                rotate(particle.rotation)
            }) {
                if (particle.isEmoji) {
                    drawContext.canvas.nativeCanvas.drawText(
                        particle.emoji,
                        0f,
                        0f,
                        android.graphics.Paint().apply {
                            textSize = particle.size * density
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                } else if (particle.isStar) {
                    drawStar(size = particle.size, color = alphaColor)
                } else {
                    drawCircle(
                        color = alphaColor,
                        radius = particle.size / 2f,
                        center = Offset.Zero
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStar(size: Float, color: Color) {
    val path = Path().apply {
        val center = 0f
        val numPoints = 5
        val outerRadius = size / 2f
        val innerRadius = size / 5f
        var angle = Math.PI / 2.0 * 3.0
        val step = Math.PI / numPoints

        moveTo(
            (center + outerRadius * Math.cos(angle)).toFloat(),
            (center + outerRadius * Math.sin(angle)).toFloat()
        )
        for (i in 0 until numPoints * 2) {
            val r = if (i % 2 == 0) innerRadius else outerRadius
            angle += step
            lineTo(
                (center + r * Math.cos(angle)).toFloat(),
                (center + r * Math.sin(angle)).toFloat()
            )
        }
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * Super tactile physical calculator key.
 * Features 3D press depths, bounce spring animations, haptic responses and sound trigger.
 */
@Composable
fun TactileKey(
    text: String,
    onClick: (x: Float, y: Float) -> Unit,
    theme: CalcTheme,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier,
    isDoubleWidth: Boolean = false,
    isAccent: Boolean = false,
    isEquals: Boolean = false,
    isSpec: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Explicitly enable sound and haptic feedback flags on the view
    LaunchedEffect(view) {
        try {
            view.isHapticFeedbackEnabled = true
            view.isSoundEffectsEnabled = true
        } catch (e: Exception) {}
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager }
    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
    }
    
    // Dynamic spring feedback based on Theme (Float)
    val springSpecFloat = when (theme.styleId) {
        ThemeStyle.CANDY -> spring<Float>(dampingRatio = 0.4f, stiffness = 400f) // bouncy jelly
        ThemeStyle.RETRO -> spring(dampingRatio = 0.82f, stiffness = 900f) // snappy mechanical switch
        ThemeStyle.NEON -> spring(dampingRatio = 0.7f, stiffness = 500f)
        else -> spring(dampingRatio = 0.8f, stiffness = 600f)
    }

    // Dynamic spring feedback based on Theme (Dp)
    val springSpecDp = when (theme.styleId) {
        ThemeStyle.CANDY -> spring<Dp>(dampingRatio = 0.4f, stiffness = 400f)
        ThemeStyle.RETRO -> spring(dampingRatio = 0.82f, stiffness = 900f)
        ThemeStyle.NEON -> spring(dampingRatio = 0.7f, stiffness = 500f)
        else -> spring(dampingRatio = 0.8f, stiffness = 600f)
    }

    // Key scale animations
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = springSpecFloat,
        label = "KeyScale"
    )

    // Layout position calculations for spawning particles on the press spot
    var buttonAbsoluteCoords by remember { mutableStateOf(Offset.Zero) }

    // Resolve color scheme for this button from theme definitions
    val isLimeAccent = theme.styleId == ThemeStyle.ARTISTIC && (isEquals || text == "÷")

    val buttonBgColor = when {
        isLimeAccent -> Color(0xFFC3FF4D)
        isEquals -> theme.equalsKeyColor
        isAccent -> theme.actionKeyColor
        isSpec -> theme.specKeyColor
        else -> theme.numKeyColor
    }

    val buttonTextColor = when {
        isLimeAccent -> Color(0xFF0F0F0F)
        isEquals -> theme.equalsKeyTextColor
        isAccent -> theme.actionKeyTextColor
        isSpec -> theme.specKeyTextColor
        else -> theme.numKeyTextColor
    }

    val currentKeyShape = if (theme.styleId == ThemeStyle.ARTISTIC) {
        if (isLimeAccent) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)
    } else {
        theme.keyShape
    }

    val finalBorderWidth = when {
        theme.styleId == ThemeStyle.ARTISTIC -> {
            if (isAccent && !isLimeAccent) 1.dp else 0.dp
        }
        theme.hasTactileBorders -> theme.borderWeight.dp
        else -> 0.dp
    }

    val finalBorderColor = when {
        theme.styleId == ThemeStyle.ARTISTIC -> {
            if (isAccent && !isLimeAccent) Color(0xFF333333) else Color.Transparent
        }
        theme.hasTactileBorders -> theme.borderColor
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(6.dp)
            .aspectRatio(if (isDoubleWidth) 2.1f else 1f, matchHeightConstraintsFirst = isDoubleWidth)
            .scale(scaleFactor)
            .onGloballyPositioned { coordinates ->
                buttonAbsoluteCoords = coordinates.positionInWindow().copy(
                    x = coordinates.positionInWindow().x + coordinates.size.width / 2f,
                    y = coordinates.positionInWindow().y + coordinates.size.height / 2f
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressed = true
                        
                        // Active sound response
                        if (soundEnabled) {
                            try {
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                        // Active haptics
                        if (hapticEnabled) {
                            try {
                                // Try view-level keyboard tap haptic first with override flags
                                view.performHapticFeedback(
                                    HapticFeedbackConstants.KEYBOARD_TAP,
                                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or
                                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                                )
                                // Fallback/supplement via Android Hardware Vibrator for absolute 100% success on all devices (especially ROMs where touch haptic is muted in settings)
                                if (vibrator != null && vibrator.hasVibrator()) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator.vibrate(
                                            android.os.VibrationEffect.createOneShot(
                                                18, // Ultra tight, premium professional 18ms keyboard click buzz
                                                android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                            )
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(18)
                                    }
                                }
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                        
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                            onClick(buttonAbsoluteCoords.x, buttonAbsoluteCoords.y)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Brutalist Neo-Pop style utilizes offsets and real physical thick solid shadows
        if (theme.styleId == ThemeStyle.BRUTALIST) {
            val offsetDp by animateDpAsState(
                targetValue = if (isPressed) 0.dp else theme.keyElevation.dp,
                animationSpec = springSpecDp,
                label = "ShadowDistance"
            )
            
            // Draw brutalist solid black shadow
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(x = offsetDp, y = offsetDp)
                    .background(theme.shadowColor, shape = theme.keyShape)
                    .border(width = finalBorderWidth, color = finalBorderColor, shape = theme.keyShape)
            )
            
            // Top overlay layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(buttonBgColor, shape = theme.keyShape)
                    .border(width = finalBorderWidth, color = finalBorderColor, shape = theme.keyShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = buttonTextColor,
                    fontSize = if (text.length > 2) 20.sp else 28.sp,
                    fontWeight = theme.fontWeight,
                    fontFamily = theme.fontFamily,
                    textAlign = TextAlign.Center
                )
            }
        } else if (theme.styleId == ThemeStyle.RETRO) {
            // Bevelled mechanical retro switch depth
            val shadowHighlight = if (theme.isDark) Color(0x33FFFFFF) else Color(0xAAFFFFFF)
            val shadowColor = theme.shadowColor
            val topOffset = if (isPressed) 2.dp else 4.dp
            
            Box(
                Modifier
                    .fillMaxSize()
                    .background(shadowColor, shape = theme.keyShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = topOffset)
                        .background(buttonBgColor, shape = theme.keyShape)
                        .border(width = finalBorderWidth, color = finalBorderColor, shape = theme.keyShape)
                        .drawBehind {
                            drawLine(
                                color = shadowHighlight,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = shadowHighlight,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 3f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = buttonTextColor,
                        fontSize = if (text.length > 2) 18.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = theme.fontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (theme.styleId == ThemeStyle.NEON) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRoundRect(
                            color = theme.shadowColor.copy(alpha = if (isPressed) 0.6f else 0.25f),
                            size = size,
                            cornerRadius = CornerRadius(24f, 24f),
                            style = Stroke(width = if (isPressed) 12f else 6f)
                        )
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(buttonBgColor, buttonBgColor.copy(alpha = 0.8f))
                        ),
                        shape = theme.keyShape
                    )
                    .border(
                        width = if (isPressed) 3.dp else 1.5.dp,
                        color = if (isPressed) Color.White else finalBorderColor,
                        shape = theme.keyShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (isPressed) Color.White else buttonTextColor,
                    fontSize = if (text.length > 2) 20.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = theme.fontFamily,
                    textAlign = TextAlign.Center
                )
            }
        } else if (theme.styleId == ThemeStyle.ARTISTIC) {
            // Elegant Bento design: 
            // - Numeric/Special action keys are flat rounded rectangles.
            // - Active presses apply slight opacity/scaling overlay for visual satisfaction.
            val bentoAlpha by animateFloatAsState(
                targetValue = if (isPressed) 0.7f else 1.0f,
                animationSpec = springSpecFloat,
                label = "BentoAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(bentoAlpha)
                    .background(buttonBgColor, shape = currentKeyShape)
                    .border(width = finalBorderWidth, color = finalBorderColor, shape = currentKeyShape)
                    .clip(currentKeyShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = buttonTextColor,
                    fontSize = if (text.length > 2) 20.sp else 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = theme.fontFamily,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Elegant modern look with spring depth elevation
            val relativeElevation by animateDpAsState(
                targetValue = if (isPressed) 1.dp else theme.keyElevation.dp,
                animationSpec = springSpecDp,
                label = "CardElevation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (relativeElevation > 0.dp) {
                            drawRoundRect(
                                color = theme.shadowColor.copy(alpha = 0.15f),
                                topLeft = Offset(0f, relativeElevation.toPx() / 1.5f + 2f),
                                size = size,
                                cornerRadius = CornerRadius(48f, 48f)
                            )
                        }
                    }
                    .background(buttonBgColor, shape = theme.keyShape)
                    .clip(theme.keyShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = buttonTextColor,
                    fontSize = if (text.length > 2) 22.sp else 28.sp,
                    fontWeight = theme.fontWeight,
                    fontFamily = theme.fontFamily,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Beautiful Horizontal Theme switcher.
 * Displays the list of custom premium themes with cute active indicator elements.
 */
@Composable
fun ThemeSelectorRow(
    currentTheme: CalcTheme,
    onThemeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎨",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            
            Text(
                text = currentTheme.name,
                fontFamily = currentTheme.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = currentTheme.screenTextColor.copy(alpha = 0.8f)
            )
        }

        // Beautiful mini capsules to click and slide
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.theme.ThemesList.forEach { item ->
                val isActive = item.id == currentTheme.id
                val activeSize by animateDpAsState(
                    targetValue = if (isActive) 24.dp else 12.dp,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "ThemeCircleWidth"
                )

                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .width(activeSize)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(item.equalsKeyColor, item.actionKeyColor)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) currentTheme.screenTextColor else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable(
                            onClick = { onThemeSelected(item.id) },
                            indication = null, 
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )
            }
        }
    }
}

/**
 * Receipt style historical log item.
 */
@Composable
fun HistoryRow(
    item: HistoryItem,
    fontFamily: FontFamily,
    textColor: Color,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = item.expression,
            fontFamily = fontFamily,
            fontSize = 15.sp,
            color = textColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "= " + item.result,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = textColor,
            textAlign = TextAlign.Right
        )
    }
}
