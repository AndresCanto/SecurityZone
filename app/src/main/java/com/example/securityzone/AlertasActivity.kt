package com.example.securityzone

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

import android.content.Context
import android.content.res.Configuration
import java.util.Locale


class AlertasActivity : AppCompatActivity() {
    private lateinit var textoAlerta: TextView
    private lateinit var fechaHoraAlerta: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var backButton: ImageButton

    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var alertas: List<Alerta> = listOf()
    private var currentIndex: Int = 0

    companion object {
        private const val TAG = "AlertasActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cargar el idioma guardado antes de establecer el contenido de la vista
        loadLocale()
        setContentView(R.layout.activity_alertas)

        Log.d(TAG, "onCreate: Initializing views")
        textoAlerta = findViewById(R.id.textoAlerta)
        fechaHoraAlerta = findViewById(R.id.fechaHoraAlerta)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        backButton = findViewById(R.id.backButton)

        fetchAlerts()
        setupButtonClickListeners()
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

    private fun fetchAlerts() {
        Log.d(TAG, "fetchAlerts: Fetching alerts from Firestore")
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
                if (alertas.isNotEmpty()) {
                    currentIndex = 0
                    displayCurrentAlert()
                } else {
                    Log.w(TAG, "fetchAlerts: No alerts found")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "fetchAlerts: Error getting documents", exception)
            }
    }

    private fun displayCurrentAlert() {
        if (alertas.isNotEmpty() && currentIndex in alertas.indices) {
            val alerta = alertas[currentIndex]
            textoAlerta.text = alerta.texto
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            fechaHoraAlerta.text = sdf.format(alerta.timestamp.toDate())
            Log.d(TAG, "displayCurrentAlert: Displaying alert at index $currentIndex")
        } else {
            Log.w(TAG, "displayCurrentAlert: Invalid index or empty alert list")
        }
    }

    private fun setupButtonClickListeners() {
        Log.d(TAG, "setupButtonClickListeners: Setting up button click listeners")
        prevButton.setOnClickListener {
            if (currentIndex < alertas.size - 1) {
                currentIndex++
                displayCurrentAlert()
            } else {
                Log.d(TAG, "setupButtonClickListeners: No newer alert to display")
            }
        }

        nextButton.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                displayCurrentAlert()
            } else {
                Log.d(TAG, "setupButtonClickListeners: No older alert to display")
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}

data class Alerta(val texto: String, val timestamp: Timestamp)
