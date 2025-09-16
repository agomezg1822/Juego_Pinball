package com.example.painball_proyecto

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LienzoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lienzoView = object : View(this) {
            private val paint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 5f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            private val path = Path()

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                canvas.drawPath(path, paint)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        path.moveTo(event.x, event.y)
                        enviarCoordenada(event.x, event.y)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        path.lineTo(event.x, event.y)
                        enviarCoordenada(event.x, event.y)
                        invalidate()
                    }
                }
                return true
            }
        }

        // Create a vertical LinearLayout to hold the lienzoView and buttons
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Add the lienzoView (weight=1 to fill space)
        val lienzoParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        layout.addView(lienzoView, lienzoParams)

        // Add "Volver al menú anterior" button
        val volverBtn = android.widget.Button(this).apply {
            text = "Volver al menú anterior"
            setOnClickListener { finish() }
        }
        layout.addView(volverBtn)

        // Add "Salir" button
        val salirBtn = android.widget.Button(this).apply {
            text = "Salir"
            setOnClickListener { finishAffinity() }
        }
        layout.addView(salirBtn)

        setContentView(layout)
    }



    private fun enviarCoordenada(x: Float, y: Float) {
        thread {
            try {
                val url = URL("http://192.168.0.18:6800/dibujo?x=$x&y=$y")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connect()
                conn.inputStream.close()
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}