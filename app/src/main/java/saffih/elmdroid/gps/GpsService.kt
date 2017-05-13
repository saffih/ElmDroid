package saffih.elmdroid.gps

import android.app.Service
import android.content.Intent


// bound service

class GpsService : Service() {
    val elm = GpsElm(this)

    override fun onBind(intent: android.content.Intent): android.os.IBinder? {
        return elm.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        elm.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

}

