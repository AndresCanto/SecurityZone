package com.example.securityzone

import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import android.content.Context
import android.content.res.Configuration
import android.icu.text.SimpleDateFormat
import android.util.Log
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InformeActivity : AppCompatActivity() {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var datePickerButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadLocale()
        setContentView(R.layout.activity_informe)

        datePickerButton = findViewById(R.id.datePickerButton2)

        fetchAlerts(Timestamp.now().toDate())
        fetchCarsToday(Timestamp.now().toDate())
        setupButtonClickListeners()
    }

    private fun setupDatePicker() {
        datePickerButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    fetchCarsToday(selectedDate.time)
                    fetchAlerts(selectedDate.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
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

    private fun fetchCarsToday(time: Date) {
        db.collection("entradasSalidas")
            .get()
            .addOnSuccessListener { result ->
                // Obtener la fecha de hoy
                val startOfDay = getStartOfDay(time)
                val endOfDay = getEndOfDay(time)

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
                val carros: TextView = findViewById(R.id.carros)
                carros.text = getString(R.string.current_entries, entradaH)
                val pickDate: TextView = findViewById(R.id.datePickerButton2)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                pickDate.text = sdf.format(time)
            }
    }

    private fun fetchAlerts(time: Date) {
        db.collection("alertas")
            .whereEqualTo("msj", false)
            .get()
            .addOnSuccessListener { result ->
                // Obtener la fecha de hoy
                val startOfDay = getStartOfDay(time)
                val endOfDay = getEndOfDay(time)

                var alertas = 0
                for (document in result) {
                    val timestamp = document.getTimestamp("hora")
                    if (timestamp != null) {
                        val date = timestamp.toDate()
                        if (date.after(startOfDay) && date.before(endOfDay)) {
                            alertas++
                        }
                    }
                }

                val alertasV: TextView = findViewById(R.id.alertas)
                alertasV.text = getString(R.string.alerts_sent,alertas)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener documentos", exception)
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

    private fun setupButtonClickListeners() {
        findViewById<Button>(R.id.reporteBtn).setOnClickListener {
            startActivity(Intent(this, ReporteActivity::class.java))
        }
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        setupDatePicker()
    }
}