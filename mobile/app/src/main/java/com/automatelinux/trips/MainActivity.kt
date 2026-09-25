package com.automatelinux.trips

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.automatelinux.trips.ui.LocalAssetImage
import com.automatelinux.trips.ui.LocationState
import com.automatelinux.trips.ui.TripListScreen
import com.automatelinux.trips.ui.TripScreen
import com.automatelinux.trips.ui.TripsViewModel
import com.automatelinux.trips.ui.feedback.FeedbackHost
import com.automatelinux.trips.ui.map.TripMap
import com.automatelinux.trips.ui.map.rememberMapController
import com.automatelinux.trips.ui.theme.AppTheme
import com.automatelinux.trips.util.rememberAssetImage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TripsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        viewModel.onPermissionResult(
            granted[Manifest.permission.ACCESS_FINE_LOCATION] == true || granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Daylight UI on a light map: dark status-bar icons, whatever the phone's night setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalAssetImage provides { file, width -> rememberAssetImage(file, width) },
            ) {
                AppTheme {
                    FeedbackHost {
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val controller = rememberMapController()
                        controller.onUserGesture = viewModel::onUserGesture

                        LaunchedEffect(Unit) {
                            viewModel.effects.collect { e ->
                                when (e) {
                                    TripsViewModel.Effect.AskLocationPermission -> permissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                                    )
                                    TripsViewModel.Effect.FitTrip -> controller.fitTrip()
                                    is TripsViewModel.Effect.CenterOn -> controller.centerOn(e.lat, e.lon)
                                    TripsViewModel.Effect.FollowMe -> (state.location as? LocationState.Fix)?.let { controller.follow(it.lat, it.lon) }
                                }
                            }
                        }
                        // keep the map's sources in step with the state
                        val trip = state.selected
                        LaunchedEffect(trip?.id, state.focusedWaypointId) { if (trip != null) controller.setTrip(trip, state.focusedWaypointId) }
                        val fix = state.location as? LocationState.Fix
                        LaunchedEffect(fix, state.headingDeg) {
                            if (fix != null) controller.setPuck(fix.lat, fix.lon, fix.accuracyM, state.headingDeg) else controller.clearPuck()
                        }
                        LaunchedEffect(fix?.lat, fix?.lon, state.follow) {
                            if (state.follow && fix != null) controller.follow(fix.lat, fix.lon)
                        }

                        BackHandler(enabled = trip != null) { viewModel.closeTrip() }

                        AnimatedContent(
                            targetState = trip,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            contentKey = { it?.id },
                            label = "screen",
                        ) { current ->
                            if (current == null) {
                                TripListScreen(state, viewModel)
                            } else {
                                TripScreen(
                                    state = state, trip = current, actions = viewModel,
                                    onMapInsets = { top, bottom -> controller.setInsets(top, bottom) },
                                    map = { modifier -> TripMap(controller, modifier) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
