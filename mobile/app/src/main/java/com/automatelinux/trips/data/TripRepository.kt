package com.automatelinux.trips.data

import android.content.Context
import com.automatelinux.trips.model.Trip
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trips ship inside the APK (`assets/trips/<id>.json`, written by scripts/build-trip-<id>.py),
 * because the places they describe are exactly where there is no signal.
 */
@Singleton
class TripRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun all(): List<Trip> = withContext(Dispatchers.IO) {
        val names = context.assets.list("trips")?.filter { it.endsWith(".json") }.orEmpty().sorted()
        names.map { name ->
            context.assets.open("trips/$name").bufferedReader().use { json.decodeFromString(Trip.serializer(), it.readText()) }
        }
    }
}
