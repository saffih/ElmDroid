/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */
package saffih.elmdroid.gps

import saffih.elmdroid.Que
import saffih.elmdroid.gps.child.*
import saffih.elmdroid.service.ElmMessengerBoundService


class GpsElm(override val me: android.app.Service) : ElmMessengerBoundService<Model, Msg, Msg.Api>(me,
        toApi = { it.toApi() },
        toMessage = { it.toMessage() }) {
    val child = ElmGpsChild(me, { dispatch(it) })

//    private fun dispatchBack(que: Que<Msg>) {
//
//        que.lst.forEach { dispatchReply(it as Msg.Api) }
//    }

    override fun init(): Pair<Model, Que<Msg>> {
        return child.init()
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        val (m, c) = child.update(msg, model)

        // send response
        c.lst.forEach { if (it is Msg.Api) dispatchReply(it) }
        // process rest
        return ret(m, c.lst.filter { it !is Msg.Api })
    }

    override fun view(model: Model, pre: Model?) {
        return child.view(model, pre)
    }

}
