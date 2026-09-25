package com.automatelinux.trips.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.automatelinux.trips.model.Trip
import com.automatelinux.trips.ui.theme.Tp
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Imperative handle on the MapLibre view. The screen feeds it the trip, the phone's
 * position and which waypoint is being looked at; it keeps the style's sources in sync
 * and moves the camera. Every setter is idempotent and replayed after a style load.
 *
 * The base map is `assets/style.json`: the Israel Hiking Map raster tiles for this
 * trip's area packed into the APK, with the same tiles fetched online drawn above them
 * when there is signal. Offline the online layer simply draws nothing.
 */
class MapController(context: Context) {
    var mapView: MapView? = null
        private set
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private val density = context.resources.displayMetrics.density

    private var trip: Trip? = null
    private var puck: Triple<Double, Double, Float>? = null   // lat, lon, accuracy
    private var heading: Float? = null
    private var highlighted: String? = null
    private var topInsetPx = 0
    private var bottomInsetPx = 0
    /** A camera move asked for before the style was ready; run once it is. */
    private var pendingCamera: (() -> Unit)? = null

    var onUserGesture: () -> Unit = {}

    fun attach(view: MapView) {
        mapView = view
        view.getMapAsync { m ->
            map = m
            m.uiSettings.apply {
                isCompassEnabled = true
                setCompassMargins((14 * density).toInt(), (120 * density).toInt(), (14 * density).toInt(), 0)
                isLogoEnabled = false
                isAttributionEnabled = true
                setAttributionMargins((14 * density).toInt(), 0, 0, (8 * density).toInt())
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = false
            }
            m.setMinZoomPreference(7.0)
            m.setMaxZoomPreference(19.0)
            m.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) onUserGesture()
            }
            m.setStyle(Style.Builder().fromUri("asset://style.json")) { st ->
                style = st
                st.addImage(IMG_PUCK, Bitmaps.puck(density))
                st.addImage(IMG_START, Bitmaps.flag(density, finish = false))
                st.addImage(IMG_FINISH, Bitmaps.flag(density, finish = true))
                st.addSource(GeoJsonSource(SRC_ROUTE, empty()))
                st.addSource(GeoJsonSource(SRC_WAYPOINTS, empty()))
                st.addSource(GeoJsonSource(SRC_ACCURACY, empty()))
                st.addSource(GeoJsonSource(SRC_PUCK, empty()))
                st.addLayer(
                    LineLayer(LAYER_CASING, SRC_ROUTE).withProperties(
                        lineColor("#FFFFFF"), lineWidth(9f), lineOpacity(0.9f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
                    ),
                )
                st.addLayer(
                    LineLayer(LAYER_TRAIL, SRC_ROUTE).withProperties(
                        lineColor(get("color")), lineWidth(5f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
                    ).withFilter(eq(get("kind"), literal("trail"))),
                )
                st.addLayer(
                    LineLayer(LAYER_ROAD, SRC_ROUTE).withProperties(
                        lineColor(get("color")), lineWidth(4f), lineDasharray(arrayOf(1.2f, 1.6f)), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
                    ).withFilter(eq(get("kind"), literal("road"))),
                )
                st.addLayer(
                    FillLayer(LAYER_ACCURACY, SRC_ACCURACY).withProperties(fillColor("#2E7FC0"), fillOutlineColor("#2E7FC0"), fillOpacity(0.12f)),
                )
                st.addLayer(
                    SymbolLayer(LAYER_WAYPOINTS, SRC_WAYPOINTS).withProperties(
                        iconImage(get("icon")), iconAllowOverlap(true), iconIgnorePlacement(true), iconAnchor(get("anchor")), iconSize(1f),
                    ),
                )
                st.addLayer(
                    SymbolLayer(LAYER_PUCK, SRC_PUCK).withProperties(
                        iconImage(IMG_PUCK), iconRotate(get("bearing")), iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                        iconAllowOverlap(true), iconIgnorePlacement(true), iconSize(1f),
                    ),
                )
                replay()
                val pending = pendingCamera
                pendingCamera = null
                if (pending != null) pending() else if (trip != null) fitTrip()
            }
        }
    }

    private val ready: Boolean get() = map != null && style != null

    fun setInsets(topPx: Int, bottomPx: Int) { topInsetPx = topPx; bottomInsetPx = bottomPx }

    private fun replay() {
        trip?.let { setTrip(it, highlighted) }
        puck?.let { setPuck(it.first, it.second, it.third, heading) }
    }

    fun setTrip(t: Trip, highlightedId: String?) {
        trip = t; highlighted = highlightedId
        val st = style ?: return
        val lines = t.segments.map { s ->
            Feature.fromGeometry(LineString.fromLngLats(s.points.map { Point.fromLngLat(it[1], it[0]) })).also {
                it.addStringProperty("color", hex(Tp.marker(s.marker).value))
                it.addStringProperty("kind", s.kind)
            }
        }
        st.getSourceAs<GeoJsonSource>(SRC_ROUTE)?.setGeoJson(FeatureCollection.fromFeatures(lines))

        val feats = ArrayList<Feature>()
        t.waypoints.forEachIndexed { i, wp ->
            val n = i + 1
            // on a loop the finish stands on the start; the flag says it, a second disc would only hide "1"
            if (t.isLoop && i == t.waypoints.lastIndex && wp.kind == "finish") return@forEachIndexed
            val marker = com.automatelinux.trips.ui.markerAt(t, wp.atM)
            val hl = wp.id == highlightedId
            val key = "wp-$n-$marker-$hl"
            if (st.getImage(key) == null) st.addImage(key, Bitmaps.waypoint(density, n, Tp.marker(marker).toArgb(), hl))
            feats += Feature.fromGeometry(Point.fromLngLat(wp.lon, wp.lat)).also {
                it.addStringProperty("icon", key); it.addStringProperty("anchor", "center")
            }
        }
        val first = t.waypoints.firstOrNull(); val last = t.waypoints.lastOrNull()
        if (first != null) feats += Feature.fromGeometry(Point.fromLngLat(first.lon, first.lat)).also { it.addStringProperty("icon", IMG_START); it.addStringProperty("anchor", "bottom-left") }
        if (last != null && !t.isLoop) feats += Feature.fromGeometry(Point.fromLngLat(last.lon, last.lat)).also { it.addStringProperty("icon", IMG_FINISH); it.addStringProperty("anchor", "bottom-left") }
        st.getSourceAs<GeoJsonSource>(SRC_WAYPOINTS)?.setGeoJson(FeatureCollection.fromFeatures(feats))
    }

