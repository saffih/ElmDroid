package saffih.elmdroid.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.widget.Toast
import saffih.elmdroid.ElmBase


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 7/05/17.
 */


fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}


abstract class ElmMessengerService<M, MSG, API : MSG>(
        override val me: Service,
        val toMessage: (API) -> Message,
        val toApi: (Message) -> API, val debug: Boolean = false) :
        ElmBase<M, MSG>(me) {

    fun toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
        if (!debug) return
        val handler = Handler(Looper.getMainLooper())
        handler.post({ Toast.makeText(me, txt, duration).show() })
    }


    private var lastincomingMessage: Message? = null

    protected fun dispatchReply(msg: API) {
        val message = toMessage(msg)
        val last = lastincomingMessage!!
        val replyTo = last.replyTo
        if (replyTo !== null) {
            replyTo.send(message)
        } else if (clientMessenger != null)
            clientMessenger?.send(message)
        else {
            toast("no Messenger to reply ${msg}", duration = Toast.LENGTH_LONG)
        }
    }

    private val handler = object : Handler() {
        override fun handleMessage(message: Message) {
            lastincomingMessage = message
            val msg = toApi(message)
            if (msg == null) {
                super.handleMessage(message)
            } else {
                // any reply would use dispatchReply to return the response.
                dispatch(msg)
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private val mMessenger = Messenger(handler)

    private var clientMessenger: Messenger? = null

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    fun onBind(intent: Intent): IBinder {
        setMessenger(intent)
        return mMessenger.binder
    }


    fun onRebind(intent: Intent) {
        setMessenger(intent)

        // issue refresh...
    }


    fun onUnbind(intent: Intent): Boolean {
        clientMessenger = null
        return true
    }

    // for unbound service
    fun onStartCommand(intent: Intent, flags: Int, startId: Int) {
        setMessenger(intent)
    }

    private fun setMessenger(intent: Intent): Messenger? {
        val extras = intent.extras
        clientMessenger = extras?.get("MESSENGER") as Messenger?
        val dbg = extras?.get("PAYLOAD") as Message?
        if (dbg != null) {
            handler.handleMessage(dbg)
        }
        return clientMessenger
    }

    companion object {
        fun startService(context: Context, serviceClass: Class<*>, messenger: Messenger? = null,
                         payload: Message? = null) {
            if (!context.isServiceRunning(serviceClass) || (messenger != null) || (payload != null)) {
                val startIntent = Intent(context, serviceClass)
                if (messenger != null) {
                    startIntent.putExtra("MESSENGER", messenger)
                }
                if (payload != null) {
                    startIntent.putExtra("PAYLOAD", payload)
                }
                context.startService(startIntent)
            }
        }
    }
}

