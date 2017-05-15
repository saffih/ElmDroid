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
import android.os.Message
import android.widget.Toast
import saffih.elmdroid.ElmChild
import saffih.elmdroid.Que
import saffih.elmdroid.activityCheckForPermission


sealed class Msg {
    fun isReply() = this is Api.Reply
    class Init : Msg()
    sealed class Response : Msg() {
        data class Disabled(val provider: String) : Response()
        data class Enabled(val provider: String) : Response()
        data class StatusChanged(val provider: String,
                                 val status: Int,
                                 val extras: android.os.Bundle? = null) : Response()

        data class LocationChanged(val location: Location) : Response()
    }
    sealed class Step:Msg(){
        class Start : Step()
        class Done(val location: Location) :Step()
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


/**
 * For Remoting
 */
enum class API {
    RequestLocation,
    NotifyLocation
}


fun Msg.Api.toMessage(): Message {
    return when (this) {
        is Msg.Api.Request.Location -> Message.obtain(null, API.RequestLocation.ordinal)
        is Msg.Api.Reply.NotifyLocation -> Message.obtain(null, API.NotifyLocation.ordinal, location)
    }
}


fun Message.toApi(): Msg.Api {
    return when (this.what) {
        API.RequestLocation.ordinal -> Msg.Api.Request.Location()
        API.NotifyLocation.ordinal -> Msg.Api.Reply.NotifyLocation(this.obj as Location)
        else -> {
            throw RuntimeException("${this} has no 'what' value set")
        }
    }
}
///////////////////////////////////


data class Model(
        val enabled: Boolean = false,
        val forced: Boolean = false,
        val lastLocation: Location? = null,
        val time: java.util.Date? = null,
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

    /**
     * The parent delegator should use the following pattern
     *     override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
     *     val (m, c) = child.update(msg, model)
     *     // send response
     *     c.lst.forEach { if (it is Msg.Api) dispatchReply(it) }
     *     // process rest
     *     return ret(m, c.lst.filter { it !is Msg.Api })
     *     }

     */
    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                ret(model)
            }
            is Msg.Response -> {
                return when (msg) {
                    is Msg.Response.Disabled -> {
                        toast("disabled ${msg}")
                        ret(model.copy(forced = true))
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
                        ret(model.copy(lastLocation = msg.location),
                                Msg.Step.Done(msg.location))
                    }
                }
            }
            is Msg.Api -> {

                val (m, c) = update(msg, model.state)
                ret(model.copy(state = m), c)
            }
            is Msg.Step -> {
                val (m, c) = update(msg, model.state)
                ret(model.copy(state = m), c)
            }

        }
    }

    private fun update(msg: Msg.Step, model: MState): Pair<MState, Que<Msg>> {
        return when(msg){
            is Msg.Step.Start ->
                if (model.listeners.isEmpty())
                    ret(model.copy(listeners = startListenToGps()))
                else
                    ret(model)
            is Msg.Step.Done -> {
                model.listeners.forEach { it.unregister() }
                onLocationChanged(msg.location)
                val replyMsg = Msg.Api.Reply.NotifyLocation(msg.location)
                onReplyNotifyLocation(replyMsg)
                ret(model.copy(listeners = listOf()), replyMsg)

            }
        }
    }

    // Shugaring
    fun RequestLocation() = Msg.Api.Request.Location()


    abstract fun onReplyNotifyLocation(replyMsg: Msg.Api.Reply.NotifyLocation)
    abstract fun onLocationChanged(location: Location)

    fun update(msg: Msg.Api, model: MState): Pair<MState, Que<Msg>> {
        return when (msg) {
            is Msg.Api.Request.Location -> ret(model, Msg.Step.Start())
            is Msg.Api.Reply.NotifyLocation ->
                // the client got my response.
                ret(model)
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model.lastLocation, pre?.lastLocation) {
            toast("LocationChanged  ${model}")
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
            activityCheckForPermission(me, "android.permission.RECEIVE_SMS", 1)
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





