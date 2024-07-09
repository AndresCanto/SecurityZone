package com.example.securityzone

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale


class MsjMotivoActivity : AppCompatActivity() {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Cargar el idioma guardado antes de establecer el contenido de la vista
        loadLocale()
        setContentView(R.layout.activity_mensaje)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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

    private fun setupButtonClickListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.remainderBtn).setOnClickListener {
            showBloquearDialog()
        }
    }

    private fun showBloquearDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_msj_bloquear, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editText)

        builder.setView(dialogLayout)
            .setTitle("Bloquear")
            .setPositiveButton("Confirmar") { _, _ ->
                val inputText = editText.text.toString()
                if (inputText.isNotEmpty()) {
                    readTxtField(inputText) { success ->
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

    private fun readTxtField(textF: String,  onComplete: (Boolean) -> Unit) {
        val data = hashMapOf(
            "text" to textF,
            "hora" to pickDate(),
            "remainder" to true
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

    private fun pickDate(): Timestamp {
        return Timestamp(Date())
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
}