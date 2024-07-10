package com.example.securityzone

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.*

class BluetoothManager(private val deviceAddress: String) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar para SPP

    fun connect(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            return false
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(deviceAddress)
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun sendCommand(command: String): Boolean {
        return try {
            bluetoothSocket?.outputStream?.write(command.toByteArray())
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun close() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}