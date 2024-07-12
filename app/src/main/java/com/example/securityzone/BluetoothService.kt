package com.example.securityzone

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothService : Service() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    private val deviceAddress: String = "00:22:11:30:EC:81" // Replace with your device address
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val binder = LocalBinder()
    private var readThread: Thread? = null

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectToDevice()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun connectToDevice() {
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        device?.let {
            try {
                bluetoothSocket = it.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket.connect()
                inputStream = bluetoothSocket.inputStream
                outputStream = bluetoothSocket.outputStream
                startReadingData()
            } catch (e: IOException) {
                Log.e("BluetoothService", "Error connecting to device", e)
                stopSelf()
            }
        }
    }

    private fun startReadingData() {
        readThread = Thread {
            val buffer = ByteArray(1024)
            try {
                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        for (i in 0 until bytesRead) {
                            val receivedByte = buffer[i].toInt() and 0xFF
                            Log.d("BluetoothService", "Received raw byte: $receivedByte")

                            val evento = when (receivedByte) {
                                1 -> "entrada"
                                2 -> "salida"
                                else -> {
                                    Log.w("BluetoothService", "Received unexpected value: $receivedByte")
                                    continue  // Salta a la siguiente iteración del bucle
                                }
                            }

                            Log.d("BluetoothService", "Interpreted event: $evento")
                            val intent = Intent("BluetoothDataReceived")
                            intent.putExtra("evento", evento)
                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("BluetoothService", "Error reading from input stream", e)
            }
        }
        readThread?.start()
    }

    fun sendCommand(command: String): Boolean {
        return try {
            outputStream.write(command.toByteArray())
            true
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error sending command", e)
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket.close()
            readThread?.interrupt()
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error closing socket", e)
        }
    }
}