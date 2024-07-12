package com.example.securityzone

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class BloquearActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var statusTextView: TextView
    private lateinit var bloquearButton: Button
    private lateinit var desbloquearButton: Button
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var bluetoothManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadLocale()
        setContentView(R.layout.activity_bloquear)

        bluetoothManager = (application as MyApplication).bluetoothManager
        preferencesManager = PreferencesManager(this)
        statusTextView = findViewById(R.id.statusTextView)
        bloquearButton = findViewById(R.id.button2)
        desbloquearButton = findViewById(R.id.button)

        setupButtonClickListeners()
        updateUI()
    }

    private fun setupButtonClickListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        bloquearButton.setOnClickListener {
            showBloquearDialog()
        }

        desbloquearButton.setOnClickListener {
            sendUnblockCommandToArduino()
        }
    }

    private fun showBloquearDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_bloquear, null)
        val radioGroup = dialogLayout.findViewById<RadioGroup>(R.id.radioGroup)

        builder.setView(dialogLayout)
            .setTitle(getString(R.string.lock_title)) // Aquí se utiliza getString() para obtener la cadena del archivo strings.xml
            .setPositiveButton(getString(R.string.positive)) { _, _ ->
                val selectedOption = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioButton1 -> dialogLayout.findViewById<RadioButton>(R.id.radioButton1).text.toString()
                    R.id.radioButton2 -> dialogLayout.findViewById<RadioButton>(R.id.radioButton2).text.toString()
                    R.id.radioButton3 -> dialogLayout.findViewById<RadioButton>(R.id.radioButton3).text.toString()
                    else -> ""
                }
                if (selectedOption.isNotEmpty()) {
                    sendBlockCommandToArduino(selectedOption)
                } else {
                    showSaveResult(false)
                }
            }
            .setNegativeButton(getString(R.string.negative)) { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun sendBlockCommandToArduino(selectedOption: String) {
        Thread {
            if (bluetoothManager.sendCommand("BLOCK")) {
                runOnUiThread {
                    preferencesManager.isBlocked = true
                    updateUI()
                    val messageWithHeader = "Alert: $selectedOption"
                    readTxtField(messageWithHeader) { success ->
                        showSaveResult(success)
                    }
                    Toast.makeText(this, (getString(R.string.lock_positive)), Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, (getString(R.string.lock_negative)), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendUnblockCommandToArduino() {
        Thread {
            if (bluetoothManager.sendCommand("UNBLOCK")) {
                runOnUiThread {
                    preferencesManager.isBlocked = false
                    updateUI()
                    Toast.makeText(this, (getString(R.string.unlock_positive)), Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, (getString(R.string.unlock_positive)), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun readTxtField(textF: String, onComplete: (Boolean) -> Unit) {
        val data = hashMapOf(
            "text" to textF,
            "hora" to com.google.firebase.Timestamp(Date())
        )

        db.collection("alertas")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("BloquearActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.w("BloquearActivity", "Error adding document", e)
                onComplete(false)
            }
    }

    private fun showSaveResult(success: Boolean) {
        val message = if (success) {
            (getString(R.string.save_result_positive))
        } else {
            (getString(R.string.save_result_negative))
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
            statusTextView.text = (getString(R.string.status_blocked))
            statusTextView.setBackgroundColor(resources.getColor(R.color.red, theme))
            bloquearButton.isVisible = false
            desbloquearButton.isVisible = true
        } else {
            statusTextView.text = (getString(R.string.status_open))
            statusTextView.setBackgroundColor(resources.getColor(R.color.green, theme))
            bloquearButton.isVisible = true
            desbloquearButton.isVisible = false
        }
    }

    private fun setLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("Language", language)
        editor.apply()
    }

    private fun loadLocale() {
        val sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("Language", "en") ?: "en"
        setLocale(this, language)
    }
}
