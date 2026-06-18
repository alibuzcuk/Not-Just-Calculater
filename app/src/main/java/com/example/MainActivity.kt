package com.example

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.example.ui.theme.ThemeStyle

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentTheme by viewModel.theme.collectAsState()
            
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = currentTheme.mainBackground
                ) {
                    NotJustCalculatorApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NotJustCalculatorApp(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsState()
    val liveResult by viewModel.liveResult.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val history by viewModel.history.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val shakeTrigger by viewModel.shakeTrigger.collectAsState()

    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    // SharedPreferences to restore saved theme choice
    val sharedPrefs = remember { context.getSharedPreferences("calc_settings", Context.MODE_PRIVATE) }
    
    // Load persisted theme on start
    LaunchedEffect(Unit) {
        val savedTheme = sharedPrefs.getString("selected_theme_id", "artistic_flair") ?: "artistic_flair"
        viewModel.setTheme(savedTheme)
    }

    // Slide down animation state for historical receipt drawer
    var showHistoryDrawer by remember { mutableStateOf(false) }

    // Screen Shake Animation logic
    val shakeOffset by animateDpAsState(
        targetValue = if (shakeTrigger) 12.dp else 0.dp,
        animationSpec = keyframes {
            durationMillis = 150
            (-10).dp at 15
            10.dp at 45
            (-8).dp at 75
            8.dp at 105
            (-4).dp at 135
        },
        label = "ShakeAnimation"
    )

    // Main responsive wrapper centering content on tablets
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(currentTheme.mainBackground),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 520.dp) // Perfect tablet sizing
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP UTILITY STATUS BAR OR ARTISTIC HEADER
                if (currentTheme.styleId == ThemeStyle.ARTISTIC) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "PREMIUM EXPERIENCE",
                                    fontFamily = currentTheme.fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color(0xFF666666),
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = "NOT JUST",
                                    fontFamily = currentTheme.fontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = Color(0xFFE0E0E0),
                                    lineHeight = 22.sp
                                )
                                Text(
                                    text = "Calculator",
                                    fontFamily = currentTheme.fontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    color = Color(0xFFC3FF4D),
                                    lineHeight = 30.sp
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sound switch
                                    IconButton(
                                        onClick = { viewModel.toggleSound() },
                                        modifier = Modifier.size(36.dp).testTag("sound_toggle")
                                    ) {
                                        Text(
                                            text = if (soundEnabled) "🔊" else "🔇",
                                            fontSize = 16.sp
                                        )
                                    }

                                    // Vibration switch
                                    IconButton(
                                        onClick = { viewModel.toggleHaptic() },
                                        modifier = Modifier.size(36.dp).testTag("haptic_toggle")
                                    ) {
                                        Text(
                                            text = if (hapticEnabled) "📳" else "📴",
                                            fontSize = 16.sp,
                                            modifier = Modifier.alpha(if (hapticEnabled) 1.0f else 0.4f)
                                        )
                                    }

                                    // TM badge
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFF333333), shape = RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "TM",
                                            color = Color(0xFFC3FF4D),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "Vibrant Theme",
                                    fontFamily = currentTheme.fontFamily,
                                    fontSize = 9.sp,
                                    color = Color(0xFF666666),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOT JUST",
                            fontFamily = currentTheme.fontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = currentTheme.screenTextColor.copy(alpha = 0.5f),
                            letterSpacing = 2.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sound switch
                            IconButton(
                                onClick = { viewModel.toggleSound() },
                                modifier = Modifier.testTag("sound_toggle")
                            ) {
                                Text(
                                    text = if (soundEnabled) "🔊" else "🔇",
                                    fontSize = 18.sp
                                )
                            }

                            // Vibration switch
                            IconButton(
                                onClick = { viewModel.toggleHaptic() },
                                modifier = Modifier.testTag("haptic_toggle")
                            ) {
                                Text(
                                    text = if (hapticEnabled) "📳" else "📴",
                                    fontSize = 18.sp,
                                    modifier = Modifier.alpha(if (hapticEnabled) 1.0f else 0.4f)
                                )
                            }

                            // Clear history
                            IconButton(
                                onClick = { viewModel.clearHistory() },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Text(
                                    text = "🗑",
                                    fontSize = 18.sp,
                                    modifier = Modifier.alpha(0.7f)
                                )
                            }
                        }
                    }
                }

                // 2. PRIMARY DISPLAY CABINET
                val displayBorder = if (currentTheme.hasTactileBorders) currentTheme.borderWeight.dp else 0.dp
                val displayBorderColor = if (currentTheme.hasTactileBorders) currentTheme.borderColor else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                        .offset(x = shakeOffset)
                        .shadow(
                            elevation = if (currentTheme.styleId == ThemeStyle.BRUTALIST) 4.dp else 2.dp,
                            shape = currentTheme.keyShape
                        )
                        .background(currentTheme.screenBackground, shape = currentTheme.keyShape)
                        .border(
                            width = displayBorder,
                            color = displayBorderColor,
                            shape = currentTheme.keyShape
                        )
                        // Swipe to delete gesture!
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -15) { // Swiped left
                                    viewModel.onKeyPressed("DEL")
                                }
                            }
                        }
                        .clip(currentTheme.keyShape),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Decorative backgrid in retro styling
                    if (currentTheme.styleId == ThemeStyle.RETRO) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dotSpacing = 30f
                            val dotRadius = 2f
                            val widthVal = drawContext.size.width
                            val heightVal = drawContext.size.height
                            for (x in 0..widthVal.toInt() step dotSpacing.toInt()) {
                                for (y in 0..heightVal.toInt() step dotSpacing.toInt()) {
                                    drawCircle(
                                        color = currentTheme.screenTextColor.copy(alpha = 0.08f),
                                        radius = dotRadius,
                                        center = Offset(x.toFloat(), y.toFloat())
                                    )
                                }
                            }
                        }
                    }

                    if (currentTheme.styleId == ThemeStyle.ARTISTIC) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Left vertical bar of accent color (alpha 20%)
                            drawLine(
                                color = Color(0xFFC3FF4D).copy(alpha = 0.20f),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 14f
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        // Drag down indicator hint
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Row(
                                modifier = Modifier
                                    .alpha(0.6f)
                                    .padding(top = 2.dp)
                                    .background(
                                        color = currentTheme.screenTextColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clickable { showHistoryDrawer = !showHistoryDrawer },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "⏱",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "GEÇMİŞ",
                                    fontFamily = currentTheme.fontFamily,
                                    fontSize = 11.sp,
                                    color = currentTheme.screenTextColor,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Expression Text Area
                        val dynamicFontSize = when {
                            expression.length > 18 -> 26.sp
                            expression.length > 13 -> 34.sp
                            expression.length > 8 -> 42.sp
                            else -> 52.sp
                        }

                        AnimatedContent(
                            targetState = expression,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "ExpressionAnim"
                        ) { text ->
                            val rawText = text.ifEmpty { "0" }
                            val displayAnnotatedText = remember(rawText, currentTheme.styleId) {
                                buildAnnotatedString {
                                    val limeColor = Color(0xFFC3FF4D)
                                    rawText.forEach { char ->
                                        if (currentTheme.styleId == ThemeStyle.ARTISTIC && (char == '.' || char == ',' || char == '+' || char == '-' || char == '×' || char == '÷' || char == '%')) {
                                            withStyle(style = SpanStyle(color = limeColor)) {
                                                append(char)
                                            }
                                        } else {
                                            append(char)
                                        }
                                    }
                                }
                            }

                            Text(
                                text = displayAnnotatedText,
                                fontFamily = currentTheme.fontFamily,
                                fontWeight = if (currentTheme.styleId == ThemeStyle.BRUTALIST || currentTheme.styleId == ThemeStyle.ARTISTIC) FontWeight.Black else FontWeight.Medium,
                                fontSize = dynamicFontSize,
                                color = if (text.isEmpty()) currentTheme.screenTextColor.copy(alpha = 0.35f) else currentTheme.screenTextColor,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Live preview calculations
                        AnimatedVisibility(
                            visible = liveResult.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = "= $liveResult",
                                fontFamily = currentTheme.fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = currentTheme.screenTextColor.copy(alpha = 0.5f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 3. MID PANEL: SWITCHER BAR & SWIPER LOG DRAWER
                ThemeSelectorRow(
                    currentTheme = currentTheme,
                    onThemeSelected = { newThemeId ->
                        viewModel.setTheme(newThemeId)
                        sharedPrefs.edit().putString("selected_theme_id", newThemeId).apply()
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // 4. MAIN INTERACTIVE BUTTON GRID
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val rowKeys = listOf(
                            listOf("AC", "DEL", "%", "÷"),
                            listOf("7", "8", "9", "×"),
                            listOf("4", "5", "6", "-"),
                            listOf("1", "2", "3", "+"),
                            listOf("0", ".", "H", "=") // 'H' represents History Toggle key
                        )

                        rowKeys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { key ->
                                    val isAction = key in listOf("÷", "×", "-", "+")
                                    val isEquals = key == "="
                                    val isSpec = key in listOf("AC", "DEL", "%", "H")

                                    TactileKey(
                                        text = if (key == "H") "⏱" else key,
                                        onClick = { x, y ->
                                            if (key == "H") {
                                                showHistoryDrawer = !showHistoryDrawer
                                            } else {
                                                viewModel.onKeyPressed(key, x, y)
                                            }
                                        },
                                        theme = currentTheme,
                                        soundEnabled = soundEnabled,
                                        hapticEnabled = hapticEnabled,
                                        isAccent = isAction,
                                        isEquals = isEquals,
                                        isSpec = isSpec,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // FLYING SPARKLE ELEMENT DECK (OVER buttons)
                    SparkleCanvas(
                        particles = viewModel.particles,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 5. EXPANDABLE RECEIPT SHEET (SLIDING DRAWER FROM DISPLAY AREA)
            AnimatedVisibility(
                visible = showHistoryDrawer,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .padding(8.dp)
                        .shadow(16.dp, shape = RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = currentTheme.screenBackground
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(
                            listOf(currentTheme.equalsKeyColor.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        width = 1.5.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GEÇMİŞ GÜNLÜK",
                                fontFamily = currentTheme.fontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = currentTheme.screenTextColor,
                                letterSpacing = 2.sp
                            )

                            IconButton(
                                onClick = { showHistoryDrawer = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = currentTheme.screenTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = currentTheme.screenTextColor.copy(alpha = 0.15f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "⏱",
                                        fontSize = 44.sp,
                                        modifier = Modifier.alpha(0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Hesaplama geçmişi boş.",
                                        fontFamily = currentTheme.fontFamily,
                                        fontSize = 14.sp,
                                        color = currentTheme.screenTextColor.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(history, key = { it.id }) { item ->
                                    HistoryRow(
                                        item = item,
                                        fontFamily = currentTheme.fontFamily,
                                        textColor = currentTheme.screenTextColor,
                                        onItemClicked = {
                                            viewModel.selectHistoryItem(item)
                                            showHistoryDrawer = false
                                        }
                                    )
                                    HorizontalDivider(
                                        color = currentTheme.screenTextColor.copy(alpha = 0.05f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
