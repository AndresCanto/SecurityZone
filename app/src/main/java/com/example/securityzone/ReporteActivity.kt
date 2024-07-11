package com.example.securityzone

import android.content.ContentValues.TAG
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
import android.icu.util.Calendar
import android.util.Log
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale

class ReporteActivity : AppCompatActivity() {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadLocale()
        setContentView(R.layout.activity_reporte)
        fetchCarrosToday()

        val backButton: ImageButton = findViewById(R.id.backButton)
        val barChart = findViewById<BarChartView>(R.id.barChart)
        val values = fetchCarrosMonths()
        val labels = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val maxValue = 1000
        barChart.setValues(values, labels, maxValue)
        backButton.setOnClickListener {
            finish() // Finaliza la actividad actual y vuelve a la anterior
        }

        setupButtonClickListeners()
    }

    private fun getStartOfDay(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getEndOfDay(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
    
    private fun fetchCarrosMonths(): FloatArray {
        val entradasPorMes = FloatArray(12) { 0.0F }
        db.collection("entradasSalidas")
            .get()
            .addOnSuccessListener { result ->
                // Crear un mapa para contar entradas y salidas por mes
                val salidasPorMes = FloatArray(12) { 0.0F }

                // Iterar sobre los documentos y contar entradas y salidas por mes
                for (document in result.documents) {
                    val timestamp = document.getTimestamp("hora")
                    if (timestamp != null) {
                        val fecha = timestamp.toDate()
                        val cal = Calendar.getInstance()
                        cal.time = fecha
                        val mes = cal.get(Calendar.MONTH)

                        val tipo = document.getString("tipo")
                        if (tipo != null) {
                            if (tipo == "entrada") {
                                entradasPorMes[mes]++
                            } else if (tipo == "salida") {
                                salidasPorMes[mes]++
                            }
                        }
                    }
                }
//                val entradasM: TextView = findViewById(R.id.entradasMes)
//                entradasM.text = getString(R.string.month_entries,entradasPorMes[6])
//
//                val salidasM: TextView = findViewById(R.id.salidasMes)
//                salidasM.text = getString(R.string.month_exits,entradasPorMes[6])
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener documentos", exception)
            }
        return entradasPorMes
    }

    private fun fetchCarrosToday() {
        db.collection("entradasSalidas")
            .get()
            .addOnSuccessListener { result ->
                // Obtener la fecha de hoy
                val startOfDay = getStartOfDay()
                val endOfDay = getEndOfDay()

                // Contar las entradas y salidas del MES
                var entradaH = 0
                var salidaH = 0

                // Iterar sobre los resultados y contar entradas y salidas para hoy
                for (document in result) {
                    val timestamp = document.getTimestamp("hora")
                    if (timestamp != null) {
                        val date = timestamp.toDate()
                        if (date.after(startOfDay) && date.before(endOfDay)) {
                            val tipo = document.getString("tipo")
                            if (tipo != null) {
                                if (tipo == "entrada") {
                                    entradaH++
                                } else if (tipo == "salida") {
                                    salidaH++
                                }
                            }
                        }
                    }
                }

                // Actualizar los TextViews con los contadores
                val entradasH: TextView = findViewById(R.id.entradasHoy)
                entradasH.text = getString(R.string.today_entries, entradaH)

                val salidasH: TextView = findViewById(R.id.salidasHoy)
                salidasH.text = getString(R.string.today_exits, salidaH)
            }
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
