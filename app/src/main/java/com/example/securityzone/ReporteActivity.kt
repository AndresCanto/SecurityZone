package com.example.securityzone

import android.app.DatePickerDialog
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
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale

class ReporteActivity : AppCompatActivity() {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var entradasM: TextView
    private lateinit var salidasM: TextView
    private lateinit var datePickerButton: Button
    private var entradasPorMes = FloatArray(12) { 0.0F }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadLocale()
        setContentView(R.layout.activity_reporte)
        entradasM = findViewById<TextView>(R.id.entradasMes)
        salidasM = findViewById<TextView>(R.id.salidasMes)
        datePickerButton = findViewById(R.id.datePickerButton2)

        fetchCarsToday(Timestamp.now().toDate())
        val barChart = findViewById<BarChartView>(R.id.barChart)
        fetchCarsMonths(object : FetchCarsMonthsCallback {
            override fun onResult(values: FloatArray) {
                val labels = arrayOf((getString(R.string.m1)), (getString(R.string.m2)), (getString(R.string.m3)),
                    (getString(R.string.m4)), (getString(R.string.m5)), (getString(R.string.m6)), (getString(R.string.m7)),
                    (getString(R.string.m8)), (getString(R.string.m9)), (getString(R.string.m10)), (getString(R.string.m11)),
                    (getString(R.string.m12)))
                val maxValue = 100
                barChart.setValues(values, labels, maxValue)
                entradasPorMes = values
            }
        })

        setupDatePicker()
        setupButtonClickListeners()
    }

    private fun setupDatePicker() {
        datePickerButton.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = java.util.Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    fetchCarsToday(selectedDate.time)
                    try {
                        val cal = Calendar.getInstance()
                        cal.time = selectedDate.time
                        val m = cal.get(Calendar.MONTH)

                        entradasM.text = getString(R.string.month_entries, entradasPorMes[m])
                        salidasM.text = getString(R.string.month_exits, entradasPorMes[m])
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al asignar texto a TextView", e)
                    }
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun getStartOfDay(t: Date= Timestamp.now().toDate()): Date {
        val cal = Calendar.getInstance()
        cal.time = t
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getEndOfDay(t: Date= Timestamp.now().toDate()): Date {
        val cal = Calendar.getInstance()
        cal.time = t
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }

    private interface FetchCarsMonthsCallback {
        fun onResult(values: FloatArray)
    }

    private fun fetchCarsMonths(callback: FetchCarsMonthsCallback) {
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

                val fecha = Timestamp.now().toDate()
                val cal = Calendar.getInstance()
                cal.time = fecha
                val m = cal.get(Calendar.MONTH)
                try {
                    entradasM.text = getString(R.string.month_entries, entradasPorMes[m])
                    salidasM.text = getString(R.string.month_exits, salidasPorMes[m])
                } catch (e: Exception) {
                    Log.e(TAG, "Error al asignar texto a TextView", e)
                }

                callback.onResult(entradasPorMes)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener documentos", exception)
            }
    }

    private fun fetchCarsToday(time: Date) {
        db.collection("entradasSalidas")
            .get()
            .addOnSuccessListener { result ->
                // Obtener la fecha de hoy
                val startOfDay = getStartOfDay(time)
                val endOfDay = getEndOfDay(time)

                // Contar las entradas y salidas de hoy
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

                val pickDate: TextView = findViewById(R.id.datePickerButton2)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                pickDate.text = sdf.format(time)
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
