package saffih.elmdroid.service.client

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 7/05/17.
 */


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import saffih.elmdroid.ElmEngine
import saffih.elmdroid.Que


sealed class Msg {
    class Init : Msg()
    sealed class Service : Msg() {
        class Connected(val className: ComponentName, val service: IBinder) : Service()
        class Disconnected(val className: ComponentName) : Service()
    }

    class Request(val payload: Message) : Msg()
}

data class Model(val service: MService = MService())

data class MService(val mConnection: ServiceConnection? = null,
                    val messenger: Messenger? = null,
                    val bound: Boolean = false)


abstract class ElmMessengerServiceClient<API>(val me: Context,
                                              val javaClassName: Class<*>,
                                              val toApi: (Message) -> API,
                                              val toMessage: (API) -> Message,
                                              val debug: Boolean = false) :
        ElmEngine<Model, Msg>() {
    init {
        start()
    }

    override fun init(): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }

    fun request(payload: API) {
        request(toMessage(payload))
    }

    private fun request(payload: Message) {
        dispatch(Msg.Request(payload))
    }

    open fun onConnected(msg: MService): Unit {}
    open fun onDisconnected(msg: MService): Unit {}
    abstract fun onAPI(msg: API)

    private val handler = object : Handler() {
        override fun handleMessage(message: Message) {
            val msg = toApi(message)
            onAPI(msg)
        }
    }
    private val replyMessenger = Messenger(handler)

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {

            is Msg.Init -> {
                val (m, c) = update(msg, model.service)
                ret(model.copy(service = m), c)
            }
            is Msg.Service -> {
                val (m, c) = update(msg, model.service)
                ret(model.copy(service = m), c)
            }
            is Msg.Request -> {
                val message = msg.payload
                message.replyTo = replyMessenger
                model.service.messenger!!.send(message)
                ret(model)
            }
        }

    }

    private fun update(msg: Msg.Service, model: MService): Pair<MService, Que<Msg>> {
        return when (msg) {
            is Msg.Service.Connected -> {
                ret(model.copy(messenger = Messenger(msg.service), bound = true))
            }
            is Msg.Service.Disconnected -> {
                ret(model.copy(messenger = null, bound = false))
            }
        }
    }


    private fun update(msg: Msg.Init, model: MService): Pair<MService, Que<Msg>> {
        return ret(model.copy(mConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                // This is called when the connection with the service has been
                // established, giving us the object we can use to
                // interact with the service.  We are communicating with the
                // service using a Messenger, so here we get a client-side
                // representation of that from the raw IBinder object.
                dispatch(Msg.Service.Connected(className, service))
            }

            override fun onServiceDisconnected(className: ComponentName) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                dispatch(Msg.Service.Disconnected(className))
            }
        }))
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model, pre) {
            view(model.service, pre?.service)
        }
    }

    private fun view(model: MService, pre: MService?) {
        val setup = {}
        checkView(setup, model, pre) {
            if (model.bound) onConnected(model)
            else onDisconnected(model)
        }

    }


    fun onStart() {
//        start()
        // Bind to the service
        val startService = Intent(me, javaClassName)
        startService.putExtra("MESSENGER", replyMessenger)

        me.bindService(startService, myModel.service.mConnection,
                Context.BIND_AUTO_CREATE)
    }

    fun onStop() {
        // Unbind from the service
        if (myModel.service.bound)
            me.unbindService(myModel.service.mConnection)
    }

}


