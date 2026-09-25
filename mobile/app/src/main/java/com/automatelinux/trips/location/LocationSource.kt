package com.automatelinux.trips.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where the phone is and which way it faces, straight from the platform's
 * [LocationManager]: it works on every device, needs no Play Services, and answers to
 * `adb emu geo fix` on the emulator. On a trail the phone moves at walking pace, so a
 * fix every two seconds or five metres is plenty and cheap.
 */
@Singleton
class LocationSource @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun anyProviderEnabled(): Boolean = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .any { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

    @SuppressLint("MissingPermission")
    fun positions(): Flow<Location> = callbackFlow {
        if (!hasPermission()) { close(); return@callbackFlow }
        val lm = manager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) { close(); return@callbackFlow }

        var best: Location? = null
        fun offer(candidate: Location?) {
            if (candidate == null) return
            if (candidate.isBetterThan(best)) { best = candidate; trySend(candidate) }
        }
        providers.forEach { offer(runCatching { lm.getLastKnownLocation(it) }.getOrNull()) }
        val listener = LocationListener { offer(it) }
        providers.forEach { lm.requestLocationUpdates(it, UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, listener, Looper.getMainLooper()) }
        awaitClose { lm.removeUpdates(listener) }
    }

    /** Newer wins, unless the newer fix is markedly vaguer than a still-fresh one. */
    private fun Location.isBetterThan(other: Location?): Boolean {
        if (other == null) return true
        val newerBy = time - other.time
        if (newerBy > STALE_AFTER_MS) return true
        if (newerBy < 0) return false
        if (!hasAccuracy()) return !other.hasAccuracy()
        if (!other.hasAccuracy()) return true
        return accuracy <= other.accuracy * 2f
    }

    /** Heading in degrees clockwise from TRUE north; empty on a phone without a rotation sensor. */
    fun headings(declinationAt: () -> Location?): Flow<Float> = callbackFlow {
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotation = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotation == null) { close(); return@callbackFlow }
        val matrix = FloatArray(9); val orientation = FloatArray(3)
        var smoothSin = 0.0; var smoothCos = 0.0; var seeded = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                val magnetic = Math.toDegrees(orientation[0].toDouble())
                val declination = declinationAt()?.let {
                    GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(), it.altitude.toFloat(), System.currentTimeMillis()).declination.toDouble()
                } ?: 0.0
                val trueNorth = Math.toRadians(magnetic + declination)
                if (!seeded) { smoothSin = sin(trueNorth); smoothCos = cos(trueNorth); seeded = true } else {
                    smoothSin += (sin(trueNorth) - smoothSin) * SMOOTHING
                    smoothCos += (cos(trueNorth) - smoothCos) * SMOOTHING
                }
                trySend(((Math.toDegrees(atan2(smoothSin, smoothCos)) + 360.0) % 360.0).toFloat())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensors.unregisterListener(listener) }
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 2_000L
        const val UPDATE_DISTANCE_M = 5f
        const val STALE_AFTER_MS = 60_000L
        const val SMOOTHING = 0.15
    }
}
