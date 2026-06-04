package com.bayg.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bayg

@Composable
fun GreyOutlinedCard(minHeight: Dp = 94.dp, content: @Composable () -> Unit) {
    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
        border = BorderStroke(1.dp, MaterialTheme.bayg.outline),
        shape = RoundedCornerShape(size = 5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).defaultMinSize(minHeight = minHeight)) {
            content()
        }
    }
}

@Composable
fun GreyCard(height: Dp = 52.dp, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.card),
        shape = RoundedCornerShape(size = 5.dp),
        modifier = Modifier.size(width = 334.dp, height = height)
    ) {
        Column (modifier = Modifier.padding(5.dp)){
            content()
        }
    }
}

@Composable
fun ToggleCard(text: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.bayg.green else MaterialTheme.bayg.card
        ),
        shape = RoundedCornerShape(size = 5.dp),
        modifier = Modifier.size(width = 163.dp, height = 44.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Paragraph(
                text,
                if (selected) MaterialTheme.bayg.black else MaterialTheme.bayg.textGrey,
                true
            )
        }
    }
}

@Composable
fun RedTagCard(text: String) {
    Card (
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.darkRed),
        shape = RoundedCornerShape(size = 2.dp),
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.bayg.lightRed,
            ),
            modifier = Modifier
                .padding(5.dp)
        )
    }
}

@Composable
fun OrangeTagCard(text: String) {
    Card (
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.bayg.darkOrange),
        shape = RoundedCornerShape(size = 2.dp),
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.bayg.lightOrange,
            ),
            modifier = Modifier
                .padding(5.dp)
        )
    }
}

@Composable
fun SelectableCard(text: String, initialSelected: Boolean = false) {
    var selected by remember {
        mutableStateOf(initialSelected)
    }

    val backgroundColor =
        if (selected) MaterialTheme.bayg.darkGreen
        else MaterialTheme.bayg.card

    val borderColor =
        if (selected) MaterialTheme.bayg.green
        else MaterialTheme.bayg.outline

    val textColor =
        if (selected) MaterialTheme.bayg.green
        else MaterialTheme.bayg.textGrey

    OutlinedCard(
        onClick = { selected = !selected },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            ),
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun SmallInfoCard(
    number: String,
    title: String,
    body: List<String>,
    footer: String
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.bayg.card)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2B2B)),
                    contentAlignment = Alignment.Center
                ) {
                    Heading4(number, MaterialTheme.bayg.lightRed)
                }

                Spacer(modifier = Modifier.width(12.dp))
                Paragraph(title, bold = true, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            for (text in body.take(2)) {
                Text(
                    text,
                    color = MaterialTheme.bayg.white,
                    fontSize = 13.sp,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }

            Caption(footer, fontStyle = FontStyle.Italic)
        }
    }
}