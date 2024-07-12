package com.example.securityzone

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.res.Configuration
import java.util.Locale

class ReporteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadLocale()
        setContentView(R.layout.activity_reporte)


        setContentView(R.layout.activity_reporte)

        val backButton: ImageButton = findViewById(R.id.backButton)
        val barChart = findViewById<BarChartView>(R.id.barChart)
        val values = floatArrayOf(901f, 500f, 220f, 601f, 320f, 101f, 742f, 200f, 100f, 965f, 260f, 189f)
        val labels = arrayOf((getString(R.string.m1)), (getString(R.string.m2)), (getString(R.string.m3)),
            (getString(R.string.m4)), (getString(R.string.m5)), (getString(R.string.m6)), (getString(R.string.m7)),
            (getString(R.string.m8)), (getString(R.string.m9)), (getString(R.string.m10)), (getString(R.string.m11)),
            (getString(R.string.m12)))

        val maxValue = 1000
        barChart.setValues(values, labels, maxValue)
        backButton.setOnClickListener {
            finish() // Finaliza la actividad actual y vuelve a la anterior
        }

        setupButtonClickListeners()

    }

    private fun setupButtonClickListeners() {


        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

    }

    // Método para cambiar el idioma de la aplicación
    private fun setLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Guardar la preferencia de idioma
        val sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("Language", language)
        editor.apply()
    }

    // Método para cargar la preferencia de idioma
    private fun loadLocale() {
        val sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("Language", "en") ?: "en"
        setLocale(this, language)
    }
}
