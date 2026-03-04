package com.example.simplenotepad.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    secondary = Color(0xFF81C784),
    background = Color(0xFFF7F9F7),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun SimpleNotepadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)
        ),
        content = content
    )
}
