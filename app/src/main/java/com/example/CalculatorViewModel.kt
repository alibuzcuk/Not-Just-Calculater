package com.example

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.CalcTheme
import com.example.ui.theme.ThemesList
import com.example.ui.theme.ThemeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class HistoryItem(
    val id: String = Random.nextLong().toString(),
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Particle(
    val id: Long = Random.nextLong(),
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val isStar: Boolean = false,
    val isEmoji: Boolean = false,
    val emoji: String = "",
    val maxAge: Int,
    var age: Int = 0
)

class CalculatorViewModel : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _liveResult = MutableStateFlow("")
    val liveResult: StateFlow<String> = _liveResult.asStateFlow()

    private val _theme = MutableStateFlow(ThemesList[0])
    val theme: StateFlow<CalcTheme> = _theme.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    // Screen shake animation trigger (duration in offset)
    private val _shakeTrigger = MutableStateFlow(false)
    val shakeTrigger: StateFlow<Boolean> = _shakeTrigger.asStateFlow()

    // Particle pool rendered inside Compose canvas
    val particles = mutableStateListOf<Particle>()

    // Decimal formatter for results
    private val decimalSymbols = DecimalFormatSymbols(Locale.US).apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    }
    private val df = DecimalFormat("#,##0.######", decimalSymbols)

    init {
        // Start engine for particles
        viewModelScope.launch {
            while (true) {
                updateParticles()
                delay(16) // ~60 FPS update
            }
        }
    }

    fun setTheme(themeId: String) {
        val found = ThemesList.firstOrNull { it.id == themeId }
        if (found != null) {
            _theme.value = found
        }
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    fun toggleHaptic() {
        _hapticEnabled.value = !_hapticEnabled.value
    }

    fun onKeyPressed(key: String, buttonX: Float = 0f, buttonY: Float = 0f) {
        // Spawn active press feedback particles
        spawnClickParticles(key, buttonX, buttonY)

        when (key) {
            "AC" -> clearAll()
            "DEL" -> deleteLast()
            "=" -> evaluateFinal()
            "%" -> applyPercentage()
            "+", "-", "×", "÷" -> appendOperator(key)
            else -> appendKey(key)
        }
    }

    private fun clearAll() {
        _expression.value = ""
        _liveResult.value = ""
        triggerShake()
    }

    private fun deleteLast() {
        val cur = _expression.value
        if (cur.isNotEmpty()) {
            _expression.value = cur.dropLast(1)
            calculateLiveResult()
        }
    }

    private fun appendKey(key: String) {
        val cur = _expression.value
        // Limit total length to avoid overflow
        if (cur.length >= 60) return

        if (key == ".") {
            val lastNumber = cur.split('+', '-', '×', '÷').lastOrNull() ?: ""
            if (lastNumber.contains(".")) return
            if (cur.isEmpty() || cur.last() in listOf('+', '-', '×', '÷')) {
                _expression.value += "0"
            }
        }

        _expression.value += key
        calculateLiveResult()
    }

    private fun appendOperator(op: String) {
        val cur = _expression.value
        if (cur.isEmpty()) {
            // Can start with minus
            if (op == "-") {
                _expression.value = "-"
            }
            return
        }

        val lastChar = cur.last()
        if (lastChar in listOf('+', '-', '×', '÷')) {
            // Replace last operator
            _expression.value = cur.dropLast(1) + op
        } else {
            _expression.value = cur + op
        }
    }

    private fun applyPercentage() {
        val cur = _expression.value
        if (cur.isEmpty() || cur.last() in listOf('+', '-', '×', '÷', '.')) return
        
        // Find last numeric token to calculate percentage of it
        val tokens = cur.split(Regex("(?<=[+×÷-])|(?=[+×÷-])"))
        val lastToken = tokens.lastOrNull() ?: ""
        if (lastToken.isNotEmpty() && lastToken.toDoubleOrNull() != null) {
            val percentageVal = lastToken.toDouble() / 100.0
            val prefix = cur.substring(0, cur.length - lastToken.length)
            _expression.value = prefix + percentageVal.toString()
            calculateLiveResult()
        }
    }

    private fun calculateLiveResult() {
        val cur = _expression.value
        if (cur.isEmpty() || cur.last() in listOf('+', '-', '×', '÷', '.')) {
            _liveResult.value = ""
            return
        }

        // Only show live result if there are actual operations
        if (!cur.any { it in listOf('+', '-', '×', '÷') }) {
            _liveResult.value = ""
            return
        }

        try {
            val outcome = mathEvaluate(cur)
            if (outcome.isFinite()) {
                _liveResult.value = formatResult(outcome)
            } else {
                _liveResult.value = ""
            }
        } catch (e: Exception) {
            _liveResult.value = ""
        }
    }

    private fun evaluateFinal() {
        val cur = _expression.value
        if (cur.isEmpty()) return

        // If it's a simple number without any operators, just make particles trigger
        if (!cur.any { it in listOf('+', '-', '×', '÷') }) {
            spawnCelebrationParticles()
            return
        }

        try {
            val outcome = mathEvaluate(cur)
            val resultStr = if (outcome.isInfinite() || outcome.isNaN()) {
                "Sonsuz"
            } else {
                formatResult(outcome)
            }

            // Save to history
            val newHistory = HistoryItem(expression = cur, result = resultStr)
            _history.value = listOf(newHistory) + _history.value

            _expression.value = resultStr
            _liveResult.value = ""

            // Spawn celebration sparks!
            spawnCelebrationParticles()

        } catch (e: Exception) {
            _liveResult.value = "Hata"
            triggerShake()
        }
    }

    fun selectHistoryItem(item: HistoryItem) {
        _expression.value = item.result
        _liveResult.value = ""
        spawnCelebrationParticles()
    }

    fun clearHistory() {
        _history.value = emptyList()
        triggerShake()
    }

    private fun formatResult(num: Double): String {
        val absVal = kotlin.math.abs(num)
        if (absVal >= 1e15 || (absVal > 0.0 && absVal < 1e-6)) {
            // Represent extremely large or small numbers in scientific e notation (e.g. 7e39)
            val scientificDf = DecimalFormat("0.######E0", decimalSymbols)
            return scientificDf.format(num).replace("E", "e").replace("e+", "e")
        }
        return if (absVal < 9e18 && num % 1.0 == 0.0) {
            df.format(num.toLong())
        } else {
            df.format(num)
        }
    }

    private fun triggerShake() {
        viewModelScope.launch {
            _shakeTrigger.value = true
            delay(150)
            _shakeTrigger.value = false
        }
    }

    // Mathematical parser supporting precedence & parenthesized or simple chained tokens
    private fun mathEvaluate(exprString: String): Double {
        // Clean expression for parser
        val formula = exprString
            .replace("×", "*")
            .replace("÷", "/")
            .replace("e", "E")
            .replace("E+", "E")
            .replace(" ", "")

        if (formula.isEmpty()) return 0.0

        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < formula.length) formula[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < formula.length) throw RuntimeException("Bilinmeyen karakter: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) return Double.POSITIVE_INFINITY
                        x /= divisor // division
                    }
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || ch == 'E'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || ch == 'E'.code || (ch == '-'.code && formula.getOrNull(pos - 1) == 'E')) {
                        nextChar()
                    }
                    x = formula.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Beklenmeyen değer: " + ch.toChar())
                }

                return x
            }
        }.parse()
    }

    // --- Particle Animation Engine ---

    private fun spawnClickParticles(key: String, x: Float, y: Float) {
        val clickCount = if (theme.value.styleId == ThemeStyle.CANDY) 8 else 5
        val color = theme.value.equalsKeyColor

        for (i in 0 until clickCount) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val speed = Random.nextDouble(2.0, 6.0)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (sin(angle) * speed).toFloat(),
                    vy = (cos(angle) * speed - 1f).toFloat(), // Slight upward force
                    color = getThemeParticleColor(),
                    size = Random.nextFloat() * 12f + 8f,
                    alpha = 1.0f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f,
                    isStar = Random.nextBoolean(),
                    maxAge = Random.nextInt(15, 30)
                )
            )
        }
    }

    private fun spawnCelebrationParticles() {
        val themeStyle = theme.value.styleId
        val emitterCount = 35
        
        // Emits particles from multiple spots to cover the screen
        val midX = 500f // estimated center will be passed or scaled
        val midY = 400f

        for (i in 0 until emitterCount) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val speed = Random.nextDouble(4.0, 15.0)
            
            // Special themed particles
            val isEmoji = themeStyle == ThemeStyle.CANDY || themeStyle == ThemeStyle.RETRO
            val emojiList = if (themeStyle == ThemeStyle.CANDY) {
                listOf("🍭", "🍬", "🌸", "✨", "🎈")
            } else {
                listOf("👾", "🕹️", "⚡", "⭐", "💾")
            }
            
            particles.add(
                Particle(
                    x = midX + (Random.nextFloat() * 300f - 150f),
                    y = midY + (Random.nextFloat() * 100f - 50f),
                    vx = (sin(angle) * speed).toFloat(),
                    vy = (cos(angle) * speed - 3f).toFloat(), // Strong upward launch
                    color = getThemeParticleColor(),
                    size = if (isEmoji) 36f else Random.nextFloat() * 18f + 10f,
                    alpha = 1.0f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 15f - 7.5f,
                    isStar = Random.nextInt(3) == 0,
                    isEmoji = isEmoji && Random.nextInt(2) == 0,
                    emoji = emojiList.random(),
                    maxAge = Random.nextInt(40, 70)
                )
            )
        }
    }

    private fun getThemeParticleColor(): Color {
        val curTheme = theme.value
        return when (curTheme.styleId) {
            ThemeStyle.MODERN -> listOf(curTheme.equalsKeyColor, curTheme.actionKeyColor, Color.White).random()
            ThemeStyle.RETRO -> listOf(Color(0xFF8BAC0F), Color(0xFF333333), Color(0xFFFF4500), Color(0xFF8B0000)).random()
            ThemeStyle.NEON -> listOf(Color(0xFF00FFF0), Color(0xFFFF007F), Color(0xFFBC00DD), Color.White).random()
            ThemeStyle.CANDY -> listOf(Color(0xFFFF69B4), Color(0xFFFFB6C1), Color(0xFFE0FFFF), Color(0xFFFFF8DC)).random()
            ThemeStyle.MATCHA -> listOf(curTheme.actionKeyColor, curTheme.screenTextColor, Color.White).random()
            ThemeStyle.BRUTALIST -> listOf(Color(0xFFFCF054), Color(0xFFFF7A5C), Color(0xFF46E3A3), Color.Black).random()
            ThemeStyle.ARTISTIC -> listOf(Color(0xFFC3FF4D), Color(0xFFFFFFFF), Color(0xFF1E1E1E)).random()
        }
    }

    private fun updateParticles() {
        if (particles.isEmpty()) return
        
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age++
            if (p.age >= p.maxAge) {
                iterator.remove()
            } else {
                // Apply velocity, gravity & deceleration
                p.x += p.vx
                p.y += p.vy
                
                // Gravity pull
                p.vy += 0.35f
                
                // Air resistance
                p.vx *= 0.96f
                p.vy *= 0.98f
                
                // Rotation
                p.rotation += p.rotationSpeed
                
                // Fade out as it ages
                val remainingRatio = (p.maxAge - p.age).toFloat() / p.maxAge.toFloat()
                p.alpha = remainingRatio
            }
        }
    }
}
