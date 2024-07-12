package com.example.securityzone

import android.content.*
import android.content.ContentValues.TAG
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
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
    private lateinit var bluetoothService: BluetoothService
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadLocale()
        setContentView(R.layout.activity_bloquear)

        preferencesManager = PreferencesManager(this)
        statusTextView = findViewById(R.id.statusTextView)
        bloquearButton = findViewById(R.id.button2)
        desbloquearButton = findViewById(R.id.button)

        setupButtonClickListeners()
        updateUI()

        // Bind to BluetoothService
        Intent(this, BluetoothService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
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
            .setTitle("Bloquear")
            .setPositiveButton("Confirmar") { _, _ ->
                val selectedOption = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioButton1 -> dialogLayout.findViewById<RadioButton>(R.id.radioButton1).text.toString()
                    R.id.radioButton2 -> dialogLayout.findViewById<RadioButton>(R.id.radioButton2).text.toString()
                    R.id.radioButton3 -> dialogLayout.findViewById<RadioButton>(R.id.radioButton3).text.toString()
                    else -> ""
                }
                if (selectedOption.isNotEmpty()) {
                    sendBlockCommand(selectedOption)
                } else {
                    showSaveResult(false)
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun sendBlockCommand(selectedOption: String) {
        if (isBound) {
            if (bluetoothService.sendCommand("BLOCK")) {
                preferencesManager.isBlocked = true
                updateUI()
                val messageWithHeader = "Alerta: $selectedOption"
                readTxtField(messageWithHeader) { success ->
                    runOnUiThread {
                        showSaveResult(success)
                        Toast.makeText(this, "Comando de bloqueo enviado al Arduino y registrado en Firestore", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Error al enviar el comando de bloqueo por Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Servicio Bluetooth no conectado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendUnblockCommandToArduino() {
        if (isBound) {
            if (bluetoothService.sendCommand("UNBLOCK")) {
                preferencesManager.isBlocked = false
                updateUI()
                Toast.makeText(this, "Comando de desbloqueo enviado al Arduino y estado actualizado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al enviar el comando de desbloqueo por Bluetooth", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Servicio Bluetooth no conectado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readTxtField(textF: String, onComplete: (Boolean) -> Unit) {
        val data = hashMapOf(
            "text" to textF,
            "hora" to com.google.firebase.Timestamp(Date()),
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

    private fun showSaveResult(success: Boolean) {
        val message = if (success) {
            "Bloqueo activado y guardado con éxito"
        } else {
            "Error al guardar el bloqueo. Por favor, inténtelo de nuevo."
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
            statusTextView.text = "Status: Cerrado"
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