    fun setPuck(lat: Double, lon: Double, accuracyM: Float, headingDeg: Float?) {
        puck = Triple(lat, lon, accuracyM); heading = headingDeg
        val st = style ?: return
        val f = Feature.fromGeometry(Point.fromLngLat(lon, lat))
        f.addNumberProperty("bearing", headingDeg ?: 0f)
        st.getSourceAs<GeoJsonSource>(SRC_PUCK)?.setGeoJson(f)
        st.getSourceAs<GeoJsonSource>(SRC_ACCURACY)?.setGeoJson(circle(lat, lon, accuracyM.toDouble()))
    }

    fun clearPuck() {
        puck = null
        style?.getSourceAs<GeoJsonSource>(SRC_PUCK)?.setGeoJson(empty())
        style?.getSourceAs<GeoJsonSource>(SRC_ACCURACY)?.setGeoJson(empty())
    }

    /** The whole route in the part of the screen the sheet leaves free. */
    fun fitTrip() {
        if (!ready) { pendingCamera = { fitTrip() }; return }
        val m = map ?: return
        val t = trip ?: return
        val b = LatLngBounds.Builder().also { bb -> t.segments.forEach { s -> s.points.forEach { bb.include(LatLng(it[0], it[1])) } } }.build()
        val side = (36 * density).toInt()
        m.easeCamera(CameraUpdateFactory.newLatLngBounds(b, side, topInsetPx + side, side, bottomInsetPx + side), 700)
    }

    /** Walking camera: north up, you a little below the free area's centre. */
    fun follow(lat: Double, lon: Double, zoom: Double = 16.6, animateMs: Int = 700) {
        if (!ready) { pendingCamera = { follow(lat, lon, zoom, animateMs) }; return }
        val m = map ?: return
        val cp = CameraPosition.Builder()
            .target(LatLng(lat, lon)).zoom(zoom).tilt(0.0)
            .padding(doubleArrayOf(0.0, topInsetPx.toDouble(), 0.0, bottomInsetPx.toDouble()))
            .build()
        m.easeCamera(CameraUpdateFactory.newCameraPosition(cp), animateMs)
    }

    fun centerOn(lat: Double, lon: Double, zoom: Double = 16.8) {
        if (!ready) { pendingCamera = { centerOn(lat, lon, zoom) }; return }
        val m = map ?: return
        val cp = CameraPosition.Builder()
            .target(LatLng(lat, lon)).zoom(zoom).tilt(0.0)
            .padding(doubleArrayOf(0.0, topInsetPx.toDouble(), 0.0, bottomInsetPx.toDouble()))
            .build()
        m.easeCamera(CameraUpdateFactory.newCameraPosition(cp), 600)
    }

    private fun empty() = FeatureCollection.fromFeatures(emptyList())

    private fun circle(lat: Double, lon: Double, radiusM: Double): Feature {
        val r = radiusM.coerceIn(3.0, 500.0)
        val kLat = 1.0 / 111_320.0
        val kLon = 1.0 / (111_320.0 * cos(lat * PI / 180))
        val ring = (0..40).map { i ->
            val a = i * 2 * PI / 40
            Point.fromLngLat(lon + r * sin(a) * kLon, lat + r * cos(a) * kLat)
        }
        return Feature.fromGeometry(Polygon.fromLngLats(listOf(ring)))
    }

    private fun hex(argb: ULong): String = String.format("#%06X", Color(argb).toArgb() and 0xFFFFFF)

    companion object {
        const val SRC_ROUTE = "route-src"
        const val SRC_WAYPOINTS = "waypoints-src"
        const val SRC_ACCURACY = "accuracy-src"
        const val SRC_PUCK = "puck-src"
        const val LAYER_CASING = "route-casing"
        const val LAYER_TRAIL = "route-trail"
        const val LAYER_ROAD = "route-road"
        const val LAYER_ACCURACY = "accuracy"
        const val LAYER_WAYPOINTS = "waypoints"
        const val LAYER_PUCK = "puck"
        const val IMG_PUCK = "puck"
        const val IMG_START = "flag-start"
        const val IMG_FINISH = "flag-finish"
    }
}

@Composable
fun rememberMapController(): MapController {
    val context = LocalContext.current
    return remember { MapController(context) }
}

@Composable
fun TripMap(controller: MapController, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).also { view ->
                view.onCreate(null)
                controller.attach(view)
            }
        },
    )
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            val v = controller.mapView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> v.onStart()
                Lifecycle.Event.ON_RESUME -> v.onResume()
                Lifecycle.Event.ON_PAUSE -> v.onPause()
                Lifecycle.Event.ON_STOP -> v.onStop()
                Lifecycle.Event.ON_DESTROY -> v.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
