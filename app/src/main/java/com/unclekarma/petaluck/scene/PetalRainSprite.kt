package com.unclekarma.petaluck.scene

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/** Draw the Editor sprite silhouette with the actual rose albedo, without a hue multiplier. */
internal object PetalRainSprite {
    fun draw(assets: AssetManager, texturePath: String): Bitmap {
        val source = assets.open(texturePath).use { checkNotNull(BitmapFactory.decodeStream(it)) }
        try {
            val sprite = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            try {
                val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                shader.setLocalMatrix(Matrix().apply {
                    setRectToRect(RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
                        RectF(42f, 28f, 214f, 224f), Matrix.ScaleToFit.FILL)
                })
                val silhouette = Path().apply {
                    moveTo(128f, 224f)
                    cubicTo(109f, 196f, 40f, 155f, 42f, 99f)
                    cubicTo(43f, 43f, 99f, 23f, 132f, 49f)
                    cubicTo(169f, 21f, 223f, 49f, 213f, 105f)
                    cubicTo(206f, 161f, 162f, 207f, 128f, 224f)
                    close()
                }
                Canvas(sprite).drawPath(silhouette, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    this.shader = shader
                })
                return sprite
            } catch (e: Exception) {
                sprite.recycle()
                throw e
            }
        } finally {
            source.recycle()
        }
    }
}
