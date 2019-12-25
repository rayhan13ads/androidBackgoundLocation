package com.example.backlocatonupdate.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.content.res.Resources
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.backlocatonupdate.MainActivity
import com.google.android.gms.location.*
import org.greenrobot.eventbus.EventBus

class MyLocationService : Service() {

    private val EXTRA_STARTED_FROM_NOTIFICATION = "com.example.backlocatonupdate.services" + ".started_from_notification"
    private  val mIBinder = LocalBinder()
    private var mChangingConfiguration = false
    private lateinit var mNotificationManager:NotificationManager

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var mServiceHandler: Handler
    private lateinit var mLocation: Location
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient


   private companion object{
        val CHANNEL_ID = "my_channel"
        val UPDATE_INTERVAL_IN_MIL = 10000
        val FASTEST_UPDATE_INTERVAL_IN_MUL = UPDATE_INTERVAL_IN_MIL / 2
        val NOTI_ID = 1223
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var startedNotification:Boolean = intent!!.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,false)

        if (startedNotification){
            removeLocationUpdates()
            stopSelf()
        }

        return START_NOT_STICKY
    }



    override fun onCreate() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object :LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                onNewLocation(locationResult?.lastLocation)
            }
        }

        createLocationRequest()
        getLastLocation()

       var mServiceHandlerThread = HandlerThread("MyLocationService")
        mServiceHandlerThread.start()
        mServiceHandler = Handler(mServiceHandlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            var mChannel = NotificationChannel(CHANNEL_ID,"Backlocatonupdate",NotificationManager.IMPORTANCE_DEFAULT)
            mNotificationManager.createNotificationChannel(mChannel)
        }

    }


    private fun removeLocationUpdates() {
        try {
            mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
            Commons.setRequestingLocationUpdates(this,false)
        }catch (ex:SecurityException){
            Commons.setRequestingLocationUpdates(this,true)
            Log.e("MylocatonService" , "lost location permission . Could not remove updates ${ex.message}")
            Toast.makeText(this, "lost location permission . Could not remove updates ${ex.message}",Toast.LENGTH_LONG).show()
        }
    }



    private fun getLastLocation() {
        try {
            mFusedLocationProviderClient.lastLocation.addOnCompleteListener {
                if (it.isSuccessful && it.result != null){
                    mLocation = it.result!!
                }else{
                    Log.e("MylocatonService" , "Failed to get location")
                    Toast.makeText(this, "Failed to get location",Toast.LENGTH_LONG).show()
                }
            }
        }catch (ex:SecurityException){
            Toast.makeText(this, "Last location permission  ${ex.message}",Toast.LENGTH_LONG).show()
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_IN_MIL.toLong()
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MUL.toLong()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun onNewLocation(lastLocation: Location?) {
        mLocation = lastLocation!!
        EventBus.getDefault().postSticky(SendLocatonToActitvity(mLocation))

        //Update notification content if running as a foreground service
        if (serviceRunningInForeGround(this)){
            mNotificationManager.notify(NOTI_ID,getNotification())
        }
    }

    private fun getNotification(): Notification? {

        var intent = Intent(this,MyLocationService::class.java)
        var text:String = Commons.getLocationText(mLocation)
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION,true)
        var servicePandingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        var activityPendingIntent = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),0)
        var notifyAction = NotificationCompat.Action.Builder(android.R.drawable.ic_dialog_map, "Launch",activityPendingIntent).build()
        var notifyServiceAction = NotificationCompat.Action.Builder(android.R.drawable.ic_input_delete, "Launch",activityPendingIntent).build()
        val bulider = NotificationCompat.Builder(this)
            .addAction(notifyAction)
            .addAction(notifyServiceAction)
            .setContentText(text)
            .setContentTitle(Commons.getLocationTitle(this))
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(android.R.mipmap.sym_def_app_icon)
            .setWhen(System.currentTimeMillis())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            bulider.setChannelId(CHANNEL_ID)
        }

        return bulider.build()
    }

    private fun serviceRunningInForeGround(context: Context): Boolean {

        var manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service in manager.getRunningServices(Int.MAX_VALUE)){
            if (this.javaClass.simpleName.equals(service.javaClass.simpleName)){
                if (service.foreground){
                    return true
                }
            }

        }


        return false
    }


    override fun onBind(intent: Intent): IBinder {
        stopForeground(true)
        mChangingConfiguration = false
        return  mIBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!mChangingConfiguration && Commons.requestingLocatonUpdates(this)){
            startForeground(NOTI_ID,getNotification())
        }

        return true
    }

    override fun onDestroy() {

        mServiceHandler.removeCallbacks(null)
        super.onDestroy()
    }


    inner class LocalBinder: Binder() {

        fun getService():MyLocationService{
            return this@MyLocationService
        }
    }
}
