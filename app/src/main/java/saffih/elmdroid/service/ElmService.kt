package saffih.elmdroid.service

import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast
import saffih.elmdroid.ElmEngine

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 7/05/17.
 */


abstract class ElmMessengerBoundService<M, MSG, API : MSG>(
        open val me: Service,
        val toMessage: (API) -> Message,
        val toApi: (Message) -> API, val debug: Boolean = false) :
        ElmEngine<M, MSG>() {
//    init {
//        start()
//    }

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
        val extras = intent.extras
        clientMessenger = extras?.get("MESSENGER") as Messenger
        if (notStarted()) {
            start()
        }
        return mMessenger.binder
    }

    fun onCreate() { // for long lived services
        start()
    }


    fun onDestroy() {}
}
