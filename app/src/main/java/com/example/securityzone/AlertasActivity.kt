package com.example.securityzone

import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class AlertasActivity : AppCompatActivity() {
    private lateinit var alertasRecyclerView: RecyclerView
    private lateinit var alertaAdapter: AlertaAdapter
    private lateinit var datePickerButton: Button
    private lateinit var showAllAlertsButton: Button
    private lateinit var backButton: ImageButton

    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var alertas: List<Alerta> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
        setContentView(R.layout.activity_alertas)

        alertasRecyclerView = findViewById(R.id.alertasRecyclerView)
        datePickerButton = findViewById(R.id.datePickerButton)
        showAllAlertsButton = findViewById(R.id.showAllAlertsButton)
        backButton = findViewById(R.id.backButton)

        setupRecyclerView()
        setupDatePicker()
        setupShowAllAlertsButton()
        setupBackButton()
        fetchAlerts()
    }

    private fun setupRecyclerView() {
        alertaAdapter = AlertaAdapter(alertas)
        alertasRecyclerView.layoutManager = LinearLayoutManager(this)
        alertasRecyclerView.adapter = alertaAdapter
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
                    fetchAlertsByDate(selectedDate.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupShowAllAlertsButton() {
        showAllAlertsButton.setOnClickListener {
            alertaAdapter.updateAlertas(alertas)
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchAlerts() {
        db.collection("alertas")
            .orderBy("hora", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                alertas = result.mapNotNull { document ->
                    val texto = document.getString("text")
                    val timestamp = document.getTimestamp("hora")
                    if (texto != null && timestamp != null) {
                        Alerta(texto, timestamp)
                    } else {
                        null
                    }
                }
                alertaAdapter.updateAlertas(alertas)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "fetchAlerts: Error getting documents", exception)
            }
    }

    private fun fetchAlertsByDate(date: Date) {
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time

        db.collection("alertas")
            .whereGreaterThanOrEqualTo("hora", Timestamp(startOfDay))
            .whereLessThanOrEqualTo("hora", Timestamp(endOfDay))
            .orderBy("hora", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val filteredAlertas = result.mapNotNull { document ->
                    val texto = document.getString("text")
                    val timestamp = document.getTimestamp("hora")
                    if (texto != null && timestamp != null) {
                        Alerta(texto, timestamp)
                    } else {
                        null
                    }
                }
                alertaAdapter.updateAlertas(filteredAlertas)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "fetchAlertsByDate: Error getting documents", exception)
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

data class Alerta(val texto: String, val timestamp: Timestamp)