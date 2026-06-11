package com.bayg.widgets

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import bayg
import kotlin.math.roundToInt

@Composable
fun GreenButton(navController: NavController, route: String, text: String, height: Dp = 56.dp, color: Color = MaterialTheme.bayg.green) {
    Button(
        onClick = {navController.navigate(route)},
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(size = 5.dp),
        modifier = Modifier
            .width(334.dp)
            .height(height)
    ) { Text(text,
        color = MaterialTheme.bayg.black,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
    ) }
}

@Composable
fun GreenButton(
    onClick: () -> Unit,
    text: String,
    height: Dp = 56.dp,
    color: Color = MaterialTheme.bayg.green,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(size = 5.dp),
        modifier = Modifier
            .width(334.dp)
            .height(height)
    ) { Text(text,
        color = MaterialTheme.bayg.black,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
    ) }
}

@Composable
fun GreenArrowButton(navController: NavController, route: String) {
    TextButton(
        onClick = {navController.navigate(route)},
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .width(50.dp)
            .height(50.dp)
    ) {
        Text(
            text = "←",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.bayg.green,
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitSlider(color: Color, value: Float, steps: Int, range: ClosedFloatingPointRange<Float>,
                valueChange: (Float) -> Unit) {
    var sliderValue by remember { mutableFloatStateOf(value) }
    val stepSize = (range.endInclusive - range.start) / (steps + 1)

    Slider(
        value = sliderValue,
        onValueChange = {
            sliderValue = (it / stepSize).roundToInt() * stepSize
            valueChange(sliderValue)},
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = remember { MutableInteractionSource() },
                thumbSize = DpSize(20.dp, 20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                ),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                trackInsideCornerSize = 0.dp,
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(4.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = color,
                    inactiveTrackColor = MaterialTheme.bayg.outline,
                ),
            )
        },
        steps = 0,
        valueRange = range,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ProgressBar(color: Color, value: Float) {
    LinearProgressIndicator(
        progress = value,
        color = color,
        trackColor = MaterialTheme.bayg.card,
        modifier = Modifier.width(334.dp)
    )
}

@Composable
fun Toggle(checkedValue: Boolean) {
    var toggleValue by remember { mutableStateOf(checkedValue) }

    Switch(
        checked = toggleValue,
        onCheckedChange = { toggleValue = it },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.bayg.black,
            uncheckedThumbColor = MaterialTheme.bayg.card,
            checkedTrackColor = MaterialTheme.bayg.green,
            uncheckedTrackColor = MaterialTheme.bayg.textGrey
        ),
        modifier = Modifier.width(36.dp)
    )
}

@Composable
fun PermissionToggle(isGranted: Boolean, onToggle: () -> Unit) {
    Switch(
        checked = isGranted,
        onCheckedChange = { onToggle() },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.bayg.black,
            uncheckedThumbColor = MaterialTheme.bayg.card,
            checkedTrackColor = MaterialTheme.bayg.green,
            uncheckedTrackColor = MaterialTheme.bayg.textGrey
        ),
        modifier = Modifier.width(36.dp),
        enabled = !isGranted // Disable toggle once permission is granted (optional)
    )
}
