package com.example.securityzone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var statusTextView: TextView
    private lateinit var bluetoothManager: BluetoothManager
    private var spanish = true
    private var plumaLevantada = false

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadLocale()
        setContentView(R.layout.activity_main)

        preferencesManager = PreferencesManager(this)
        statusTextView = findViewById(R.id.textView2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButtonClickListeners()
        setupCountryToggle()
        updateStatusTextView()

        bluetoothManager = (application as MyApplication).bluetoothManager
        requestBluetoothPermissions()
        connectBluetooth()
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
        findViewById<ImageButton>(R.id.mensajeBtn).setOnClickListener {
            startActivity(Intent(this, MsjMotivoActivity::class.java))
        }

        findViewById<ImageButton>(R.id.bloquearBtn).setOnClickListener {
            startActivity(Intent(this, BloquearActivity::class.java))
        }

        findViewById<ImageButton>(R.id.alertaBtn).setOnClickListener {
            startActivity(Intent(this, AlertasActivity::class.java))
        }

        findViewById<ImageButton>(R.id.informeBtn).setOnClickListener {
            startActivity(Intent(this, InformeActivity::class.java))
        }

        findViewById<Button>(R.id.plumaBtn).setOnClickListener {
            togglePluma()
        }
    }

    private fun updateStatusTextView() {
        if (preferencesManager.isBlocked) {
            statusTextView.text = getString(R.string.status_blocked)
            statusTextView.setBackgroundColor(resources.getColor(R.color.red, theme))
        } else {
            statusTextView.text = getString(R.string.status_open)
            statusTextView.setBackgroundColor(resources.getColor(R.color.green, theme))
        }
    }

    private fun setupCountryToggle() {
        val toggleButton = findViewById<ToggleButton>(R.id.toggleIdioma)
        val sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("Language", "en") ?: "en"

        val isSpanish = language == "es"
        toggleButton.isChecked = isSpanish
        updateToggleButtonBackground(toggleButton, isSpanish)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            val newLanguage = if (isChecked) "es" else "en"
            if (newLanguage != language) {
                setLocale(this, newLanguage)
                recreate()
            }
        }
    }

    private fun updateToggleButtonBackground(button: ToggleButton, isSpanish: Boolean) {
        if (isSpanish) {
            button.setBackgroundResource(R.drawable.espana)
        } else {
            button.setBackgroundResource(R.drawable.usa)
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun connectBluetooth() {
        if (!bluetoothManager.isConnected()) {
            Thread {
                if (bluetoothManager.connect()) {
                    runOnUiThread {
                        Toast.makeText(this, "Conectado al dispositivo Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "No se pudo conectar al dispositivo Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun togglePluma() {
        plumaLevantada = !plumaLevantada
        val command = if (plumaLevantada) "PLUMA_UP" else "PLUMA_DOWN"
        Thread {
            if (bluetoothManager.sendCommand(command)) {
                runOnUiThread {
                    updatePlumaButtonText()
                    Toast.makeText(this, "Comando enviado: $command", Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Error al enviar el comando", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updatePlumaButtonText() {
        val plumaBtn = findViewById<Button>(R.id.plumaBtn)
        plumaBtn.text = if (plumaLevantada) getString(R.string.pluma_up) else getString(R.string.pluma_down)
    }

    override fun onResume() {
        super.onResume()
        updateStatusTextView()
        if (!bluetoothManager.isConnected()) {
            connectBluetooth()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No cerramos la conexión Bluetooth aquí para mantenerla activa entre actividades
    }
}