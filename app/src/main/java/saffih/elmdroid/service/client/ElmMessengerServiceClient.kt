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
import saffih.elmdroid.ElmBase
import saffih.elmdroid.service.ElmMessengerService.Companion.startServiceIfNotRunning


sealed class Msg {
    class Init : Msg()
    sealed class Service : Msg() {
        class Connected(val className: ComponentName, val service: IBinder) : Service()
        class Disconnected(val className: ComponentName) : Service()
        class Pending(val pendingRequest: Request) : Service()
    }

    class Request(val payload: Message) : Msg()
}

data class Model(val service: MService = MService())

data class MService(val mConnection: ServiceConnection? = null,
                    val messenger: Messenger? = null,
                    val bound: Boolean = false,
                    val pending: List<Msg> = listOf<Msg>())


abstract class ElmMessengerServiceClient<API>(override val me: Context,
                                              val javaClassName: Class<*>,
                                              val toApi: (Message) -> API,
                                              val toMessage: (API) -> Message,
                                              val debug: Boolean = false) :
        ElmBase<Model, Msg>(me) {

    override fun init(): Model {
        dispatch(Msg.Init())
        return Model().copy(service = MService(mConnection = createServiceConnection()))
    }

    fun request(payload: API) {
        val toSend = Msg.Request(toMessage(payload))
        val service = myModel.service
        if (!service.bound) {
            dispatch(Msg.Service.Pending(toSend))
            init()
            bindToService()
            return
        }
        dispatch(toSend)
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

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {

            is Msg.Init -> {
                val m = update(msg, model.service)
                model.copy(service = m)
            }
            is Msg.Service -> {
                val m = update(msg, model.service)
                model.copy(service = m)
            }
            is Msg.Request -> {
                val message = msg.payload
                message.replyTo = replyMessenger
                model.service.messenger!!.send(message)
                model
            }
        }

    }

    private fun update(msg: Msg.Service, model: MService): MService {
        return when (msg) {
            is Msg.Service.Pending -> {
                model.copy(pending = model.pending + msg.pendingRequest)
            }
            is Msg.Service.Connected -> {
                val que = model.pending

                model.copy(messenger = Messenger(msg.service), bound = true,
                        pending = listOf())
            }
            is Msg.Service.Disconnected -> {
                model.copy(messenger = null, bound = false)
            }
        }
    }


    private fun update(msg: Msg.Init, model: MService): MService {
        return model.copy(mConnection = createServiceConnection())
    }

    private fun createServiceConnection(): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                // This is called when the connection with the service has been
                // established, giving us the object we can use to
                // interact with the service.  We are communicating with the
                // service using a Messenger, so here we get a client-side
                // representation of that from the raw IBinder object.
                post { dispatch(Msg.Service.Connected(className, service)) }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                post { dispatch(Msg.Service.Disconnected(className)) }
            }
        }
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

    override fun onCreate() {

        onCreateBound()
    }

    fun onCreateUnbound() {
        super.onCreate()
        startUnbound()
    }

    fun onCreateBound() {
        super.onCreate()
        bindToService()
    }

    private fun bindToService() {
        // Bind to the service
        val bindIntent = Intent(me, javaClassName)
        bindIntent.putExtra("MESSENGER", replyMessenger)

        me.bindService(bindIntent, myModel.service.mConnection,
                Context.BIND_AUTO_CREATE)
    }

    private fun startUnboundAndBind() {
        startUnbound()
        bindToService()
    }

    fun startUnbound() {
        startServiceIfNotRunning(me, javaClassName, replyMessenger)
    }

    fun sendPayload(payload: Message) {
        startServiceIfNotRunning(me, javaClassName, replyMessenger)
    }

    override fun onDestroy() {
        // Unbind from the service
        if (myModel.service.bound)
            me.unbindService(myModel.service.mConnection)
        super.onDestroy()
    }

}

