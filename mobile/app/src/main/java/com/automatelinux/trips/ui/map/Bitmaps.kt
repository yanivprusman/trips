package com.automatelinux.trips.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/** The few marks the map draws itself. Everything is drawn at the phone's density. */
object Bitmaps {
    private fun paint(color: Int, style: Paint.Style = Paint.Style.FILL) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.style = style }

    /** You: a blue disc in a white ring with a soft halo, and a wedge for the heading. */
    fun puck(density: Float): Bitmap {
        val s = (56 * density).toInt()
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(b); val cx = s / 2f; val cy = s / 2f
        // heading wedge, pointing up (the layer rotates the icon)
        val wedge = Path().apply {
            moveTo(cx, cy - 26 * density); lineTo(cx - 11 * density, cy - 6 * density); lineTo(cx + 11 * density, cy - 6 * density); close()
        }
        c.drawPath(wedge, paint(0xFF2E7FC0.toInt()).apply { alpha = 200 })
        c.drawCircle(cx, cy, 13 * density, paint(0x332E7FC0))
        c.drawCircle(cx, cy, 9.5f * density, paint(Color.WHITE))
        c.drawCircle(cx, cy, 7 * density, paint(0xFF2E7FC0.toInt()))
        return b
    }

    /** A numbered disc in the blaze colour of its segment, with a white rim so it reads on any tile. */
    fun waypoint(density: Float, n: Int, color: Int, highlighted: Boolean): Bitmap {
        val s = (36 * density).toInt()
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(b); val cx = s / 2f; val cy = s / 2f
        c.drawCircle(cx, cy, 15 * density, paint(0x33000000))
        c.drawCircle(cx, cy - 0.5f * density, 14 * density, paint(Color.WHITE))
        c.drawCircle(cx, cy - 0.5f * density, if (highlighted) 12.5f * density else 11.5f * density, paint(if (highlighted) 0xFFC2410C.toInt() else color))
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE; textSize = 13 * density; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText(n.toString(), cx, cy + 4.5f * density, t)
        return b
    }

    /** Start (a flag) and finish (chequered) on one small pole. */
    fun flag(density: Float, finish: Boolean): Bitmap {
        val w = (30 * density).toInt(); val h = (34 * density).toInt()
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val poleX = 6 * density
        c.drawRoundRect(RectF(poleX - 1.5f * density, 2 * density, poleX + 1.5f * density, h - 2 * density), 2f, 2f, paint(0xFF1F1A15.toInt()))
        val flag = RectF(poleX, 3 * density, poleX + 20 * density, 17 * density)
        if (finish) {
            c.drawRect(flag, paint(Color.WHITE))
            val cell = flag.width() / 4
            for (i in 0 until 4) for (j in 0 until 2) if ((i + j) % 2 == 0) {
                c.drawRect(flag.left + i * cell, flag.top + j * cell, flag.left + (i + 1) * cell, flag.top + (j + 1) * cell, paint(0xFF1F1A15.toInt()))
            }
            c.drawRect(flag, paint(0xFF1F1A15.toInt(), Paint.Style.STROKE).apply { strokeWidth = 1.2f * density })
        } else {
            c.drawRect(flag, paint(0xFFC2410C.toInt()))
        }
        return b
    }
}
