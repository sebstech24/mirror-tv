package io.github.jqssun.airplay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.github.jqssun.airplay.ui.isTv

// Apple's dark system colors (tvOS/iOS): neutral greys instead of Material's tinted ones, system blue accent
private val AppleDark = darkColorScheme(
    primary = Color(0xFF0A84FF), onPrimary = Color.White,
    // focused cards turn white with dark text, like a focused tile on Apple TV
    primaryContainer = Color.White, onPrimaryContainer = Color.Black,
    secondary = Color(0xFF98989F), onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C2C2E), onSecondaryContainer = Color.White,
    tertiary = Color(0xFF30D158), onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1E3A26), onTertiaryContainer = Color(0xFF30D158),
    background = Color.Black, onBackground = Color.White,
    surface = Color(0xFF000000), onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0x99EBEBF5),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF111113),
    surfaceContainer = Color(0xFF1C1C1E), surfaceContainerHigh = Color(0xFF242426),
    surfaceContainerHighest = Color(0xFF2C2C2E),
    outline = Color(0xFF48484A), outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF453A), onError = Color.White,
)

// Material spaces letters out (up to +0.5sp); Apple tightens big text and keeps body text near zero
private val AppleTracking = Typography().run {
    fun TextStyle.t(sp: Double) = copy(letterSpacing = sp.sp)
    copy(
        displayLarge = displayLarge.t(-1.0), displayMedium = displayMedium.t(-0.8), displaySmall = displaySmall.t(-0.6),
        headlineLarge = headlineLarge.t(-0.5), headlineMedium = headlineMedium.t(-0.4), headlineSmall = headlineSmall.t(-0.3),
        titleLarge = titleLarge.t(-0.3), titleMedium = titleMedium.t(-0.2), titleSmall = titleSmall.t(-0.1),
        bodyLarge = bodyLarge.t(-0.2), bodyMedium = bodyMedium.t(-0.1), bodySmall = bodySmall.t(0.0),
        labelLarge = labelLarge.t(-0.1), labelMedium = labelMedium.t(0.0), labelSmall = labelSmall.t(0.0),
    )
}

@Composable
fun AirPlayTheme(
    // TVs always get the dark look (no bright flash in a dark room, upstream #8/#26)
    darkTheme: Boolean = isSystemInDarkTheme() || isTv(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> AppleDark
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(LocalContext.current)
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme, typography = AppleTracking, content = content)
}
