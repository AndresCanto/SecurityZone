package com.example.securityzone

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.firestore.FirebaseFirestore
import java.sql.Timestamp
import java.util.Date
import android.content.res.Configuration
import java.util.Locale

class BloquearActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var statusTextView: TextView
    private lateinit var bloquearButton: Button
    private lateinit var desbloquearButton: Button
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Cargar el idioma guardado antes de establecer el contenido de la vista
        loadLocale()
        setContentView(R.layout.activity_bloquear)

        val unlockButton: Button = findViewById(R.id.button)
        unlockButton.setOnClickListener {
            // Lógica para desbloquear
        }

        val lockButton: Button = findViewById(R.id.button2)
        lockButton.setOnClickListener {
            // Lógica para bloquear
        }

        preferencesManager = PreferencesManager(this)
        statusTextView = findViewById(R.id.statusTextView)
        bloquearButton = findViewById(R.id.button2)
        desbloquearButton = findViewById(R.id.button)

        setupButtonClickListeners()
        updateUI()

    }

    private fun readTxtField(textF: String, onComplete: (Boolean) -> Unit) {
        val data = hashMapOf(
            "text" to textF,
            "hora" to com.google.firebase.Timestamp(Date())
        )

        db.collection("alertas")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
                onComplete(false)
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
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        bloquearButton.setOnClickListener {
            showBloquearDialog()
        }

        desbloquearButton.setOnClickListener {
            preferencesManager.isBlocked = false
            updateUI()
        }
    }

    fun showBloquearDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_bloquear, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editText)

        builder.setView(dialogLayout)
            .setTitle("Bloquear")
            .setPositiveButton("Confirmar") { _, _ ->
                val inputText = editText.text.toString()
                if (inputText.isNotEmpty()) {
                    preferencesManager.isBlocked = true
                    readTxtField(inputText) { success ->
                        updateUI()
                        showSaveResult(success)
                    }
                } else {
                    showSaveResult(false)
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun showSaveResult(success: Boolean) {
        val message = if (success) {
            "Bloqueo activado y guardado con éxito"
        } else {
            "Error al guardar el bloqueo. Por favor, intente de nuevo."
        }

        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateUI() {
        if (preferencesManager.isBlocked) {
            statusTextView.text = "Status: Bloqueado"
            statusTextView.setBackgroundColor(resources.getColor(R.color.red, theme))
            bloquearButton.isVisible = false
            desbloquearButton.isVisible = true
        } else {
            statusTextView.text = "Status: Abierto"
            statusTextView.setBackgroundColor(resources.getColor(R.color.green, theme))
            bloquearButton.isVisible = true
            desbloquearButton.isVisible = false
        }
    }
}