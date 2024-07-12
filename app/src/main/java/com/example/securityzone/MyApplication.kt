package com.example.securityzone

import android.app.Application
import android.content.Intent
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    lateinit var bluetoothService: BluetoothService

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        startBluetoothService()
    }

    private fun startBluetoothService() {
        val intent = Intent(this, BluetoothService::class.java)
        startService(intent)
    }
}