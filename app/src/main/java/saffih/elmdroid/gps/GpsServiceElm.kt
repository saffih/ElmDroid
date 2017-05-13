/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */
package saffih.elmdroid.gps

import android.app.Service
import android.location.Location
import saffih.elmdroid.Que
import saffih.elmdroid.bind
import saffih.elmdroid.gps.child.*
import saffih.elmdroid.service.ElmMessengerBoundService


class GpsElm(me: Service) : ElmMessengerBoundService<Model, Msg, Msg.Api>(me,
        toApi = { it.toApi() },
        toMessage = { it.toMessage() }) {

    private val child = bind(object : ElmGpsChild(me) {
        override fun onReplyNotifyLocation(replyMsg: Msg.Api.Reply.NotifyLocation) {
            dispatchReply(replyMsg)
        }

        override fun onLocationChanged(location: Location) {
            // ok as remote service we use the reply
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
        return child.view(model, pre)
    }

}
