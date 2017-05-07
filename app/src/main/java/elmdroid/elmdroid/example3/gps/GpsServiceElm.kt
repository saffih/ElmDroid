/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */
package elmdroid.elmdroid.example3.gps

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Message
import elmdroid.elmdroid.Que
import elmdroid.elmdroid.service.ElmMessengerBoundService
import elmdroid.elmdroid.service.Messageable


enum class API {
    Connected,
    RequestLocation,
    NotifyLocation
}

sealed class Msg {
    class Init : Msg()
    sealed class Response : Msg() {
        data class Disabled(val provider: String) : Msg.Response()
        data class Enabled(val provider: String) : Msg.Response()
        data class StatusChanged(val provider: String,
                                 val status: Int,
                                 val extras: android.os.Bundle? = null) : Msg.Response()

        data class LocationChanged(val location: Location) : Msg.Response()
    }

    sealed class Api : Msg(), Messageable {
        class Connected : Api() {
            override fun toMessage(): Message = Message.obtain(null, API.Connected.ordinal)
        }

        class RequestLocation : Api() {
            override fun toMessage(): Message = Message.obtain(null, API.RequestLocation.ordinal)
        }

        class NotifyLocation(val location: Location) : Api() {
            override fun toMessage(): Message = Message.obtain(null, API.NotifyLocation.ordinal, location)
        }
    }
}


fun Message.toApi(): Msg.Api {
    return when (this.what) {
        API.Connected.ordinal -> Msg.Api.Connected()
        API.RequestLocation.ordinal -> Msg.Api.RequestLocation()
        API.NotifyLocation.ordinal -> Msg.Api.NotifyLocation(this.obj as Location)
        else -> {
            throw RuntimeException("${this} has no 'what' value set")
        }
    }
}


data class Model(
        val enabled: Boolean = false,
        val forced: Boolean = false,
        val lastLocation: Location? = null,
        val time: java.util.Date? = null,
        val status: Int = 0,
        val api: MApi = MApi()
)

data class MApi(val listeners: List<LocationAdapter> = listOf<LocationAdapter>())


class GpsElm(override val me: android.app.Service) : ElmMessengerBoundService<Model, Msg>(me) {


    override fun toMsg(message: android.os.Message): Msg? {
        return message.toApi()
    }


    override fun init(savedInstanceState: android.os.Bundle?): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }


    fun enableGps() {
        val intent = android.content.Intent("android.location.GPS_ENABLED_CHANGE")
        intent.putExtra("enabled", true)
        me.sendBroadcast(intent)
    }

    fun disableGps() {
        val intent = android.content.Intent("android.location.GPS_ENABLED_CHANGE")
        intent.putExtra("enabled", false)
        me.sendBroadcast(intent)
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                ret(model)
            }
            is Msg.Response -> {
                return when (msg) {
                    is Msg.Response.Disabled -> {
                        enableGps()
                        ret(model.copy(forced = true))
                    }
                    is Msg.Response.Enabled -> {
                        ret(model.copy(enabled = true))
                    }
                    is Msg.Response.StatusChanged -> {
                        toast("status changed ${msg}")
                        ret(model)
                    }
                    is Msg.Response.LocationChanged -> {
                        if (model.forced) {
                            disableGps()
                        }
                        ret(model.copy(lastLocation = msg.location))
                    }
                }
            }
            is Msg.Api -> {
                val (m, c) = update(msg, model.api)
                ret(model.copy(api = m))
            }
        }
    }

    fun update(msg: Msg.Api, model: MApi): Pair<MApi, Que<Msg>> {
        return when (msg) {
            is Msg.Api.RequestLocation -> {
                if (model.listeners.isEmpty())
                    ret(model.copy(listeners = startListenToGps()))
                else ret(model)
            }
            is Msg.Api.NotifyLocation -> {
                // won't happen
                ret(model)
            }
            is Msg.Api.Connected -> {
                ret(model)
            }
        }

    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model.lastLocation, pre?.lastLocation) {
            toast("LocationChanged  ${model}")
            dispatchReply(Msg.Api.NotifyLocation(model.lastLocation!!))
        }
    }


}

fun GpsElm.startListenToGps(): List<LocationAdapter> {
    val lm: LocationManager = me.getSystemService(android.content.Context.LOCATION_SERVICE)
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
    }

    operator fun invoke() {
        registerAt()
    }

    fun unregister() {
        locationManager.removeUpdates(this)
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





