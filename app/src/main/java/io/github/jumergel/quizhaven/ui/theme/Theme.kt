package io.github.jumergel.quizhaven.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Tan,
    onPrimary = Ivory,
    background = Ivory,
    surface = Color.White.copy(alpha = 0.95f),
    onBackground = Cedar,
    onSurface = Cedar
)

@Composable
fun QuizHavenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
