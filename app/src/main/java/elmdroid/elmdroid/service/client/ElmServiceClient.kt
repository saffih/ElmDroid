package elmdroid.elmdroid.service.client

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 7/05/17.
 */


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import elmdroid.elmdroid.ElmEngine
import elmdroid.elmdroid.Que
import elmdroid.elmdroid.service.Messageable


sealed class Msg {
    class Init : Msg()
    sealed class Service : Msg() {
        class Connected(val className: ComponentName, val service: IBinder) : Service()
        class Disconnected(val className: ComponentName) : Service()
    }

    class Request(val payload: Messageable) : Msg()
}

data class Model(val service: MService = MService())

data class MService(val mConnection: ServiceConnection? = null,
                    val messenger: Messenger? = null,
                    val bound: Boolean = false)


abstract class ElmServiceClient<API : Messageable>(override val me: Context,
                                                   val javaClassName: Class<*>) :
        ElmEngine<Model, Msg>(me) {
    init {
        start()
    }

    override fun init(savedInstanceState: Bundle?): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }

    fun request(payload: Messageable) {
        dispatch(Msg.Request(payload))
    }

    abstract fun onAPI(msg: API)
    abstract fun toMsg(message: Message): API


    private val handler = object : Handler() {
        override fun handleMessage(message: Message) {
            val msg = toMsg(message)
            if (msg == null) super.handleMessage(message)
            else {
                onAPI(msg)
            }
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
                val message = msg.payload.toMessage()
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
    }


    fun onStart() {
        // Bind to the service
        val startService = Intent(me, javaClassName)
        startService.putExtra("MESSENGER", replyMessenger)

        me.bindService(startService, model.service.mConnection,
                Context.BIND_AUTO_CREATE)
    }

    fun onStop() {
        // Unbind from the service
        if (model.service.bound)
            me.unbindService(model.service.mConnection)
    }

}
