package com.example.securityzone

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    lateinit var bluetoothManager: BluetoothManager

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        bluetoothManager = BluetoothManager("00:22:11:30:EC:81") // Reemplaza con la dirección MAC real
    }
}