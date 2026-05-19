import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BAYGColors(
    val green: Color,
    val black: Color,
    val darkGreen: Color,
    val card: Color,
    val textGrey: Color,
    val white: Color,
    val outline: Color,
    val lightRed: Color,
    val darkRed: Color,
    val lightOrange: Color,
    val darkOrange: Color,
)

val LocalBAYGColors = staticCompositionLocalOf<BAYGColors> {
    error("No BAYGColors provided")
}

val MaterialTheme.bayg: BAYGColors
    @Composable
    get() = LocalBAYGColors.current

private val baygColors = BAYGColors(
    green = Green,
    black = Black,
    darkGreen = DarkGreen,
    lightRed = LightRed,
    darkRed = DarkRed,
    lightOrange = LightOrange,
    darkOrange = DarkOrange,
    card = CardGrey,
    white = White,
    textGrey = TextGrey,
    outline = OutlineGrey,
)

@Composable
fun BAYGTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBAYGColors provides baygColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
