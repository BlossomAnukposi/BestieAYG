package com.bayg.widgets

import android.graphics.fonts.Font
import android.icu.number.IntegerWidth
import android.icu.text.CaseMap
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
fun Paragraph(text: String, color: Color = MaterialTheme.bayg.white, bold: Boolean = false, fontSize: TextUnit = 16.sp) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = if(bold) FontWeight.Bold else FontWeight.Normal,
            color = color,
        ),
    )
}

@Composable
fun Caption(text: String, width: Dp = 0.dp, align: TextAlign = TextAlign.Start, fontStyle: FontStyle = FontStyle.Normal) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.bayg.textGrey,
            textAlign = align,
            fontStyle = fontStyle
        ),
        softWrap = true,
        modifier = if (width > 0.dp) {Modifier.width(width)} else Modifier
    )
}