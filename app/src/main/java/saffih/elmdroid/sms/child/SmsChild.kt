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

package saffih.elmdroid.sms.child


import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Toast
import saffih.elmdroid.Que
import saffih.elmdroid.StateChild

//import saffih.elmdroid.sms.toast


sealed class Msg {
    companion object {
        fun received(received: List<SmsMessage>) = Msg.Received(received)
    }

    class Init : Msg()
    data class Received(val received: List<SmsMessage>) : Msg()
}


class Model

data class MSms(val address: String, val text: String)

abstract class SmsChild(val me: Context) : StateChild<Model, Msg>() {
    abstract fun onSmsArrived(sms: List<SmsMessage>)

    val smsReceiver = SMSReceiverAdapter(
            hook = { arr: Array<out SmsMessage?> -> dispatch(Msg.received(arr.filterNotNull())) })

    //    for services
    override fun onCreate() {
        super.onCreate()
        smsReceiver.meRegister(me)
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver.meUnregister(me)
    }

    override fun init(): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                ret(model)
            }
            is Msg.Received -> {
                onSmsArrived(msg.received)
                ret(model)
            }
        }
    }

    val smsManager = SmsManager.getDefault()

    fun sendSms(data: MSms) {
        smsManager.sendTextMessage(
                data.address,
                null,
                data.text, null, null)
    }
}

fun SmsChild.toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
    val handler = Handler(Looper.getMainLooper())
    handler.post({ Toast.makeText(me, txt, duration).show() })
}

