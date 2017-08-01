package elmdroid.elmdroid.example3

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.os.Message
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import elmdroid.elmdroid.R
import saffih.elmdroid.ElmBase
import saffih.elmdroid.gps.GpsElmServiceClient
import saffih.elmdroid.gps.GpsLocalService
import saffih.elmdroid.permissionGranted
import saffih.elmdroid.post
import saffih.elmdroid.service.LocalServiceClient
import saffih.elmdroid.service.client.MService
import saffih.elmdroid.gps.child.Msg as GpsMsg
import saffih.elmdroid.gps.child.Msg.Api as GpsMsgApi
import saffih.elmdroid.service.client.Msg as ClientServiceMsg


class MapsActivity : FragmentActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.onCreate()
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
    fun toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
        me.post({ Toast.makeText(me, txt, duration).show() })
    }

    fun getPerm(): Boolean {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        return me.permissionGranted(perm)
    }

    // example using local service
    val localServiceGps = object :
            LocalServiceClient<GpsLocalService>(me, localserviceJavaClass = GpsLocalService::class.java) {
        override fun onReceive(payload: Message?) {
            val location = payload?.obj as Location
            post { dispatch(Msg.Activity.GotLocation(location)) }
        }

        override fun onConnected() {
            super.onConnected()
            post {
                toast("Using local service")
                fun must_succeed() {
                    val sbound = bound!!
                    if (getPerm()) {
                        sbound.request()
                    } else {
                        post { must_succeed() }
                    }
                }
                must_succeed()
            }
        }
    }

    // example using service
    val serviceGps = object : GpsElmServiceClient(me) {
        override fun onAPI(msg: GpsMsgApi) {
            when (msg) {
                is saffih.elmdroid.gps.child.Msg.Api.Reply.NotifyLocation ->
                    post { dispatch(Msg.Activity.GotLocation(msg.location)) }
            }
        }

        override fun onConnected(msg: MService) {
            post {
                toast("Using service")
                fun must_succeed() {
                    if (getPerm()) {
                        request(saffih.elmdroid.gps.child.Msg.Api.Request.Location())
                    } else {
                        post { must_succeed() }
                    }
                }
                must_succeed()
            }
        }
    }

    val useLocal by lazy {
        val action = me.intent.action
        action == "localService"
    }

    override fun onCreate() {
        super.onCreate()
        // Bind to the service
        if (useLocal) localServiceGps.onCreate()
        else serviceGps.onCreate()
    }

    override fun onDestroy() {
        // Unbind from the service
        if (useLocal) localServiceGps.onDestroy()
        else serviceGps.onDestroy()
        super.onDestroy()
    }

    override fun init(): Model {
        dispatch(Msg.Init)
        return Model()
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> {
                model
            }
            is Msg.Activity -> {
                val activityModel =
                        update(msg, model.activity)
                model.copy(activity = activityModel)
            }
        }
    }

    fun update(msg: Msg.Activity, model: MActivity): MActivity {
        return when (msg) {
            is Msg.Activity.Map -> {
                val mapModel = update(msg, model.mMap)
                model.copy(mMap = mapModel)
            }
            is Msg.Activity.GotLocation -> {
                val m = update(msg, model.mMap)
                model.copy(mMap = m)
            }
        }
    }

    private fun update(msg: Msg.Activity.GotLocation, model: MMap): MMap {
        val here = LatLng(msg.location.latitude, msg.location.longitude)
        return model.copy(markers = model.markers + MarkerOptions().position(here).title("you are here"),
                camera = (CameraUpdateFactory.newLatLng(here)))
    }

    fun update(msg: Msg.Activity.Map, model: MMap): MMap {
        return when (msg) {

            is Msg.Activity.Map.Ready -> {
                model.copy(googleMap = msg.googleMap)
            }
            is Msg.Activity.Map.AddMarker -> {
                model.copy(markers = model.markers + msg.markerOptions)
            }
            is Msg.Activity.Map.MoveCamera -> {
                model.copy(camera = msg.cameraUpdate)
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
            checkView({}, model.markers, pre?.markers) {
                model.markers.forEach {
                    model.googleMap!!.addMarker(it)
                }
            }
            checkView({}, model.camera, pre?.camera) {
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

