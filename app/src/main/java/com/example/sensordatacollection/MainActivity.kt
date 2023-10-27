package com.example.sensordatacollection

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Objects


class MainActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManagers: SensorManager? = null
    private var sensorManagerG: SensorManager? = null
    private var senAccelerometor: Sensor? = null
    private var senGyroscope: Sensor? = null
    private var GPSx: TextView? = null
    private var GPSy: TextView? = null
    private var MyFusedLocationClient: FusedLocationProviderClient? = null
    private var lastUpdate: Long = 0
    private var lastUpdate_gyro: Long = 0
    private val locationRequestCode = 1000
    private var wayLatitude = 0.0
    private var wayLongitude = 0.0
    var i = 0
    var fileString = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_mode_bg)
        MyFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManagers = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManagerG = getSystemService(SENSOR_SERVICE) as SensorManager
        assert(sensorManagers != null)
        senAccelerometor = sensorManagers!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        senGyroscope = sensorManagerG!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                locationRequestCode
            )
        } else {
            val T = Toast.makeText(
                applicationContext,
                "Location & file access Permission Granted",
                Toast.LENGTH_SHORT
            )
            T.show()
        }
        val toggle = findViewById<View>(R.id.startTracking) as Switch
        toggle.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                i = 1
                onResume()
            } else {
                i = 0
                onPause()
            }
        }
    }

    fun FileWriters(str: String) {
        val dateObj = SimpleDateFormat("dd/MM/yyyy")
        val calendar = Calendar.getInstance()
        val date = dateObj.format(calendar.time)
        val path = File(Objects.requireNonNull(getExternalFilesDir(null))!!.absolutePath + "/DC_data")
        if (!path.exists()) {
            path.mkdirs()
            val txt = findViewById<View>(R.id.city) as TextView
            txt.text = date
        }
        val file = File(path, "DC_data.csv")
        try {
            if (!file.exists()) {
                file.createNewFile()
                val fOut = FileOutputStream(file, true)
                //                OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
//                outWriter.append("GPS_Lat, GPS_Long, AX, AY, AZ, GX, GY, GZ\n");
//                outWriter.close();
                fOut.write(" AX, AY, AZ, GPS_Lat, GPS_Long, GX, GY, GZ\n".toByteArray())
                //                fOut.flush();
                fOut.close()
            }
            val fOut = FileOutputStream(file, true)
            //            OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
//            outWriter.append(str);
//
//            outWriter.close();
            fOut.write(str.toByteArray())
            fOut.flush()
            fOut.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed")
        }
        fileString = ""


//        try {
//            File file = new File(date + ".csv");
//            if (!file.exists()){
//                file.createNewFile();
//            }
//        }catch (IOException e){
//            e.printStackTrace();
//        }
    }

    override fun onPause() {
        super.onPause()
        sensorManagers!!.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (i == 1) {
            sensorManagers!!.registerListener(
                this,
                senAccelerometor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            sensorManagers!!.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun GetNewLocation() {
        MyFusedLocationClient?.flushLocations()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        MyFusedLocationClient!!.getLastLocation().addOnSuccessListener(this) { location ->
            if (location != null) {
                wayLatitude = location.getLatitude()
                wayLongitude = location.getLongitude()
                GPSx = findViewById<View>(R.id.gpsLatitude) as TextView
                GPSx!!.text = "" + wayLatitude
                GPSy = findViewById<View>(R.id.gpsLongitude) as TextView
                GPSy!!.text = "" + wayLongitude
                fileString = "$fileString$wayLatitude, $wayLongitude, "
            }
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val mySensor = sensorEvent.sensor
        val GSensor = sensorEvent.sensor
        if (GSensor.type == Sensor.TYPE_GYROSCOPE) {
            val gx = sensorEvent.values[0]
            val gy = sensorEvent.values[1]
            val gz = sensorEvent.values[2]
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdate_gyro > 300) {
                lastUpdate_gyro = currentTime
                run {
                    GetNewLocation()
                    val sX = java.lang.Float.toString(gx)
                    var text = findViewById<View>(R.id.gyroscopeX) as TextView
                    text.text = sX
                    val sY = java.lang.Float.toString(gy)
                    text = findViewById<View>(R.id.gyroscopeY) as TextView
                    text.text = sY
                    val sZ = java.lang.Float.toString(gz)
                    text = findViewById<View>(R.id.gyroscopeZ) as TextView
                    text.text = sZ
                    fileString = "$fileString$sX, $sY, $sZ, "
                }
            }
        }
        if (mySensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdate > 300) {
                val timeDiff = currentTime - lastUpdate
                lastUpdate = currentTime
                run {
                    val sX = java.lang.Float.toString(x)
                    var text = findViewById<View>(R.id.accelerometerX) as TextView
                    text.text = sX
                    val progressBar =
                        findViewById<View>(R.id.gyroTilt) as ProgressBar
                    val progress = ((-1 * x + 10) * 10000).toInt()
                    progressBar.progress = progress
                    val sY = java.lang.Float.toString(y)
                    text = findViewById<View>(R.id.accelerometerY) as TextView
                    text.text = sY
                    val sZ = java.lang.Float.toString(z)
                    text = findViewById<View>(R.id.accelerometerZ) as TextView
                    text.text = sZ
                    fileString = "$fileString$sX, $sY, $sZ\n"
                    FileWriters(fileString)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}