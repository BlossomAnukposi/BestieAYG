package com.bayg.widgets

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bayg

/**
 * font size: 96dp
 */
@Composable
fun Title(text: String, colour: Color) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = colour,
        ),
    )
}

/**
 * font size: 56dp
 */
@Composable
fun Heading1(text: String, colour: Color) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 56.sp,
            lineHeight = 44.24.sp,
            fontWeight = FontWeight.Bold,
            color = colour,
        ),
    )
}

/**
 * font size: 48dp
 */
@Composable
fun Heading2(text: String, colour: Color) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = colour,
        ),
    )
}

/**
 * font size: 36dp
 */
@Composable
fun Heading3(text: String, colour: Color) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = colour,
        ),
    )
}

/**
 * font size: 28dp
 */
@Composable
fun Heading4(text: String, colour: Color) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colour,
        ),
    )
}


@Composable
fun Subtitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.bayg.textGrey,
        ),
    )
}

@Composable
fun Paragraph(text: String, color: Color = MaterialTheme.bayg.white, bold: Boolean = false) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = if(bold) FontWeight.Bold else FontWeight.Normal,
            color = color,
        ),
    )
}

/**
 * Overload: Caption with Color as second argument
 */
@Composable
fun Caption(text: String, color: Color) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Start
        ),
        softWrap = true,
        modifier = Modifier
    )
}

/**
 * Original overload: Caption with Dp width and TextAlign
 */
@Composable
fun Caption(text: String, width: Dp = 0.dp, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.bayg.textGrey,
            textAlign = align
        ),
        softWrap = true,
        modifier = if (width > 0.dp) { Modifier.width(width) } else Modifier
    )
}