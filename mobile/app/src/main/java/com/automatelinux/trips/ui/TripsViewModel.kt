package com.automatelinux.trips.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automatelinux.trips.data.TripRepository
import com.automatelinux.trips.geo.LatLon
import com.automatelinux.trips.geo.RouteLine
import com.automatelinux.trips.geo.bearingDeg
import com.automatelinux.trips.geo.estimateMinutes
import com.automatelinux.trips.location.LocationSource
import com.automatelinux.trips.model.Trip
import com.automatelinux.trips.model.Waypoint
import com.automatelinux.trips.util.ScreenTracker
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: TripRepository,
    private val location: LocationSource,
) : ViewModel(), TripsActions {

    /** Things only the Activity can do: ask the system, move the map. */
    sealed interface Effect {
        data object AskLocationPermission : Effect
        data object FitTrip : Effect
        data class CenterOn(val lat: Double, val lon: Double) : Effect
        data object FollowMe : Effect
    }

    private val _state = MutableStateFlow(TripsUiState())
    val state: StateFlow<TripsUiState> = _state.asStateFlow()
    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 8)
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    private val prefs = context.getSharedPreferences("trips", Context.MODE_PRIVATE)
    private var route: RouteLine? = null
    private var lastChainage: Double? = null
    private var lastFix: Location? = null
    private var locationJob: Job? = null
    private var headingJob: Job? = null

    init {
        viewModelScope.launch {
            val trips = repository.all()
            _state.update { it.copy(trips = trips) }
            // Back to the trip you were on — the app is opened mid-walk far more often than from the sofa.
            prefs.getString(KEY_LAST_TRIP, null)?.let { id -> if (trips.any { it.id == id }) openTrip(id) }
        }
        _state.update { it.copy(location = if (location.hasPermission()) LocationState.Waiting else LocationState.NoPermission) }
        if (location.hasPermission()) startLocation()
        viewModelScope.launch {
            // The fix's age is part of the read-out ("אות ישן"); tick it without a new fix.
            while (true) {
                delay(10_000)
                val fix = lastFix ?: continue
                _state.update { s -> (s.location as? LocationState.Fix)?.let { s.copy(location = it.copy(ageS = ageOf(fix))) } ?: s }
            }
        }
    }

    // ---- actions -----------------------------------------------------------------

    override fun openTrip(id: String) {
        val trip = _state.value.trips.firstOrNull { it.id == id } ?: return
        route = RouteLine(trip)
        lastChainage = null
        prefs.edit().putString(KEY_LAST_TRIP, id).apply()
        ScreenTracker.currentScreen = trip.name
        _state.update { it.copy(selected = trip, progress = null, follow = false, focusedWaypointId = null) }
        recompute()
        _effects.tryEmit(Effect.FitTrip)
    }

    override fun closeTrip() {
        route = null
        prefs.edit().remove(KEY_LAST_TRIP).apply()
        ScreenTracker.currentScreen = "רשימת טיולים"
        _state.update { it.copy(selected = null, progress = null, follow = false, focusedWaypointId = null) }
    }

    override fun askLocation() {
        _effects.tryEmit(Effect.AskLocationPermission)
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _state.update { it.copy(location = LocationState.Waiting) }
            startLocation()
        } else {
            _state.update { it.copy(location = LocationState.NoPermission) }
        }
    }

    override fun toggleFollow() {
        val on = !_state.value.follow
        _state.update { it.copy(follow = on, focusedWaypointId = null) }
        if (on) _effects.tryEmit(Effect.FollowMe)
    }

    override fun fitTrip() {
        _state.update { it.copy(follow = false, focusedWaypointId = null) }
        _effects.tryEmit(Effect.FitTrip)
    }

    override fun focusWaypoint(waypoint: Waypoint) {
        _state.update { it.copy(follow = false, focusedWaypointId = waypoint.id) }
        _effects.tryEmit(Effect.CenterOn(waypoint.lat, waypoint.lon))
    }

    override fun clearFocus() {
        _state.update { it.copy(focusedWaypointId = null) }
    }

    /** The user moved the map: stop dragging it back under the puck. */
    fun onUserGesture() {
        if (_state.value.follow) _state.update { it.copy(follow = false) }
    }

    // ---- location ------------------------------------------------------------------

    private fun startLocation() {
        locationJob?.cancel()
        headingJob?.cancel()
        if (!location.anyProviderEnabled()) {
            _state.update { it.copy(location = LocationState.Off) }
            return
        }
        locationJob = viewModelScope.launch {
            location.positions().collect { fix ->
                lastFix = fix
                _state.update { it.copy(location = LocationState.Fix(fix.latitude, fix.longitude, fix.accuracy, ageOf(fix))) }
                recompute()
            }
        }
        headingJob = viewModelScope.launch {
            location.headings { lastFix }.collect { h -> _state.update { it.copy(headingDeg = h) } }
        }
    }

    private fun ageOf(fix: Location): Long = ((System.currentTimeMillis() - fix.time) / 1000).coerceAtLeast(0)

    private fun recompute() {
        val r = route ?: return
        val trip = _state.value.selected ?: return
        val fix = lastFix ?: return
        val here = LatLon(fix.latitude, fix.longitude)
        val at = r.locate(here, lastChainage)
        lastChainage = at.chainageM
        val remaining = (r.totalM - at.chainageM).coerceAtLeast(0.0)
        val gain = r.remainingGainM(at.chainageM)
        val next = nextWaypoint(trip, at.chainageM)
        _state.update {
            it.copy(
                progress = Progress(
                    chainageM = at.chainageM,
                    offRouteM = at.offRouteM,
                    remainingM = remaining,
                    remainingGainM = gain,
                    etaMin = estimateMinutes(remaining, gain),
                    elevationM = r.elevationAt(at.chainageM),
                    nextWaypoint = next,
                    toNextM = next?.let { w -> (w.atM - at.chainageM).coerceAtLeast(0.0) } ?: 0.0,
                    bearingToRoute = bearingDeg(here, at.onRoute),
                ),
            )
        }
    }

    /** The first waypoint still ahead — with a little slack, so a point you are standing on stays "now". */
    private fun nextWaypoint(trip: Trip, chainageM: Double): Waypoint? =
        trip.waypoints.firstOrNull { it.atM > chainageM - 25 && it.atM > 0 }

    private companion object { const val KEY_LAST_TRIP = "lastTrip" }
}
