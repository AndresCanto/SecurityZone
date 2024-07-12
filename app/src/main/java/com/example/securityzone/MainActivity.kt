package com.example.securityzone

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.widget.Button
import java.util.*

@SuppressLint("StaticFieldLeak")
object AppContext {
    lateinit var context: Context
}

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var statusTextView: TextView
    private lateinit var bluetoothStatusTextView: TextView
    private lateinit var vehicleCountTextView: TextView
    private lateinit var manualSalidaBtn: Button
    private var plumaLevantada = false
    private val db = FirebaseFirestore.getInstance()
    private lateinit var bluetoothService: BluetoothService
    private var isBound = false
    private var localEntradasCount = 0
    private var localSalidasCount = 0

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
        vehicleCountTextView = findViewById(R.id.vehicleCountTextView)
        manualSalidaBtn = findViewById(R.id.manualSalidaBtn) // Referencia al nuevo botón

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButtonClickListeners()
        setupCountryToggle()
        updateStatusTextView()
        updateVehicleCount()

        updateBluetoothStatus(getString(R.string.bluetooth_connecting))

        Intent(this, BluetoothService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        requestBluetoothPermissions()
        val filter = IntentFilter("BluetoothDataReceived")
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDataReceiver, filter)

        listenForVehicleChanges()

        manualSalidaBtn.setOnClickListener {
            Toast.makeText(this, getString(R.string.salida_detectada), Toast.LENGTH_SHORT).show()
            localSalidasCount++
            updateVehicleCount()
            registrarEvento("salida")
        }
    }

    private fun manualSalida() {
        Toast.makeText(this, getString(R.string.salida_detectada), Toast.LENGTH_SHORT).show()
        localSalidasCount++
        updateVehicleCount()
        registrarEvento("salida")
    }

    private val bluetoothDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val evento = intent?.getStringExtra("evento")
            evento?.let {
                Log.d("MainActivity", "Evento recibido: $it")
                when (it) {
                    "entrada" -> {
                        Toast.makeText(this@MainActivity, getString(R.string.entrada_detectada), Toast.LENGTH_SHORT).show()
                        localEntradasCount++
                        updateVehicleCount()
                        registrarEvento("entrada")
                    }
                    "salida" -> {
                        Toast.makeText(this@MainActivity, getString(R.string.salida_detectada), Toast.LENGTH_SHORT).show()
                        localSalidasCount++
                        updateVehicleCount()
                        registrarEvento("salida")
                    }
                }
            }
        }
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
        findViewById<ImageButton>(R.id.mensajeBtn)?.setOnClickListener {
            startActivity(Intent(this, MsjMotivoActivity::class.java))
        }

        findViewById<ImageButton>(R.id.bloquearBtn)?.setOnClickListener {
            startActivity(Intent(this, BloquearActivity::class.java))
        }

        findViewById<ImageButton>(R.id.alertaBtn)?.setOnClickListener {
            startActivity(Intent(this, AlertasActivity::class.java))
        }

        findViewById<ImageButton>(R.id.informeBtn)?.setOnClickListener {
            startActivity(Intent(this, InformeActivity::class.java))
        }

        findViewById<Button>(R.id.plumaBtn)?.setOnClickListener {
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
                changeLanguage(newLanguage)
            }
        }
    }

    private fun changeLanguage(newLanguage: String) {
        setLocale(this, newLanguage)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }, 100)
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

    private fun registrarEvento(tipoEvento: String) {
        val entradaSalida = hashMapOf(
            "tipo" to tipoEvento,
            "hora" to Timestamp.now()
        )

        db.collection("entradasSalidas")
            .add(entradaSalida)
            .addOnSuccessListener { documentReference ->
                Log.d("MainActivity", "Evento registrado exitosamente: $tipoEvento")
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al registrar evento: $e")
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.reg_negative) + ": " + e, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun listenForVehicleChanges() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        db.collection("entradasSalidas")
            .whereGreaterThanOrEqualTo("hora", Timestamp(today))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MainActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var entradasCount = 0
                    var salidasCount = 0

                    for (doc in snapshot.documents) {
                        when (doc.getString("tipo")) {
                            "entrada" -> entradasCount++
                            "salida" -> salidasCount++
                        }
                    }

                    localEntradasCount = entradasCount
                    localSalidasCount = salidasCount
                    updateVehicleCount()
                }
            }
    }

    private fun updateVehicleCount() {
        vehicleCountTextView.text = "Entradas: $localEntradasCount | Salidas: $localSalidasCount"
    }

    override fun onResume() {
        super.onResume()
        updateStatusTextView()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothDataReceiver)
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("plumaLevantada", plumaLevantada)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        plumaLevantada = savedInstanceState.getBoolean("plumaLevantada", false)
        updatePlumaButtonText()
    }
}