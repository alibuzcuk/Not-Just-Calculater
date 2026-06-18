package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CalcTheme(
    val id: String,
    val name: String,
    val turkishName: String,
    val isDark: Boolean,
    
    // Backgrounds
    val mainBackground: Color,
    val screenBackground: Color,
    val screenTextColor: Color,
    
    // Key Colors
    val numKeyColor: Color,
    val numKeyTextColor: Color,
    val actionKeyColor: Color,
    val actionKeyTextColor: Color,
    val equalsKeyColor: Color,
    val equalsKeyTextColor: Color,
    val specKeyColor: Color, // AC, DEL, etc.
    val specKeyTextColor: Color,
    
    // Key Styling
    val shadowColor: Color,
    val keyShape: RoundedCornerShape,
    val keyElevation: Int,
    val hasTactileBorders: Boolean,
    val borderWeight: Float = 0f,
    val borderColor: Color = Color.Transparent,
    
    // Aesthetic Attributes
    val fontFamily: FontFamily,
    val fontWeight: FontWeight = FontWeight.SemiBold,
    val styleId: ThemeStyle = ThemeStyle.MODERN
)

enum class ThemeStyle {
    MODERN, RETRO, NEON, CANDY, MATCHA, BRUTALIST, ARTISTIC
}

val ThemesList = listOf(
    // 1. ARTISTIC FLAIR (Sleek Dark Bento & Acid Lime - Primary Premium Highlight)
    CalcTheme(
        id = "artistic_flair",
        name = "Artistic Flair",
        turkishName = "Bento Asit",
        isDark = true,
        mainBackground = Color(0xFF0F0F0F),
        screenBackground = Color(0xFF0F0F0F),
        screenTextColor = Color(0xFFFFFFFF),
        numKeyColor = Color(0xFF1A1A1A),
        numKeyTextColor = Color(0xFFFFFFFF),
        actionKeyColor = Color(0xFF1A1A1A),
        actionKeyTextColor = Color(0xFFC3FF4D),
        equalsKeyColor = Color(0xFFC3FF4D),
        equalsKeyTextColor = Color(0xFF0F0F0F),
        specKeyColor = Color(0xFF1A1A1A),
        specKeyTextColor = Color(0xFFC3FF4D),
        shadowColor = Color(0x33000000),
        keyShape = RoundedCornerShape(16.dp), // Bento style 16dp rounded
        keyElevation = 0,
        hasTactileBorders = true,
        borderWeight = 1f,
        borderColor = Color(0xFF333333),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        styleId = ThemeStyle.ARTISTIC
    ),

    // 2. MODERN LUXURY (Default)
    CalcTheme(
        id = "modern_luxury",
        name = "Cosmic Luxury",
        turkishName = "Kozmik Lüks",
        isDark = true,
        mainBackground = Color(0xFF121214),
        screenBackground = Color(0xFF1E1E24),
        screenTextColor = Color(0xFFE3E3E6),
        numKeyColor = Color(0xFF2D2C30),
        numKeyTextColor = Color(0xFFF1F1F4),
        actionKeyColor = Color(0xFFFF9F0A),
        actionKeyTextColor = Color(0xFFFFFFFF),
        equalsKeyColor = Color(0xFFFF5E5E),
        equalsKeyTextColor = Color(0xFFFFFFFF),
        specKeyColor = Color(0xFF3E3D42),
        specKeyTextColor = Color(0xFFE4E4E6),
        shadowColor = Color(0x33000000),
        keyShape = RoundedCornerShape(24.dp),
        keyElevation = 4,
        hasTactileBorders = false,
        fontFamily = FontFamily.SansSerif,
        styleId = ThemeStyle.MODERN
    ),

    // 2. RETRO ARCADE (Gameboy/Game & Watch Vibe)
    CalcTheme(
        id = "retro_arcade",
        name = "Retro Arcade",
        turkishName = "Retro Atari",
        isDark = false,
        mainBackground = Color(0xFFF0EAE1),
        screenBackground = Color(0xFF8BAC0F), // Classic Gameboy Green Screen
        screenTextColor = Color(0xFF0F380F),
        numKeyColor = Color(0xFF333333), // Slate black buttons
        numKeyTextColor = Color(0xFFFFFFFF),
        actionKeyColor = Color(0xFF8B0000), // Dark Red action buttons
        actionKeyTextColor = Color(0xFFF0EAE1),
        equalsKeyColor = Color(0xFFFF4500), // Orange equals button
        equalsKeyTextColor = Color(0xFFFFFFFF),
        specKeyColor = Color(0xFF666666),
        specKeyTextColor = Color(0xFFEEEEEE),
        shadowColor = Color(0x66000000),
        keyShape = RoundedCornerShape(8.dp), // Blocky
        keyElevation = 8,
        hasTactileBorders = true,
        borderWeight = 3f,
        borderColor = Color(0xFF000000),
        fontFamily = FontFamily.Monospace,
        styleId = ThemeStyle.RETRO
    ),

    // 3. CYBERPUNK NEON (Midnight Tokyo)
    CalcTheme(
        id = "cyber_neon",
        name = "Cyber Neon",
        turkishName = "Siber Neon",
        isDark = true,
        mainBackground = Color(0xFF0B0914), // Dark purple-black
        screenBackground = Color(0xFF130F26),
        screenTextColor = Color(0xFF00FFE0), // Cyan glow
        numKeyColor = Color(0xFF1F1235), // Dark synthwave purple
        numKeyTextColor = Color(0xFFFFFFFF),
        actionKeyColor = Color(0xFFFF007F), // Glowing magenta
        actionKeyTextColor = Color(0xFFFFFFFF),
        equalsKeyColor = Color(0xFF00FFF0), // Cyan equals
        equalsKeyTextColor = Color(0xFF0B0914),
        specKeyColor = Color(0xFF3D215A),
        specKeyTextColor = Color(0xFFFFB3FD),
        shadowColor = Color(0xFFFF007F), // Neon shadows!
        keyShape = RoundedCornerShape(16.dp),
        keyElevation = 0, // Neon lights have border glow, no standard shadow
        hasTactileBorders = true,
        borderWeight = 1.5f,
        borderColor = Color(0xFFFF007F),
        fontFamily = FontFamily.Monospace,
        styleId = ThemeStyle.NEON
    ),

    // 4. CANDY POP (Bubblegum Sweet)
    CalcTheme(
        id = "candy_pop",
        name = "Candy Pop",
        turkishName = "Şeker Sepeti",
        isDark = false,
        mainBackground = Color(0xFFFFF0F5), // Lavender blush
        screenBackground = Color(0xFFFFE4E1), // Misty Rose
        screenTextColor = Color(0xFFFF69B4), // Hot Pink
        numKeyColor = Color(0xFFFFF8DC), // Cornsilk soft white
        numKeyTextColor = Color(0xFFC71585),
        actionKeyColor = Color(0xFFE0FFFF), // Light cyan
        actionKeyTextColor = Color(0xFF008B8B),
        equalsKeyColor = Color(0xFFFFB6C1), // Pink equals
        equalsKeyTextColor = Color(0xFF8B0000),
        specKeyColor = Color(0xFFE6E6FA),
        specKeyTextColor = Color(0xFF483D8B),
        shadowColor = Color(0x1F000000),
        keyShape = RoundedCornerShape(50), // Fully circular/bubbly!
        keyElevation = 6,
        hasTactileBorders = false,
        fontFamily = FontFamily.SansSerif,
        styleId = ThemeStyle.CANDY
    ),

    // 5. ORGANIC MATCHA (Zen Garden)
    CalcTheme(
        id = "organic_matcha",
        name = "Matcha Zen",
        turkishName = "Yeşil Çay",
        isDark = false,
        mainBackground = Color(0xFFF1F5EF), // Matcha milk cream
        screenBackground = Color(0xFFDBE5D7),
        screenTextColor = Color(0xFF2D4D2A), // Forest wood green
        numKeyColor = Color(0xFFFFFFFF),
        numKeyTextColor = Color(0xFF425E3F),
        actionKeyColor = Color(0xFF7A9A75), // Matcha tea leaf green
        actionKeyTextColor = Color(0xFFFFFFFF),
        equalsKeyColor = Color(0xFFB4C3B2),
        equalsKeyTextColor = Color(0xFF1E350E),
        specKeyColor = Color(0xFFECEEED),
        specKeyTextColor = Color(0xFF5A7555),
        shadowColor = Color(0x12000000),
        keyShape = RoundedCornerShape(32.dp), // Super soft squircle
        keyElevation = 2,
        hasTactileBorders = false,
        fontFamily = FontFamily.SansSerif,
        styleId = ThemeStyle.MATCHA
    ),

    // 6. BOLD BRUTALIST (Paper & Heavy Shadows)
    CalcTheme(
        id = "bold_brutalist",
        name = "Neo Brutalist",
        turkishName = "Brütalist",
        isDark = false,
        mainBackground = Color(0xFFFAF7F2), // Fine craft cardboard paper
        screenBackground = Color(0xFFFFFFFF),
        screenTextColor = Color(0xFF000000),
        numKeyColor = Color(0xFFFFFFFF),
        numKeyTextColor = Color(0xFF000000),
        actionKeyColor = Color(0xFFFCF054), // Hot yellow
        actionKeyTextColor = Color(0xFF000000),
        equalsKeyColor = Color(0xFFFF7A5C), // Brutalist red-neon-coral
        equalsKeyTextColor = Color(0xFF000000),
        specKeyColor = Color(0xFF46E3A3), // Bright brutalist green
        specKeyTextColor = Color(0xFF000000),
        shadowColor = Color(0xFF000000), // Solid pitch black shadow!
        keyShape = RoundedCornerShape(4.dp), // Almost sharp block
        keyElevation = 8, // Very deep offset
        hasTactileBorders = true,
        borderWeight = 3f,
        borderColor = Color(0xFF000000),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        styleId = ThemeStyle.BRUTALIST
    )
)
