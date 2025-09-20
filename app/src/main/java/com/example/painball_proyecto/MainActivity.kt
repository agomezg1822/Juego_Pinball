package com.example.painball_proyecto

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*
import kotlin.random.Random
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    var envioServidor = false
    private lateinit var frame: FrameLayout
    private lateinit var bolaView: BolaView
    private lateinit var textoPuntaje: TextView
    private lateinit var botonSoltar: Button
    private lateinit var botonReiniciar: Button
    private lateinit var textoGameOver: TextView

    private var xPos = 0f
    private var yPos = 0f
    private var xVel = 0f
    private var yVel = 0f
    private val radio = 50f

    private var gameStarted = false
    private var gameOver = false
    private var puntaje = 0

    private var obstaculos: MutableList<Obstaculo> = mutableListOf()
    private var lastHitTimes: LongArray = LongArray(0) // timestamps para cooldown por obstáculo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            frame = FrameLayout(this).apply {
                setBackgroundColor(Color.WHITE)
            }

            bolaView = BolaView(this)
            frame.addView(bolaView)

            // texto puntaje
            textoPuntaje = TextView(this).apply {
                text = "Puntaje: 0"
                textSize = 18f
                setTextColor(Color.BLACK)
            }
            val paramsPuntaje = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                bottomMargin = 16
                marginStart = 16
            }
            frame.addView(textoPuntaje, paramsPuntaje)

            // layout horizontal para botones
            val layoutBotones = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            // boton soltar
            botonSoltar = Button(this).apply {
                text = "Soltar bola"
                setOnClickListener {
                    if (!gameStarted && !gameOver) {
                        yVel = -30f
                        gameStarted = true
                        isEnabled = false
                    }
                }
            }
            layoutBotones.addView(botonSoltar)

            // Botón "Volver al menú anterior"
            val volverBtn = android.widget.Button(this).apply {
                text = "Volver al menú anterior"
                setOnClickListener { finish() }
            }
            layoutBotones.addView(volverBtn)

            // boton salir (seguro)
            val botonSalir = Button(this).apply {
                text = "Salir"
                setOnClickListener {
                    finishAffinity()
                }
            }
            layoutBotones.addView(botonSalir)

            val paramsBotones = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                bottomMargin = 80
            }
            frame.addView(layoutBotones, paramsBotones)

            // texto game over (oculto)
            textoGameOver = TextView(this).apply {
                text = "GAME OVER"
                textSize = 40f
                setTextColor(Color.RED)
                visibility = View.GONE
                gravity = Gravity.CENTER
            }
            val paramsGO = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            frame.addView(textoGameOver, paramsGO)

            // boton reiniciar (oculto)
            botonReiniciar = Button(this).apply {
                text = "Jugar de nuevo"
                visibility = View.GONE
                setOnClickListener { reiniciarJuego() }
            }
            val paramsReiniciar = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            }
            frame.addView(botonReiniciar, paramsReiniciar)

            setContentView(frame)

            // sensores
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            // cuando la vista tenga tamaño, generamos obstáculos relativos y reiniciamos
            bolaView.post {
                obstaculos = generarObstaculos(bolaView.width, bolaView.height).toMutableList()
                bolaView.setObstaculos(obstaculos)
                lastHitTimes = LongArray(obstaculos.size) { 0L }
                reiniciarJuego()
            }

        } catch (e: Exception) {
            // si algo falla en onCreate, mostramos el error en pantalla en vez de dejar que la app muera sin pista
            Log.e("MainActivity", "Error en onCreate", e)
            val tv = TextView(this).apply {
                text = "Error al iniciar la app:\n${e.localizedMessage}"
                setTextColor(Color.RED)
            }
            setContentView(tv)
        }
    }

    private fun generarObstaculos(width: Int, height: Int): List<Obstaculo> {
        // posiciones relativas y distribuidas para evitar overlap razonable
        val w = width.toFloat()
        val h = height.toFloat()
        val r = min(w, h) * 0.06f // radio relativo
        return listOf(
            Obstaculo.Circulo(0.18f * w, 0.25f * h, r),
            Obstaculo.Circulo(0.82f * w, 0.20f * h, r),
            Obstaculo.Circulo(0.30f * w, 0.55f * h, r),
            Obstaculo.Circulo(0.72f * w, 0.65f * h, r),
            Obstaculo.Triangulo(0.45f * w, 0.15f * h, 0.55f * w, 0.28f * h, 0.35f * w, 0.28f * h),
            Obstaculo.Triangulo(0.60f * w, 0.60f * h, 0.70f * w, 0.72f * h, 0.50f * w, 0.72f * h),
            Obstaculo.Cuadrado(0.1f * w, 0.1f * h, min(w, h) * 0.12f)
        )
    }

    private fun reiniciarJuego() {
        xPos = bolaView.width / 2f
        yPos = bolaView.height - radio
        xVel = 0f
        yVel = 0f
        puntaje = 0
        gameStarted = false
        gameOver = false

        textoPuntaje.text = "Puntaje: 0"
        botonSoltar.isEnabled = true
        botonSoltar.visibility = View.VISIBLE
        textoGameOver.visibility = View.GONE
        botonReiniciar.visibility = View.GONE

        // fondo blanco al reiniciar
        frame.setBackgroundColor(Color.WHITE)

        bolaView.mover(xPos, yPos)
    }

    private fun mostrarGameOver() {
        gameOver = true
        gameStarted = false
        textoGameOver.visibility = View.VISIBLE
        botonReiniciar.visibility = View.VISIBLE
        botonSoltar.visibility = View.GONE
    }

    // cooldown de 300 ms para no contar múltiples frames como muchos hits
    private val HIT_COOLDOWN_MS = 300L

    private fun chequearColisiones() {
        if (gameOver) return

        for (obs in obstaculos) {
            var colision = false

            when (obs) {
                is Obstaculo.Circulo -> {
                    val dx = xPos - obs.cx
                    val dy = yPos - obs.cy
                    val dist = sqrt((dx * dx + dy * dy).toDouble())
                    if (dist < radio + obs.r) {
                        colision = true
                        // Rebote realista
                        val nx = (dx / dist).toFloat()
                        val ny = (dy / dist).toFloat()
                        val dot = xVel * nx + yVel * ny
                        xVel -= 2 * dot * nx
                        yVel -= 2 * dot * ny
                        xVel *= 0.8f
                        yVel *= 0.8f
                        xPos = obs.cx + nx * (radio + obs.r + 1)
                        yPos = obs.cy + ny * (radio + obs.r + 1)
                    }
                }

                is Obstaculo.Cuadrado -> {
                    if (xPos in obs.left..(obs.left + obs.size) &&
                        yPos in obs.top..(obs.top + obs.size)) {
                        colision = true
                        val centerX = obs.left + obs.size / 2
                        val centerY = obs.top + obs.size / 2
                        val dx = xPos - centerX
                        val dy = yPos - centerY
                        if (abs(dx) > abs(dy)) {
                            xVel *= -0.8f
                            xPos = if (dx > 0) obs.left + obs.size + radio else obs.left - radio
                        } else {
                            yVel *= -0.8f
                            yPos = if (dy > 0) obs.top + obs.size + radio else obs.top - radio
                        }
                    }
                }

                is Obstaculo.Triangulo -> {
                    val minX = min(obs.x1, min(obs.x2, obs.x3))
                    val maxX = max(obs.x1, max(obs.x2, obs.x3))
                    val minY = min(obs.y1, min(obs.y2, obs.y3))
                    val maxY = max(obs.y1, max(obs.y2, obs.y3))

                    if (xPos in minX..maxX && yPos in minY..maxY) {
                        colision = true

                        // Rebote simplificado → invertir componente dominante
                        if (abs(xVel) > abs(yVel)) {
                            xVel *= -0.8f
                            xPos += if (xVel > 0) radio else -radio // empujamos fuera
                        } else {
                            yVel *= -0.8f
                            yPos += if (yVel > 0) radio else -radio // empujamos fuera
                        }
                    }
                }
            }

            if (colision) {
                puntaje += obs.puntos
                textoPuntaje.text = "Puntaje: $puntaje"


                if (puntaje >= 20 && !envioServidor) {
                    envioServidor = true
                    thread {
                        try {
                            val url = URL("http://192.168.0.24:6800/puntos?valor=$puntaje")
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

                // Cambiar fondo aleatoriamente
                val color = android.graphics.Color.rgb(
                    Random.nextInt(256),
                    Random.nextInt(256),
                    Random.nextInt(256)
                )
                frame.setBackgroundColor(color)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            accelerometer?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error registrando sensor", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!gameStarted || gameOver) return

        val ax = -event.values[0]
        val ay = event.values[1]

        xVel += ax * 0.5f
        yVel += ay * 0.5f

        xPos += xVel
        yPos += yVel

        val maxX = bolaView.width - radio
        val maxY = bolaView.height - radio

        if (xPos < radio) { xPos = radio; xVel *= -0.8f }
        else if (xPos > maxX) { xPos = maxX; xVel *= -0.8f }

        if (yPos < radio) { yPos = radio; yVel *= -0.8f }

        if (yPos > maxY) {
            yPos = maxY
            yVel = 0f
            mostrarGameOver()
        }

        bolaView.mover(xPos, yPos)
        chequearColisiones()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

}