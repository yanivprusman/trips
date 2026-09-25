package com.automatelinux.trips.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Photos live in the APK's assets and are decoded once per requested width — the
 * hero card asks for a wide one, the gallery for a small one — so scrolling the guide
 * never decodes a 1400-px JPEG twice.
 */
object AssetImages {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun load(context: Context, file: String, targetWidthPx: Int): ImageBitmap? {
        val key = "$file@$targetWidthPx"
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(file).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetWidthPx) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.assets.open(file).use { BitmapFactory.decodeStream(it, null, opts) } ?: return@withContext null
            bmp.asImageBitmap().also { cache[key] = it }
        }
    }
}

@Composable
fun rememberAssetImage(file: String, targetWidthPx: Int): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, file, targetWidthPx) {
        value = AssetImages.load(context, file, targetWidthPx)
    }
    return image
}
