package com.example.securityzone

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.*

@SuppressLint("StaticFieldLeak")
object AppContext {
    lateinit var context: Context
}

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var statusTextView: TextView
    private lateinit var bluetoothStatusTextView: TextView
    private var spanish = true
    private var plumaLevantada = false
    private val db = FirebaseFirestore.getInstance()
    private lateinit var bluetoothService: BluetoothService
    private var isBound = false

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.getService()
            isBound = true
            updateBluetoothStatus(getString(R.string.bluetooth_connected))
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            updateBluetoothStatus(getString(R.string.bluetooth_disconnected))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppContext.context = applicationContext
        loadLocale()
        setContentView(R.layout.activity_main)



        preferencesManager = PreferencesManager(this)
        statusTextView = findViewById(R.id.textView2)
        bluetoothStatusTextView = findViewById(R.id.bluetoothStatusTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButtonClickListeners()
        setupCountryToggle()
        updateStatusTextView()

        // Set initial Bluetooth status
        updateBluetoothStatus(getString(R.string.bluetooth_connecting))

        // Bind to BluetoothService
        Intent(this, BluetoothService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        requestBluetoothPermissions()
    }

    private fun updateBluetoothStatus(status: String) {
        runOnUiThread {
            bluetoothStatusTextView.text = status
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

    private fun togglePluma() {
        plumaLevantada = !plumaLevantada
        val command = if (plumaLevantada) "PLUMA_UP" else "PLUMA_DOWN"
        if (isBound) {
            if (bluetoothService.sendCommand(command)) {
                updatePlumaButtonText()
                Toast.makeText(this, getString(R.string.command_positive) + ": " + command, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.command_negative), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, (getString(R.string.bluetooth_disconnect_msg)), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePlumaButtonText() {
        val plumaBtn = findViewById<Button>(R.id.plumaBtn)
        plumaBtn.text = if (plumaLevantada) getString(R.string.pluma_up) else getString(R.string.pluma_down)
    }

    private fun handleBluetoothData(data: String) {
        when (data.trim()) {
            "E" -> registrarEvento("entrada")
            "S" -> registrarEvento("salida")
            else -> {
                Toast.makeText(this, getString(R.string.unknown) + ": " + data, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registrarEvento(tipoEvento: String) {
        val entradaSalida = hashMapOf(
            "tipo" to tipoEvento,
            "hora" to Timestamp.now()
        )

        db.collection("entradasSalidas")
            .add(entradaSalida)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, getString(R.string.reg_positive) + ": " + tipoEvento, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, (getString(R.string.reg_negative)) + ": " + e, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        updateStatusTextView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}