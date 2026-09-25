package com.automatelinux.trips

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class TripsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
