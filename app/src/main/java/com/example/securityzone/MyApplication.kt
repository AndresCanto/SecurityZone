package com.example.securityzone

import android.app.Application

class MyApplication : Application() {
    lateinit var bluetoothManager: BluetoothManager

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = BluetoothManager("00:22:11:30:EC:81") // Reemplaza con la dirección MAC real
    }
}