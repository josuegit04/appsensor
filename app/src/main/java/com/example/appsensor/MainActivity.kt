package com.example.appsensor

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var txtSensor: TextView
    private lateinit var btnVibrar: Button
    private lateinit var btnVerificar: Button

    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_PHONE_STATE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                1
            )
        }

        txtSensor = findViewById(R.id.txtSensor)
        btnVibrar = findViewById(R.id.btnVibrar)
        btnVerificar = findViewById(R.id.btnVerificar)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        btnVerificar.setOnClickListener {
            mostrarAlertaConexiones()
        }

        btnVibrar.setOnClickListener {
            vibrarTelefono()
        }
    }

    private fun mostrarAlertaConexiones() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val textoWifi = if (wifiManager.isWifiEnabled) "Wi-Fi: ACTIVADO ✅" else "Wi-Fi: DESACTIVADO ❌"

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val textoBluetooth = if (adapter != null && adapter.isEnabled) "Bluetooth: ACTIVADO ✅" else "Bluetooth: DESACTIVADO ❌"

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var datosActivos = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    datosActivos = telephonyManager.isDataEnabled
                } else {
                    datosActivos = false
                }
            }
        } catch (e: SecurityException) {
            datosActivos = false
        }

        val textoDatos = if (datosActivos) "Datos Móviles: ACTIVADO ✅" else "Datos Móviles: DESACTIVADO ❌ (o sin permisos)"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Estado de Conexiones")
        builder.setMessage("$textoWifi\n\n$textoBluetooth\n\n$textoDatos")
        builder.setPositiveButton("Entendido") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun vibrarTelefono() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    override fun onResume() {
        super.onResume()
        acelerometro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0].toInt()
            val y = event.values[1].toInt()
            val z = event.values[2].toInt()
            txtSensor.text = "X: $x  |  Y: $y  |  Z: $z"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}