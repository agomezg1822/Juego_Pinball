package com.example.painball_proyecto

sealed class Obstaculo(val puntos: Int) {
    data class Circulo(val cx: Float, val cy: Float, val r: Float) : Obstaculo(2)
    data class Triangulo(val x1: Float, val y1: Float,
                         val x2: Float, val y2: Float,
                         val x3: Float, val y3: Float) : Obstaculo(4)
    data class Cuadrado(val left: Float, val top: Float, val size: Float) : Obstaculo(6)
}