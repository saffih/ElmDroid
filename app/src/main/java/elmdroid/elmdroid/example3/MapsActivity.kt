package elmdroid.elmdroid.example3

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import elmdroid.elmdroid.R
import saffih.elmdroid.ElmBase
import saffih.elmdroid.Que
import saffih.elmdroid.activityCheckForPermission
import saffih.elmdroid.gps.GpsService
import saffih.elmdroid.gps.child.*
import saffih.elmdroid.service.client.ElmMessengerServiceClient
import saffih.elmdroid.service.client.MService
import saffih.elmdroid.gps.child.Msg as GpsMsg
import saffih.elmdroid.gps.child.Msg.Api as GpsMsgApi
import saffih.elmdroid.service.client.Msg as ClientServiceMsg


class MapsActivity : FragmentActivity() {

    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.start()
    }

    override fun onStart() {
        super.onStart()
        app.onCreate()
    }

    override fun onStop() {
        super.onStop()
        app.onDestroy()
    }
}


sealed class Msg {
    object Init : Msg()

    sealed class Activity : Msg() {
        sealed class Map : Activity() {
            class Ready(val googleMap: GoogleMap) : Map()
            class AddMarker(val markerOptions: MarkerOptions) : Map()
            class MoveCamera(val cameraUpdate: CameraUpdate) : Map()
        }

        class FirstRequest : Activity()
        class GotLocation(val location: Location) : Activity()

    }
}


/**
 * Model representing the state of the system
 * All Model types are Prefixed with M
 */
data class Model(val activity: MActivity = MActivity())

data class MActivity(val mMap: MMap = MMap())
data class MMap(val googleMap: GoogleMap? = null,
                val markers: Set<MarkerOptions> = setOf<MarkerOptions>(),
                val camera: CameraUpdate? = null)

class ElmApp(override val me: FragmentActivity) : ElmBase<Model, Msg>(me), OnMapReadyCallback {
    inner class GpsElmRemoteServiceClient(me: Context) :
            ElmMessengerServiceClient<GpsMsgApi>(me, javaClassName = GpsService::class.java,
                    toApi = { it.toApi() },
                    toMessage = { it.toMessage() }) {

        override fun onAPI(msg: GpsMsgApi) {
            when (msg) {
                is saffih.elmdroid.gps.child.Msg.Api.Reply.NotifyLocation ->
                    postDispatch(Msg.Activity.GotLocation(msg.location))
            }
        }

        override fun onConnected(msg: MService) {
            postDispatch(Msg.Activity.FirstRequest())
        }
    }

    val gps = GpsElmRemoteServiceClient(me)
    override fun onCreate() {
        super.onCreate()
        // Bind to the service
        gps.onCreate()
    }

    override fun onDestroy() {
        // Unbind from the service
        gps.onDestroy()
        super.onDestroy()
    }

    override fun init() = ret(Model(), Msg.Init)

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                ret(model)
            }
            is Msg.Activity -> {
                val (activityModel, que) =
                        update(msg, model.activity)
                ret(model.copy(activity = activityModel), que)
            }
        }
    }

    fun update(msg: Msg.Activity, model: MActivity): Pair<MActivity, Que<Msg>> {
        return when (msg) {
            is Msg.Activity.Map -> {
                val (mapModel, que) = update(msg, model.mMap)
                ret(model.copy(mMap = mapModel), que)
            }
            is Msg.Activity.GotLocation -> {
                val here = LatLng(msg.location.latitude, msg.location.longitude)

                val que = listOf(
                        Msg.Activity.Map.AddMarker(MarkerOptions().position(here).title("you are here")),
                        Msg.Activity.Map.MoveCamera(CameraUpdateFactory.newLatLng(here)))
                ret(model, que)
            }
            is Msg.Activity.FirstRequest -> {
                val perm = Manifest.permission.ACCESS_FINE_LOCATION
                val code = 1
                if (activityCheckForPermission(me, perm, code) ){
                    gps.request(saffih.elmdroid.gps.child.Msg.Api.Request.Location())
                    ret(model)
                }else {
                    ret(model, msg)
                }
            }
        }
    }

    fun update(msg: Msg.Activity.Map, model: MMap): Pair<MMap, Que<Msg>> {
        return when (msg) {

            is Msg.Activity.Map.Ready -> {
                ret(model.copy(googleMap = msg.googleMap))
            }
            is Msg.Activity.Map.AddMarker -> {
                ret(model.copy(markers = model.markers + msg.markerOptions))

            }
            is Msg.Activity.Map.MoveCamera -> {
                ret(model.copy(camera = msg.cameraUpdate))
            }
        }

    }

    override fun view(model: Model, pre: Model?) {
        checkView({}, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = {
            me.setContentView(R.layout.activity_maps)
        }
        checkView(setup, model, pre) {
            view(model.mMap, pre?.mMap)
        }
    }

    private fun view(model: MMap, pre: MMap?) {
        val setup = {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            val mapFragment = me.supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
        checkView(setup, model, pre) {
            checkView({}, model.markers, pre?.markers){
                model.markers.forEach {
                    model.googleMap!!.addMarker(it)
                }
            }
            checkView({}, model.camera, pre?.camera){
                model.googleMap!!.moveCamera(model.camera)
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        dispatch(Msg.Activity.Map.Ready(googleMap))

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        dispatch(Msg.Activity.Map.AddMarker(MarkerOptions().position(sydney).title("Marker in Sydney")))
        dispatch(Msg.Activity.Map.MoveCamera(CameraUpdateFactory.newLatLng(sydney)))
    }
}

