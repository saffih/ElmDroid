/*
 * By Saffi Hartal, 2017.
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package saffih.elmdroid.gps

import android.app.Service
import android.location.Location
import android.os.Message
import saffih.elmdroid.gps.child.GpsChild
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

class GpsServiceApp(me: Service) : ElmMessengerService<Model, Msg, Msg.Api>(me,
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

    private val child = object : GpsChild(me) {
        override fun handleMSG(cur: Msg) {
            this@GpsServiceApp.dispatch(cur)
        }

        override fun onLocationChanged(location: Location) {
            // ok as remote service we use the reply
            dispatchReply(Msg.replyLocationMsg(location))
        }
    }

    override fun init(): Model {
        return child.init()
    }

    override fun update(msg: Msg, model: Model): Model {
        val m = child.update(msg, model)
        return m
    }

}
