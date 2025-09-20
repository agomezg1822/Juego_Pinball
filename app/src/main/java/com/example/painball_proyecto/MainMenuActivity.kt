package com.example.painball_proyecto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        // Botón 1: abre el juego actual
        val botonJuego = Button(this).apply {
            text = "Abrir Juego"
            setOnClickListener {
                startActivity(Intent(this@MainMenuActivity, MainActivity::class.java))
            }
        }
        layout.addView(botonJuego)

        // Botón 2: lienzo en blanco
        val botonLienzo = Button(this).apply {
            text = "Lienzo en blanco"
            setOnClickListener {
                startActivity(Intent(this@MainMenuActivity, LienzoActivity::class.java))
            }
        }
        layout.addView(botonLienzo)

        // Botón 3: estrella en Python
        val botonEstrella = Button(this).apply {
            text = "Ver estrella en PC"
            setOnClickListener {
                Thread {
                    try {
                        val url = java.net.URL("http://192.168.0.24:6800/estrella")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connect()
                        // read/close the input stream to complete the request
                        conn.inputStream.close()
                        conn.disconnect()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }
        layout.addView(botonEstrella)

        // Botón 4: salir
        val botonSalir = Button(this).apply {
            text = "Salir"
            setOnClickListener {
                finishAffinity()
            }
        }
        layout.addView(botonSalir)


        // Parent container to center the layout in the screen
        val parent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            addView(layout)
        }
        setContentView(parent)
    }
}