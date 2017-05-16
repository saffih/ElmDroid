package saffih.elmdroid.gps.child

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 9/05/17.
 */


import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import saffih.elmdroid.ElmChild
import saffih.elmdroid.Que
import saffih.elmdroid.activityCheckForPermission


sealed class Msg {
    companion object {
        fun requestLocationMsg() = Api.Request.Location()
        fun replyLocationMsg(location: Location) = Api.Reply.NotifyLocation(location)
    }


    class Init : Msg()
    sealed class Response : Msg() {
        data class Disabled(val provider: String) : Response()
        data class Enabled(val provider: String) : Response()
        data class StatusChanged(val provider: String,
                                 val status: Int,
                                 val extras: android.os.Bundle? = null) : Response()

        data class LocationChanged(val location: Location) : Response()
    }

    sealed class Api : Msg() {
        companion object {
            fun locate() = Request.Location()
        }

        sealed class Request : Api() {
            class Location : Request()
        }

        sealed class Reply : Api() {
            class NotifyLocation(val location: Location) : Reply()
        }

    }
}



data class Model(
        val enabled: Boolean = false,
        val lastLocation: Location? = null,
        //        val time: java.util.Date? = null,
        val status: Int = 0,
        val state: MState = MState()
)

data class MState(val listeners: List<LocationAdapter> = listOf<LocationAdapter>())


abstract class ElmGpsChild(val me: Context) : ElmChild<Model, Msg>() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }

    fun unregister() {
        LocationAdapter.unregisterAll()
    }

    override fun init(): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                ret(model)
            }
            is Msg.Response -> { // for the order sake  - just
                return when (msg) {
                    is Msg.Response.Disabled -> {
                        toast("disabled ${msg}")
                        ret(model.copy(enabled = false))
                    }
                    is Msg.Response.Enabled -> {
                        toast("enabled ${msg}")
                        ret(model.copy(enabled = true))
                    }
                    is Msg.Response.StatusChanged -> {
                        toast("status changed ${msg}")
                        ret(model)
                    }
                    is Msg.Response.LocationChanged -> {
                        toast("location changed ${msg}")
                        ret(model.copy(lastLocation = msg.location)
                                , Msg.Api.Reply.NotifyLocation(msg.location)
                        )
                    }
                }
            }
            is Msg.Api -> {
                val (m, c) = update(msg, model.state)
                ret(model.copy(state = m), c)
            }
        }
    }

    abstract fun onLocationChanged(location: Location)

    fun update(msg: Msg.Api, model: MState): Pair<MState, Que<Msg>> {
        return when (msg) {
            is Msg.Api.Request.Location ->
                if (model.listeners.isEmpty())
                    ret(model.copy(listeners = startListenToGps()))
                else
                    ret(model)
            is Msg.Api.Reply.NotifyLocation -> {
                model.listeners.forEach { it.unregister() }
//                onLocationChanged(msg.location)
                ret(model.copy(listeners = listOf()))
            }
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model, pre) {
            viewEnabled(model.enabled, pre?.enabled)
            view(model.lastLocation, pre?.lastLocation)
            view(model.state, pre?.state)

        }
    }

    private fun view(model: MState, pre: MState?) {
        val setup = {}
        checkView(setup, model, pre) {

        }
    }

    private fun view(model: Location?, pre: Location?) {
        val setup = {}
        checkView(setup, model, pre) {
            model!!
            toast("LocationChanged  ${model}")
            onLocationChanged(model)

        }
    }

    private fun viewEnabled(model: Boolean, pre: Boolean?) {
        val setup = { }
        checkView(setup, model, pre) {
        }
    }


    fun toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
        val handler = Handler(Looper.getMainLooper())
        handler.post({ Toast.makeText(me, txt, duration).show() })
    }


    fun startListenToGps(): List<LocationAdapter> {
        val lm: LocationManager = me.getSystemService(Context.LOCATION_SERVICE)
                as LocationManager
        val allListeners = listOf(
                LocationProviderDisabledListener(lm) { dispatch(Msg.Response.Disabled(it)) },
                LocationProviderEnabledListener(lm) { dispatch(Msg.Response.Enabled(it)) },
                LocationStatusChangedListener(lm) { provider, status, extras ->
                    dispatch(Msg.Response.StatusChanged(provider, status, extras))
                },
                LocationChangedListener(lm) { dispatch(Msg.Response.LocationChanged(it)) })

        allListeners.forEach { it.registerAt() }
        return allListeners
    }


}

open class LocationAdapter(val locationManager: LocationManager) : LocationListener {

    override fun onLocationChanged(location: Location?) {
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    fun registerAt() {
        val minTime: Long = 5000L
        val minDistance: Float = 10.toFloat()
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, minTime, minDistance, this)
        track.add(this)
    }

    operator fun invoke() {
        registerAt()
    }

    fun unregister() {
        locationManager.removeUpdates(this)
        track.remove(this)
    }


    companion object {
        fun checkPermission(me: Activity) {
            activityCheckForPermission(me, "android.permission.ACCESS_FINE_LOCATION", 1)
        }

        private val track = mutableSetOf<LocationAdapter>()
        fun unregisterAll() = track.asIterable().forEach { it.unregister() }
    }
}


open class LocationProviderEnabledListener(
        lm: LocationManager,
        val f: (String) -> Unit) : LocationAdapter(lm) {
    override fun onProviderEnabled(provider: String?) {
        f(provider!!)
    }
}

open class LocationProviderDisabledListener(
        lm: LocationManager,
        val f: (String) -> Unit) : LocationAdapter(lm) {
    override fun onProviderDisabled(provider: String?) {
        f(provider!!)
    }
}

open class LocationStatusChangedListener(
        lm: LocationManager,
        val f: (String, Int, android.os.Bundle?) -> Unit) : LocationAdapter(lm) {
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
        f(provider!!, status, extras)
    }
}

open class LocationChangedListener(
        lm: LocationManager,
        val f: (Location) -> Unit) : LocationAdapter(lm) {
    override fun onLocationChanged(location: Location?) {
        f(location!!)
    }
}


