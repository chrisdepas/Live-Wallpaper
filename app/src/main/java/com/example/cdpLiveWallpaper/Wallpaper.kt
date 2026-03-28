package com.example.cdpLiveWallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class Wallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return CanvasEngine()
    }

    inner class CanvasEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { draw() }
        private var visible = false
        private val startTime = System.currentTimeMillis()

        private val paint = Paint()
        private var shader: RuntimeShader? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val shaderCode = assets.open("raymarching.agsl").bufferedReader().use { it.readText() }
                    shader = RuntimeShader(shaderCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                draw()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }
            
            try {
                if (canvas != null) {
                    drawFrame(canvas)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.postDelayed(drawRunnable, 16)
            }
        }

        private fun drawFrame(canvas: Canvas) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
                val time = (System.currentTimeMillis() - startTime) / 1000f
                shader?.apply {
                    setFloatUniform("iResolution", canvas.width.toFloat(), canvas.height.toFloat())
                    setFloatUniform("iTime", time)
                }
                paint.shader = shader
                canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
            } else {
                canvas.drawColor(Color.BLACK)
                val fallbackPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 40f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Shader requires Android 13+", canvas.width / 2f, canvas.height / 2f, fallbackPaint)
            }
        }
    }
}
