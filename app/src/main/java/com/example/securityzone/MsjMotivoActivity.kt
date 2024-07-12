package com.example.securityzone

import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
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
        setContentView(R.layout.activity_mensaje)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupButtonClickListeners()
        setupEditTextListener()

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

    private fun setupButtonClickListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        findViewById<Button>(R.id.remainderBtn).setOnClickListener {
            createRemainder()
        }
    }

    private fun setupEditTextListener() {
        val editText = findViewById<EditText>(R.id.editText)
        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                createRemainder()
                true
            } else {
                false
            }
        }
    }

    private fun createRemainder() {
        val editText = findViewById<EditText>(R.id.editText)

        val inputText = editText.text.toString()
        if (inputText.isNotEmpty()) {
            val messageWithHeader = "Mensaje: $inputText"
            sendMessageToArduino(inputText, messageWithHeader)
        } else {
            showSaveResult(false)
        }
    }

    private fun sendMessageToArduino(message: String, messageWithHeader: String) {
        if (isBound) {
            if (bluetoothService.sendCommand("MSG:$message")) {
                Toast.makeText(this, "Mensaje enviado al monitor", Toast.LENGTH_SHORT).show()
                readTxtField(messageWithHeader) { success ->
                    showSaveResult(success)
                }
            } else {
                Toast.makeText(this, "Error al enviar el mensaje al monitor", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Servicio Bluetooth no conectado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readTxtField(textF: String, onComplete: (Boolean) -> Unit) {
        val data = hashMapOf(
            "text" to textF,
            "hora" to pickDate(),
            "msj" to true
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
            "Mensaje enviado de forma exitosa"
        } else {
            "Error al enviar mensaje. Por favor, intente de nuevo."
        }

        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}