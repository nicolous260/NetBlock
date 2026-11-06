package com.example.netblock

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NetBlockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF9ECAFE),
            onPrimary = Color(0xFF00325B),
            primaryContainer = Color(0xFF00497D),
            onPrimaryContainer = Color(0xFFD1E4FF),
            secondary = Color(0xFFBBC7DB),
            onSecondary = Color(0xFF253140),
            secondaryContainer = Color(0xFF3B4858),
            onSecondaryContainer = Color(0xFFD7E3F8),
            background = Color(0xFF111318),
            onBackground = Color(0xFFE2E2E6),
            surface = Color(0xFF111318),
            onSurface = Color(0xFFE2E2E6),
            surfaceContainerLow = Color(0xFF1A1C22),
            surfaceVariant = Color(0xFF42474E),
            onSurfaceVariant = Color(0xFFC2C7CF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0061A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1E4FF),
            onPrimaryContainer = Color(0xFF001D36),
            secondary = Color(0xFF535F70),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD7E3F8),
            onSecondaryContainer = Color(0xFF0F1D2A),
            background = Color(0xFFFDFCFF),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFFDFCFF),
            onSurface = Color(0xFF1A1C1E),
            surfaceContainerLow = Color(0xFFF0F4F9),
            surfaceVariant = Color(0xFFDFE2EB),
            onSurfaceVariant = Color(0xFF42474E),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
