package com.dailydairy.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

fun launchBackground(context: android.content.Context, palette: Palette, appearance: Appearance): Int {
    val dark = isDark(context, appearance)
    val dynamic = palette == Palette.Sistem && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (dynamic) {
        val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return scheme.background.toArgb()
    }
    return colors(palette, dark).bg.toArgb()
}

private fun isDark(context: Context, appearance: Appearance): Boolean = when (appearance) {
    Appearance.Dark -> true
    Appearance.Light -> false
    Appearance.System -> context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

private data class PaletteColors(
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val soft: Color,
)

private fun colors(palette: Palette, dark: Boolean): PaletteColors = when (palette) {
    Palette.Kagit -> if (dark) {
        PaletteColors(
            bg = Color(0xFF161310),
            surface = Color(0xFF221E1A),
            ink = Color(0xFFF4EFE6),
            muted = Color(0xFFB3A89B),
            accent = Color(0xFFE08A72),
            onAccent = Color(0xFF2A140E),
            soft = Color(0xFF3A2A24),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFF7F3EC),
            surface = Color(0xFFFFFDF9),
            ink = Color(0xFF1C1915),
            muted = Color(0xFF6F675E),
            accent = Color(0xFFC4654A),
            onAccent = Color(0xFFFFF8F4),
            soft = Color(0xFFF3E3DA),
        )
    }

    Palette.Deniz -> if (dark) {
        PaletteColors(
            bg = Color(0xFF0C1618),
            surface = Color(0xFF142426),
            ink = Color(0xFFE7F4F5),
            muted = Color(0xFF9BB8BC),
            accent = Color(0xFF5EC4D4),
            onAccent = Color(0xFF062126),
            soft = Color(0xFF1B383D),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFF3F8F8),
            surface = Color(0xFFFBFEFF),
            ink = Color(0xFF102428),
            muted = Color(0xFF4D686D),
            accent = Color(0xFF0E7490),
            onAccent = Color(0xFFF3FCFF),
            soft = Color(0xFFD7EEF3),
        )
    }

    Palette.Zeytin -> if (dark) {
        PaletteColors(
            bg = Color(0xFF12170F),
            surface = Color(0xFF1C2418),
            ink = Color(0xFFEEF3E4),
            muted = Color(0xFFB4C0A4),
            accent = Color(0xFFB6C77A),
            onAccent = Color(0xFF1A220E),
            soft = Color(0xFF2A3520),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFF6F8F1),
            surface = Color(0xFFFCFDF8),
            ink = Color(0xFF1B2414),
            muted = Color(0xFF5C6750),
            accent = Color(0xFF5C6B32),
            onAccent = Color(0xFFF7FBEA),
            soft = Color(0xFFE4EDD0),
        )
    }

    Palette.Mercan -> if (dark) {
        PaletteColors(
            bg = Color(0xFF160F10),
            surface = Color(0xFF241618),
            ink = Color(0xFFF8ECEC),
            muted = Color(0xFFC7A8A8),
            accent = Color(0xFFF0999A),
            onAccent = Color(0xFF2C1214),
            soft = Color(0xFF3A2226),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFFBF6F4),
            surface = Color(0xFFFFFDFC),
            ink = Color(0xFF2A1616),
            muted = Color(0xFF7A5D5B),
            accent = Color(0xFFB94B55),
            onAccent = Color(0xFFFFF7F7),
            soft = Color(0xFFF8E0E2),
        )
    }

    Palette.Gece -> if (dark) {
        PaletteColors(
            bg = Color(0xFF100E1C),
            surface = Color(0xFF1B1730),
            ink = Color(0xFFF1EEFE),
            muted = Color(0xFFB3ADC9),
            accent = Color(0xFFB7A6FF),
            onAccent = Color(0xFF1A1433),
            soft = Color(0xFF2A2450),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFF6F5FB),
            surface = Color(0xFFFCFBFF),
            ink = Color(0xFF1A1730),
            muted = Color(0xFF5E5A78),
            accent = Color(0xFF5B4DB8),
            onAccent = Color(0xFFF7F5FF),
            soft = Color(0xFFE4DFF8),
        )
    }

    Palette.Gul -> if (dark) {
        PaletteColors(
            bg = Color(0xFF160F13),
            surface = Color(0xFF241820),
            ink = Color(0xFFF8ECEF),
            muted = Color(0xFFC7A8B4),
            accent = Color(0xFFF09AB8),
            onAccent = Color(0xFF2C121C),
            soft = Color(0xFF3A222C),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFFBF4F6),
            surface = Color(0xFFFFFCFD),
            ink = Color(0xFF2A1620),
            muted = Color(0xFF7A5A68),
            accent = Color(0xFFC4527A),
            onAccent = Color(0xFFFFF7FA),
            soft = Color(0xFFF8DDE6),
        )
    }

    Palette.Bal -> if (dark) {
        PaletteColors(
            bg = Color(0xFF16140E),
            surface = Color(0xFF242014),
            ink = Color(0xFFF7F1E4),
            muted = Color(0xFFC4B89A),
            accent = Color(0xFFE6C36A),
            onAccent = Color(0xFF241C08),
            soft = Color(0xFF3A3220),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFFBF8EF),
            surface = Color(0xFFFFFDF8),
            ink = Color(0xFF241C10),
            muted = Color(0xFF75664A),
            accent = Color(0xFFA67C1A),
            onAccent = Color(0xFFFFFBF2),
            soft = Color(0xFFF3E8C8),
        )
    }

    Palette.Toprak -> if (dark) {
        PaletteColors(
            bg = Color(0xFF14110E),
            surface = Color(0xFF221C18),
            ink = Color(0xFFF4EDE6),
            muted = Color(0xFFB8A89C),
            accent = Color(0xFFD4A574),
            onAccent = Color(0xFF24160E),
            soft = Color(0xFF3A2E24),
        )
    } else {
        PaletteColors(
            bg = Color(0xFFF7F3EE),
            surface = Color(0xFFFFFDFB),
            ink = Color(0xFF241812),
            muted = Color(0xFF736056),
            accent = Color(0xFF8C5A3C),
            onAccent = Color(0xFFFFF8F3),
            soft = Color(0xFFE8D9CC),
        )
    }

    Palette.Sistem -> colors(Palette.Kagit, dark)
}

@Composable
fun AppTheme(
    palette: Palette,
    appearance: Appearance,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val dark = isDark(view.context, appearance)
    val dynamic = palette == Palette.Sistem && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val c = colors(palette, dark)
    val scheme = if (dynamic) {
        if (dark) dynamicDarkColorScheme(view.context) else dynamicLightColorScheme(view.context)
    } else if (dark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            background = c.bg,
            onBackground = c.ink,
            surface = c.surface,
            onSurface = c.ink,
            surfaceVariant = c.soft,
            onSurfaceVariant = c.muted,
            secondary = c.soft,
            onSecondary = c.ink,
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            background = c.bg,
            onBackground = c.ink,
            surface = c.surface,
            onSurface = c.ink,
            surfaceVariant = c.soft,
            onSurfaceVariant = c.muted,
            secondary = c.soft,
            onSecondary = c.ink,
        )
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = scheme.background.toArgb()
            window.statusBarColor = bg
            window.navigationBarColor = bg
            window.decorView.setBackgroundColor(bg)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            headlineLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                letterSpacing = (-0.6).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                letterSpacing = (-0.4).sp,
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            ),
        ),
        content = content,
    )
}
