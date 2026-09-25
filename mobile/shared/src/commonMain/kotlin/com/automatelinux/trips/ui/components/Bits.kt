package com.automatelinux.trips.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.automatelinux.trips.ui.theme.Tp

/**
 * An Israeli trail blaze: three horizontal stripes, white–colour–white, as painted on
 * rocks along every marked trail in the country. It is the app's alphabet — a walker
 * reads "green" faster than any word.
 */
@Composable
fun TrailBlaze(marker: String, modifier: Modifier = Modifier, height: Dp = 22.dp, width: Dp = 14.dp) {
    val colour = Tp.marker(marker)
    if (marker == "road") {
        Box(
            modifier.size(width, height).clip(RoundedCornerShape(3.dp)).background(Tp.Road.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(width - 6.dp).height(2.dp).background(Tp.Road))
        }
        return
    }
    Column(
        modifier.size(width, height).clip(RoundedCornerShape(3.dp)).background(Color(0xFF8A7358)).padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(Modifier.weight(1f).fillMaxSize().background(Color.White))
        Box(Modifier.weight(1f).fillMaxSize().background(colour))
        Box(Modifier.weight(1f).fillMaxSize().background(Color.White))
    }
}

@Composable
fun BlazeRow(markers: List<String>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        markers.forEach { TrailBlaze(it) }
    }
}

/** A number with its meaning under it: the read-out language of the whole app. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, accent: Color = Tp.Ink, icon: ImageVector? = null) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Tp.Muted, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, bg: Color = Tp.SandDeep, fg: Color = Tp.Ink) {
    Row(
        modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun Dot(colour: Color, size: Dp = 10.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(colour))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Tp.Ink, modifier = modifier.padding(top = 22.dp, bottom = 10.dp))
    Spacer(Modifier.height(0.dp))
}
