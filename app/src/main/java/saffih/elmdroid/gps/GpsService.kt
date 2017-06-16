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
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Message
import saffih.elmdroid.StateBase
import saffih.elmdroid.bindState
import saffih.elmdroid.gps.child.GpsChild
import saffih.elmdroid.service.LocalService
import saffih.elmdroid.service.LocalServiceClient
import saffih.elmdroid.gps.child.Model as GpsLocalServiceModel
import saffih.elmdroid.gps.child.Msg as GpsLocalServiceMsg


// bound service

class GpsService : Service() {
    val elm = GpsServiceApp(this)

    override fun onBind(intent: android.content.Intent): android.os.IBinder? {
        return elm.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        elm.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

    }

    override fun onDestroy() {
        elm.onDestroy()
        super.onDestroy()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

}

//  an exmaple for local service
// app as annonymous inner class
class GpsLocalService : LocalService() {
    // service loop app
    val app = object : StateBase<GpsLocalServiceModel, GpsLocalServiceMsg>(this) {
        val gps = bindState(
                object : GpsChild(this@GpsLocalService) {
                    override fun onLocationChanged(location: Location) {
                        broadcast(location)
                    }

                    private fun broadcast(location: Location) {
                        val msg = Message.obtain(null, 0, location)
                        broadcast(msg)
                    }
                }) { it }


        override fun onCreate() {
            super.onCreate()
            gps.onCreate()
        }

        override fun onDestroy() {
            super.onDestroy()
            gps.onDestroy()

        }

        override fun init() = gps.init()
        override fun update(msg: GpsLocalServiceMsg, model: GpsLocalServiceModel) = gps.update(msg, model)
    }

    // delegate to app
    override fun onCreate() {
        super.onCreate()
        app.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

    }

    fun request() = app.dispatch(GpsLocalServiceMsg.requestLocationMsg())
    override fun onDestroy() {
        unregisterAll()
        app.onDestroy()
        super.onDestroy()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

}

// example only
class GpsLocalServiceClient(me: Context) :
        LocalServiceClient<GpsLocalService>(me, localserviceJavaClass = GpsService::class.java) {
    override fun onReceive(payload: Message?) {
        payload?.obj as Location
    }
}


