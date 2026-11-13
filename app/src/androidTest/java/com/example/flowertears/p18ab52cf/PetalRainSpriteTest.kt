package com.example.flowertears.p18ab52cf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.flowertears.p18ab52cf.domain.RoseLayout
import com.example.flowertears.p18ab52cf.scene.PetalRainSprite
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/** Exercise the real Canvas path and all 24 production albedos on Android. */
@RunWith(AndroidJUnit4::class)
class PetalRainSpriteTest {
    @Test fun everyRoseLayerKeepsItsColorInsideAnAntialiasedPetal() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val signatures = mutableSetOf<Int>()
        val sheet = Bitmap.createBitmap(3 * 256, 8 * 256, Bitmap.Config.ARGB_8888)
        try {
            for (color in 0 until RoseLayout.COLOR_COUNT) {
                RoseLayout.layerNames.forEachIndexed { layer, name ->
                    val path = RoseLayout.texturePath(color, "${name}_01")
                    val sprite = PetalRainSprite.draw(context.assets, path)
                    val source = context.assets.open(path).use { BitmapFactory.decodeStream(it)!! }
                    val reference = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                    try {
                        // Independent rectangle draw: no hue/brightness change inside the shape.
                        Canvas(reference).drawBitmap(source, null, RectF(42f, 28f, 214f, 224f), Paint(Paint.FILTER_BITMAP_FLAG))
                        for ((x, y) in listOf(128 to 80, 80 to 110, 128 to 128, 175 to 110, 128 to 175)) {
                            val actual = sprite.getPixel(x, y)
                            val expected = reference.getPixel(x, y)
                            assertEquals("$path interior alpha", 255, Color.alpha(actual))
                            for (shift in listOf(0, 8, 16)) {
                                assertTrue("$path changed channel at $x,$y", abs(((actual ushr shift) and 255) - ((expected ushr shift) and 255)) <= 2)
                            }
                        }
                        for ((x, y) in listOf(0 to 0, 255 to 255, 128 to 15, 20 to 128, 128 to 245)) {
                            assertEquals("$path outside alpha", 0, Color.alpha(sprite.getPixel(x, y)))
                        }
                        val pixels = IntArray(256 * 256)
                        sprite.getPixels(pixels, 0, 256, 0, 0, 256, 256)
                        assertTrue("$path antialiased edge", pixels.any { Color.alpha(it) in 1..254 })
                        signatures += pixels.contentHashCode()
                        Canvas(sheet).drawBitmap(sprite, layer * 256f, color * 256f, null)
                    } finally { source.recycle(); reference.recycle(); sprite.recycle() }
                }
            }
            assertEquals("All eight colors and three layers stay distinct", 24, signatures.size)
            File(context.getExternalFilesDir(null), "petal-rain-colors-v2.png").outputStream().use {
                sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { sheet.recycle() }
    }
}
