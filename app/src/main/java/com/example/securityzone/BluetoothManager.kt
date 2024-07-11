package com.example.securityzone

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.util.*

class BluetoothManager(private val deviceAddress: String) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var dataReceivedListener: ((String) -> Unit)? = null

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    fun connect(): Boolean {
        if (isConnected()) return true

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            return false
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(deviceAddress)
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()

            // Iniciar un hilo para leer datos entrantes
            Thread {
                val inputStream: InputStream = bluetoothSocket!!.inputStream
                val buffer = ByteArray(1024)
                var bytes: Int

                while (true) {
                    try {
                        bytes = inputStream.read(buffer)
                        val readMessage = String(buffer, 0, bytes)
                        dataReceivedListener?.invoke(readMessage)
                    } catch (e: IOException) {
                        break
                    }
                }
            }.start()

            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun sendCommand(command: String): Boolean {
        if (!isConnected()) {
            if (!connect()) return false
        }

        return try {
            bluetoothSocket?.outputStream?.write(command.toByteArray())
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun setDataReceivedListener(listener: (String) -> Unit) {
        dataReceivedListener = listener
    }

    fun close() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
