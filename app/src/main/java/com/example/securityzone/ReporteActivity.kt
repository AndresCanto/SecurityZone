package com.example.securityzone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ReporteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reporte)

        val barChart = findViewById<BarChartView>(R.id.barChart)
        val values = floatArrayOf(901f, 500f, 220f, 601f, 320f, 101f, 742f, 200f, 100f, 965f, 260f, 189f)
        val labels = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val maxValue = 1000
        barChart.setValues(values, labels, maxValue)
        setupButtonClickListeners()

    }
    private fun setupButtonClickListeners() {


        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
