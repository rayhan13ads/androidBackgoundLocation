package com.example.backlocatonupdate.services

import android.content.Context
import android.location.Location
import android.preference.PreferenceManager
import java.lang.StringBuilder
import java.text.DateFormat
import java.util.*

public class Commons {
    companion object{

        val KEY_REQUEST_LOCATION_UPDATES = "LocationUpdatesEnable"

       fun getLocationText(location:Location):String{

           if (location == null){
               return  "Unknow Locaton"
           }else{
               var latlng = StringBuilder().append(location.latitude)
                   .append("/")
                   .append(location.longitude)
               return latlng.toString()
           }
       }

        fun getLocationTitle(myLocationService: MyLocationService): CharSequence? {
            return "Location Update : ${DateFormat.getDateInstance().format(Date())}"
        }

        fun setRequestingLocationUpdates(context: Context, b: Boolean) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_REQUEST_LOCATION_UPDATES, b)
        }

        fun requestingLocatonUpdates(context: Context): Boolean {
           return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_REQUEST_LOCATION_UPDATES,false)
        }
    }
}
