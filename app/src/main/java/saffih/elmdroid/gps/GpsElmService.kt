/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */
package saffih.elmdroid.gps

import android.app.Service
import android.location.Location
import android.os.Message
import saffih.elmdroid.Que
import saffih.elmdroid.bind
import saffih.elmdroid.gps.child.ElmGpsChild
import saffih.elmdroid.gps.child.Model
import saffih.elmdroid.gps.child.Msg
import saffih.elmdroid.service.ElmMessengerService

typealias MsgApi = saffih.elmdroid.gps.child.Msg.Api
/**
 * For Remoting
 */
enum class API {
    RequestLocation,
    NotifyLocation
}

fun Msg.isReply() = this is Msg.Api.Reply

fun Msg.Api.toMessage(): Message {
    return when (this) {
        is Msg.Api.Request.Location -> Message.obtain(null, API.RequestLocation.ordinal)
        is Msg.Api.Reply.NotifyLocation -> Message.obtain(null, API.NotifyLocation.ordinal, location)
    }
}


fun Message.toApi(): Msg.Api {
    return when (this.what) {
        API.RequestLocation.ordinal -> Msg.requestLocationMsg()
        API.NotifyLocation.ordinal -> Msg.replyLocationMsg(this.obj as Location)
        else -> {
            throw RuntimeException("${this} has no 'what' value set")
        }
    }
}
/////////////////////////////////////

class GpsElm(me: Service) : ElmMessengerService<Model, Msg, Msg.Api>(me,
        toApi = { it.toApi() },
        toMessage = { it.toMessage() }) {
    override fun onCreate() {
        super.onCreate()
        child.onCreate()
    }

    override fun onDestroy() {
        child.onDestroy()
        super.onDestroy()
    }

    private val child = bind(object : ElmGpsChild(me) {
        override fun onLocationChanged(location: Location) {
            // ok as remote service we use the reply
            dispatchReply(Msg.replyLocationMsg(location))
        }
    }) { it }

    override fun init(): Pair<Model, Que<Msg>> {
        return child.init()
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        val (m, c) = child.update(msg, model)
        return ret(m, c)
    }

    override fun view(model: Model, pre: Model?) {
        return child.impl.view(model, pre)
    }
}
