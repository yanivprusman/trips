package com.automatelinux.trips.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Signpost
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.automatelinux.trips.geo.compassWord
import com.automatelinux.trips.geo.formatDistance
import com.automatelinux.trips.geo.formatDuration
import com.automatelinux.trips.model.Trip
import com.automatelinux.trips.model.Waypoint
import com.automatelinux.trips.ui.components.BlazeRow
import com.automatelinux.trips.ui.components.Chip
import com.automatelinux.trips.ui.components.Dot
import com.automatelinux.trips.ui.components.ElevationProfile
import com.automatelinux.trips.ui.components.SectionTitle
import com.automatelinux.trips.ui.components.SegmentBar
import com.automatelinux.trips.ui.components.Stat
import com.automatelinux.trips.ui.theme.Tp
import kotlinx.coroutines.launch

private val PEEK_FALLBACK = 292.dp
private val HANDLE = 24.dp

/**
 * The trip: the map fills the screen; a sheet at the bottom reads it out — how far you
 * have come, what is next, the climb ahead — and pulls up into the whole guide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(
    state: TripsUiState,
    trip: Trip,
    actions: TripsActions,
    onMapInsets: (topPx: Int, bottomPx: Int) -> Unit,
    map: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // The closed sheet shows exactly the read-out block (stats, bar, next, profile): it is
    // measured, not guessed, so a taller "next" row or a different font never leaks the
    // guide's first line under the edge.
    var readoutPx by remember { mutableStateOf(0) }
    val peek = (if (readoutPx > 0) with(density) { readoutPx.toDp() } else PEEK_FALLBACK + navBottom) + HANDLE
    LaunchedEffect(peek, statusTop) {
        onMapInsets(with(density) { (statusTop + 72.dp).roundToPx() }, with(density) { (peek + 8.dp).roundToPx() })
    }
    val scaffold = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    val progress = state.progress

    BottomSheetScaffold(
        scaffoldState = scaffold,
        sheetPeekHeight = peek,
        sheetContainerColor = Tp.Paper,
        sheetShadowElevation = 16.dp,
        sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        sheetDragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 2.dp).size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(Tp.SandDeep))
        },
        sheetContent = {
            // Fully open, the sheet reaches the top of the screen: give the status bar its room.
            val expanded = scaffold.bottomSheetState.currentValue == SheetValue.Expanded
            val topInset by animateDpAsState(if (expanded) statusTop else 0.dp, label = "sheet-top")
            Spacer(Modifier.height(topInset))
            SheetContent(
                state = state, trip = trip, actions = actions, navBottom = navBottom,
                onReadoutMeasured = { readoutPx = it },
                onWaypointTap = { wp ->
                    actions.focusWaypoint(wp)
                    scope.launch { scaffold.bottomSheetState.partialExpand() }
                },
            )
        },
        containerColor = Tp.Sand,
    ) {
        Box(Modifier.fillMaxSize()) {
            map(Modifier.fillMaxSize())

            // top: back, name, GPS
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassButton(onClick = actions::closeTrip) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, "חזרה", tint = Tp.Ink)
                }
                Surface(color = Tp.PaperGlass, shape = RoundedCornerShape(999.dp), shadowElevation = 3.dp) {
                    Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(trip.name, style = MaterialTheme.typography.titleMedium, color = Tp.Ink, maxLines = 1)
                        BlazeRow(trip.markers)
                    }
                }
                Spacer(Modifier.weight(1f))
                GpsPill(state.location, onClick = { if (state.location is LocationState.NoPermission) actions.askLocation() })
            }

            // map controls, sitting on the sheet's edge
            Column(
                Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = peek + 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassButton(
                    onClick = { if (state.location is LocationState.NoPermission) actions.askLocation() else actions.toggleFollow() },
                    filled = state.follow,
                ) {
                    Icon(Icons.Rounded.MyLocation, "המיקום שלי", tint = if (state.follow) Color.White else Tp.Ink)
                }
                GlassButton(onClick = actions::fitTrip) {
                    Icon(Icons.Rounded.ZoomOutMap, "כל המסלול", tint = Tp.Ink)
                }
            }

        }
    }
}

@Composable
private fun GlassButton(onClick: () -> Unit, filled: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (filled) Tp.Terracotta else Tp.PaperGlass,
        shadowElevation = 4.dp,
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun GpsPill(location: LocationState, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "gps")
    val a by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "gps-alpha")
    val (text, colour, pulsing) = when (location) {
        LocationState.NoPermission -> Triple("איפה אני?", Tp.Terracotta, false)
        LocationState.Off -> Triple("מיקום כבוי", Tp.Muted, false)
        LocationState.Waiting -> Triple("מחפש לוויינים", Tp.Warn, true)
        is LocationState.Fix -> Triple(
            if (location.ageS > 90) "אות ישן" else "±${location.accuracyM.toInt()} מ׳",
            if (location.ageS > 90) Tp.Warn else Tp.Ok, false,
        )
    }
    Surface(onClick = onClick, color = Tp.PaperGlass, shape = RoundedCornerShape(999.dp), shadowElevation = 3.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dot(colour, 8.dp, Modifier.alpha(if (pulsing) a else 1f))
            Text(text, style = MaterialTheme.typography.labelMedium, color = Tp.Ink, maxLines = 1)
        }
    }
}

@Composable
private fun SheetContent(
    state: TripsUiState,
    trip: Trip,
    actions: TripsActions,
    navBottom: androidx.compose.ui.unit.Dp,
    onReadoutMeasured: (Int) -> Unit,
    onWaypointTap: (Waypoint) -> Unit,
) {
    val progress = state.progress
    val loader = LocalAssetImage.current
    val density = LocalDensity.current
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = navBottom + 28.dp),
    ) {
        // ---- what the sheet shows while closed ------------------------------------
        Column(Modifier.fillMaxWidth().onSizeChanged { onReadoutMeasured(it.height) }) {
        Spacer(Modifier.height(6.dp))
        when {
            progress != null && progress.finished -> {
                Text("סיימת את המסלול", style = MaterialTheme.typography.headlineMedium, color = Tp.Ink)
                Text("${formatDistance(trip.lengthM)} · ${formatDuration(trip.durationMin)} · חזרה לחניון", style = MaterialTheme.typography.bodyMedium, color = Tp.Muted)
            }
            progress != null -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(formatDistance(progress.chainageM), "עברת")
                Stat(formatDistance(progress.remainingM), "נשארו")
                Stat(formatDuration(progress.etaMin), "עוד")
                Stat("↑${progress.remainingGainM.toInt()}", "מ׳ לטפס", accent = if (progress.remainingGainM > 50) Tp.Terracotta else Tp.Ink)
            }
            else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(formatDistance(trip.lengthM), "אורך")
                Stat(formatDuration(trip.durationMin), "משך")
                Stat("↑${trip.gainM}", "מ׳ עלייה")
                Stat("↓${trip.lossM}", "מ׳ ירידה")
            }
        }
        Spacer(Modifier.height(14.dp))
        SegmentBar(trip, progress?.chainageM)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
            when {
                progress != null && progress.offRoute -> OffRouteRow(progress.offRouteM, progress.bearingToRoute)
                progress != null && progress.nextWaypoint != null -> NextRow(trip, progress.nextWaypoint, progress.toNextM)
                progress != null -> Text("ממשיכים ישר עד סוף המסלול", style = MaterialTheme.typography.bodyLarge, color = Tp.Muted)
                else -> LocationRow(state.location, actions::askLocation)
            }
        }
        Spacer(Modifier.height(6.dp))
        ElevationProfile(trip, progress?.chainageM)
        // The system bar is transparent; this keeps the guide's first line from showing
        // through it while the sheet is closed.
        Spacer(Modifier.height(navBottom + 8.dp))
        }

        // ---- the guide, once pulled up ---------------------------------------------
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tp.Line))
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(if (trip.isLoop) "מעגלי" else "קווי")
            Chip(trip.difficulty)
            Chip("${trip.minEleM}–${trip.maxEleM} מ׳ גובה")
        }
        Spacer(Modifier.height(14.dp))
        Text(trip.summary, style = MaterialTheme.typography.bodyLarge, color = Tp.Ink)

        SectionTitle("נקודות במסלול")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            trip.waypoints.forEachIndexed { i, wp ->
                WaypointRow(trip, i + 1, wp, current = progress?.nextWaypoint?.id == wp.id, passed = progress != null && progress.chainageM > wp.atM + 30, focused = state.focusedWaypointId == wp.id) { onWaypointTap(wp) }
            }
        }

        SectionTitle("המסלול")
        trip.description.forEach { p ->
            Text(p, style = MaterialTheme.typography.bodyLarge, color = Tp.Ink, modifier = Modifier.padding(bottom = 12.dp))
        }
        trip.linear?.let { alt ->
            Surface(color = Tp.SandDeep.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("אפשרות קווית · ${formatDistance(alt.lengthM)} · ${formatDuration(alt.durationMin)}", style = MaterialTheme.typography.titleMedium, color = Tp.Ink)
                    Text(alt.text, style = MaterialTheme.typography.bodyMedium, color = Tp.Muted)
                }
            }
        }

        SectionTitle("כדאי לדעת")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            trip.practical.forEach { p ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Tp.TerracottaSoft), contentAlignment = Alignment.Center) {
                        Icon(practicalIcon(p.icon), null, tint = Tp.Terracotta, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(p.title, style = MaterialTheme.typography.titleMedium, color = Tp.Ink)
                        Text(p.text, style = MaterialTheme.typography.bodyMedium, color = Tp.Muted)
                    }
                }
            }
        }

        if (trip.photos.isNotEmpty()) {
            SectionTitle("בדרך")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
                items(trip.photos, key = { it.file }) { photo ->
                    Column(Modifier.width(240.dp)) {
                        val bmp = loader(photo.file, with(density) { 720.dp.roundToPx() })
                        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp)).background(Tp.SandDeep)) {
                            if (bmp != null) Image(bmp, photo.caption, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Text(photo.caption, style = MaterialTheme.typography.labelMedium, color = Tp.Muted, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        if (trip.rules.isNotEmpty()) {
            SectionTitle("כללי השמורה")
            trip.rules.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    Dot(Tp.Ok, 7.dp); Text(r, style = MaterialTheme.typography.bodyMedium, color = Tp.Ink)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        trip.credits.forEach { c -> Text(c, style = MaterialTheme.typography.labelSmall, color = Tp.Faint, modifier = Modifier.padding(bottom = 3.dp)) }
    }
}

@Composable
private fun NextRow(trip: Trip, wp: Waypoint, toNextM: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Tp.marker(markerAt(trip, wp.atM))), contentAlignment = Alignment.Center) {
            Icon(waypointIcon(wp.kind), null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                if (toNextM < 25) "עכשיו" else "בעוד ${formatDistance(toNextM)}",
                style = MaterialTheme.typography.labelMedium, color = Tp.Terracotta, fontWeight = FontWeight.Bold,
            )
            Text(wp.title, style = MaterialTheme.typography.titleMedium, color = Tp.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OffRouteRow(offM: Double, bearing: Double) {
    Surface(color = Tp.WarnSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Hiking, null, tint = Tp.Warn)
            Column {
                Text("אתה ${formatDistance(offM)} מהשביל", style = MaterialTheme.typography.titleMedium, color = Tp.Warn)
                Text("השביל ${compassWord(bearing).let { if (it.startsWith("צ") || it.startsWith("ד") || it.startsWith("מ")) "ב$it" else it }} ממך", style = MaterialTheme.typography.bodyMedium, color = Tp.Ink)
            }
        }
    }
}

@Composable
private fun LocationRow(location: LocationState, onAsk: () -> Unit) {
    when (location) {
        LocationState.NoPermission -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("איפה אתה על המסלול?", style = MaterialTheme.typography.titleMedium, color = Tp.Ink)
                Text("עם מיקום תראה כמה עברת ומה הלאה", style = MaterialTheme.typography.bodyMedium, color = Tp.Muted)
            }
            Button(onClick = onAsk, colors = ButtonDefaults.buttonColors(containerColor = Tp.Terracotta, contentColor = Color.White), shape = RoundedCornerShape(999.dp)) {
                Text("אפשר מיקום", style = MaterialTheme.typography.labelLarge)
            }
        }
        LocationState.Off -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.LocationOff, null, tint = Tp.Muted)
            Column {
                Text("המיקום כבוי בטלפון", style = MaterialTheme.typography.titleMedium, color = Tp.Ink)
                Text("הדליקו אותו בהגדרות כדי לראות איפה אתם", style = MaterialTheme.typography.bodyMedium, color = Tp.Muted)
            }
        }
        LocationState.Waiting -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val pulse = rememberInfiniteTransition(label = "wait")
            val a by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "wait-alpha")
            Dot(Tp.Warn, 12.dp, Modifier.alpha(a))
            Column {
                Text("מחפש לוויינים…", style = MaterialTheme.typography.titleMedium, color = Tp.Ink)
                Text("בערוץ צר זה יכול לקחת דקה. השמיים פתוחים — זה יגיע.", style = MaterialTheme.typography.bodyMedium, color = Tp.Muted)
            }
        }
        is LocationState.Fix -> Text("מחשב איפה אתה על המסלול…", style = MaterialTheme.typography.bodyLarge, color = Tp.Muted)
    }
}

@Composable
private fun WaypointRow(trip: Trip, n: Int, wp: Waypoint, current: Boolean, passed: Boolean, focused: Boolean, onClick: () -> Unit) {
    val colour = Tp.marker(markerAt(trip, wp.atM))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (focused) Tp.TerracottaSoft.copy(alpha = 0.6f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp).alpha(if (passed) 0.55f else 1f),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(if (current) Tp.Terracotta else colour), contentAlignment = Alignment.Center) {
            Text("$n", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(wp.title, style = MaterialTheme.typography.titleMedium, color = Tp.Ink, modifier = Modifier.weight(1f, fill = false))
                Text(if (wp.atM == 0) "התחלה" else "ב־${formatDistance(wp.atM)}", style = MaterialTheme.typography.labelMedium, color = Tp.Muted, modifier = Modifier.padding(start = 8.dp))
            }
            Text(wp.text, style = MaterialTheme.typography.bodyMedium, color = Tp.Muted, maxLines = if (focused || current) 8 else 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Which blaze you are following at metre [atM] of the trip. */
fun markerAt(trip: Trip, atM: Int): String {
    var acc = 0
    for (s in trip.segments) {
        acc += s.lengthM
        if (atM < acc) return s.marker
    }
    return trip.segments.lastOrNull()?.marker ?: "road"
}

fun waypointIcon(kind: String): ImageVector = when (kind) {
    "start" -> Icons.Rounded.Flag
    "view" -> Icons.Rounded.Visibility
    "nature" -> Icons.Rounded.Landscape
    "junction" -> Icons.AutoMirrored.Rounded.CallSplit
    "road" -> Icons.Rounded.Signpost
    "climb" -> Icons.AutoMirrored.Rounded.TrendingUp
    "finish" -> Icons.Rounded.SportsScore
    "parking" -> Icons.Rounded.LocalParking
    else -> Icons.Rounded.Landscape
}

private fun practicalIcon(name: String): ImageVector = when (name) {
    "clock" -> Icons.Rounded.Schedule
    "water" -> Icons.Rounded.WaterDrop
    "shoes" -> Icons.Rounded.Hiking
    "season" -> Icons.Rounded.WbSunny
    "family" -> Icons.Rounded.FamilyRestroom
    "parking" -> Icons.Rounded.LocalParking
    else -> Icons.Rounded.Landscape
}
