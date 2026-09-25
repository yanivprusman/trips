package com.automatelinux.trips.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automatelinux.trips.geo.LatLon
import com.automatelinux.trips.geo.distanceM
import com.automatelinux.trips.geo.formatDistance
import com.automatelinux.trips.geo.formatDuration
import com.automatelinux.trips.model.Trip
import com.automatelinux.trips.ui.components.BlazeRow
import com.automatelinux.trips.ui.components.Chip
import com.automatelinux.trips.ui.components.Stat
import com.automatelinux.trips.ui.theme.Tp

/** Home: every trip, each as one big photographic card. One tap opens the map. */
@Composable
fun TripListScreen(state: TripsUiState, actions: TripsActions) {
    val here = (state.location as? LocationState.Fix)?.let { LatLon(it.lat, it.lon) }
    Surface(Modifier.fillMaxSize(), color = Tp.Sand) {
        LazyColumn(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column {
                    Text("טיולים", style = MaterialTheme.typography.displayMedium, color = Tp.Ink)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.OfflinePin, null, tint = Tp.Ok, modifier = Modifier.height(18.dp))
                        Text("המסלולים והמפה שמורים בטלפון — עובד גם בלי קליטה", style = MaterialTheme.typography.bodyMedium, color = Tp.Muted)
                    }
                }
            }
            items(state.trips, key = { it.id }) { trip ->
                val away = here?.let { distanceM(it, LatLon(trip.startLat, trip.startLon)) }
                TripCard(trip, away) { actions.openTrip(trip.id) }
            }
            item { Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun TripCard(trip: Trip, awayM: Double?, onClick: () -> Unit) {
    val density = LocalDensity.current
    val loader = LocalAssetImage.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Tp.Paper).clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 11f)) {
            val hero = trip.hero
            val bmp = if (hero != null) loader(hero.file, with(density) { 1200.dp.roundToPx() }) else null
            if (bmp != null) {
                Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Tp.SandDeep))
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Transparent, 1f to Color(0xCC1F1A15)),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text(trip.name, style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(trip.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
            if (awayM != null && awayM < 3000) {
                Chip(
                    if (awayM < 400) "אתה כאן" else "במרחק ${formatDistance(awayM)}",
                    Modifier.align(Alignment.TopStart).padding(14.dp),
                    icon = Icons.Rounded.NearMe, bg = Tp.Paper, fg = Tp.Terracotta,
                )
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Stat(formatDistance(trip.lengthM), "אורך")
                Stat(formatDuration(trip.durationMin), "משך")
                Stat("↑${trip.gainM}", "מ׳ עלייה")
                BlazeRow(trip.markers, Modifier.padding(top = 4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(if (trip.isLoop) "מעגלי" else "קווי")
                Chip(trip.difficulty)
                if (trip.family) Chip("מתאים למשפחות")
            }
            Text(trip.summary, style = MaterialTheme.typography.bodyMedium, color = Tp.Muted, maxLines = 3)
        }
    }
}
