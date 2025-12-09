package com.example.appsensor

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.Uri
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
    private var latitudActual: Double = 0.0
    private var longitudActual: Double = 0.0
    private var ubicacionEncontrada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        verificarYPedirPermisos()

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

    private fun verificarYPedirPermisos() {
        val permisosNecesarios = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val listaPermisosFaltantes = ArrayList<String>()
        for (permiso in permisosNecesarios) {
            if (ActivityCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                listaPermisosFaltantes.add(permiso)
            }
        }
        if (listaPermisosFaltantes.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listaPermisosFaltantes.toTypedArray(), 1)
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    datosActivos = telephonyManager.isDataEnabled
                }
            }
        } catch (e: Exception) { datosActivos = false }
        val textoDatos = if (datosActivos) "Datos Móviles: ACTIVADO ✅" else "Datos Móviles: DESACTIVADO ❌"

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsPrendido = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        var textoGps = if (gpsPrendido) "GPS: ENCENDIDO ✅" else "GPS: APAGADO ❌"

        ubicacionEncontrada = false
        if (gpsPrendido && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val locationFinal = locationGPS ?: locationNetwork

            if (locationFinal != null) {
                latitudActual = locationFinal.latitude
                longitudActual = locationFinal.longitude
                ubicacionEncontrada = true
                textoGps += "\n📍 Lat: $latitudActual\n📍 Long: $longitudActual"
            } else {
                textoGps += "\n(Buscando señal...)"
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Estado del Sistema")
        builder.setMessage("$textoWifi\n\n$textoBluetooth\n\n$textoDatos\n\n$textoGps")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        if (ubicacionEncontrada) {
            builder.setNeutralButton("🗺️ VER EN MAPA") { _, _ ->
                abrirMapa(latitudActual, longitudActual)
            }
        }
        builder.show()
    }

    private fun abrirMapa(lat: Double, long: Double) {
        val uri = Uri.parse("geo:$lat,$long?q=$lat,$long(Mi Ubicación)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
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
            txtSensor.text = "X: $x | Y: $y | Z: $z"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}