package com.example.painball_proyecto

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View


class BolaView(context: Context) : View(context) {
    private val paintBola = Paint().apply {
        color = 0xFF2196F3.toInt()
        isAntiAlias = true
    }
    private val paintObs = Paint().apply {
        color = 0xFFFF5722.toInt()
        isAntiAlias = true
    }

    private var x = 300f
    private var y = 300f
    private val radio = 50f

    private var obstaculos: List<Obstaculo> = emptyList()

    fun setObstaculos(list: List<Obstaculo>) {
        obstaculos = list
        invalidate()
    }

    fun mover(nx: Float, ny: Float) {
        x = nx
        y = ny
        invalidate()
    }

    fun getRadio() = radio
    fun getXpos() = x
    fun getYpos() = y

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (obs in obstaculos) {
            when (obs) {
                is Obstaculo.Circulo -> {
                    canvas.drawCircle(obs.cx, obs.cy, obs.r, paintObs)
                }
                is Obstaculo.Triangulo -> {
                    val path = Path().apply {
                        moveTo(obs.x1, obs.y1)
                        lineTo(obs.x2, obs.y2)
                        lineTo(obs.x3, obs.y3)
                        close()
                    }
                    canvas.drawPath(path, paintObs)
                }
                is Obstaculo.Cuadrado -> {
                    canvas.drawRect(obs.left, obs.top, obs.left + obs.size, obs.top + obs.size, paintObs)
                }
            }
        }

        // bola
        canvas.drawCircle(x, y, radio, paintBola)
    }
}