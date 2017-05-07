package elmdroid.elmdroid.example3.gps

import android.app.Service
import android.content.Intent
import android.os.IBinder


// bound service

class GpsService : Service() {
    val elm = GpsElm(this)

    override fun onBind(intent: Intent): IBinder? {
        return elm.onBind(intent)
    }
}

