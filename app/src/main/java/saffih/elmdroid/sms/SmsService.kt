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

package saffih.elmdroid.sms

// unused example

import android.app.Service
import android.os.Message
import android.telephony.SmsMessage
import saffih.elmdroid.service.ElmMessengerService
import saffih.elmdroid.sms.child.MSms
import saffih.elmdroid.sms.child.SmsChild
import saffih.elmdroid.sms.child.Model as CModel
import saffih.elmdroid.sms.child.Msg as CMsg

/**
 * Example of adding a layer of API messaging
 */
sealed class Msg {
    companion object {
        fun replySmsArrived(data: SmsMessage) = Msg.Api.Reply.SmsArrived(data)
    }

    fun isReply() = this is Api.Reply
    data class Child(val sms: CMsg) : Msg()

    sealed class Api : Msg() {
        companion object {
            fun mSms(destinationAddress: String, text: String) = MSms(destinationAddress, text)
            fun sms(data: MSms) = Request.SmsSend(data)
            fun sms(destinationAddress: String, text: String) = sms(mSms(destinationAddress, text))
        }

        sealed class Request : Api() {
            data class SmsSend(val data: MSms) : Request()
        }

        sealed class Reply : Api() {
            data class SmsArrived(val data: SmsMessage) : Reply()
        }
    }
}


/**
 * For Remoting But not used as such for now.
 */
enum class API {
    SendSms,
    SmsArrived
}


fun Msg.Api.toMessage(): Message {
    return when (this) {
        is Msg.Api.Request.SmsSend -> Message.obtain(null, API.SendSms.ordinal, data)
        is Msg.Api.Reply.SmsArrived -> Message.obtain(null, API.SmsArrived.ordinal, data)
    }
}


fun Message.toApi(): Msg.Api {
    return when (this.what) {
        API.SmsArrived.ordinal -> Msg.Api.Reply.SmsArrived(this.obj as SmsMessage)
        API.SendSms.ordinal -> Msg.Api.Request.SmsSend(this.obj as MSms)
        else -> {
            throw RuntimeException("${this} has no 'what' value set")
        }
    }
}
///////////////////////////////////


data class Model(
        val child: CModel = CModel()
)

class SmsElm(me: Service) : ElmMessengerService<Model, Msg, Msg.Api>(me,
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

    private val child = object : SmsChild(me) {
        override fun handleMSG(cur: saffih.elmdroid.sms.child.Msg) {
            dispatch(Msg.Child(cur))
        }

        override fun onSmsArrived(sms: List<SmsMessage>) {
            sms.forEach { dispatchReply(Msg.replySmsArrived(it)) }
        }
    }

    override fun init(): Model {
        val m = child.init()
        return Model().copy(child = m)
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Child -> {
                val m = child.update(msg.sms, model.child)
                return model.copy(child = m)

            }
            is Msg.Api.Request.SmsSend -> {
                child.sendSms(msg.data)
                // currently we do not track state.
                model
            }
            is Msg.Api.Reply -> {
                // reply processed
                // currently we do not track the state
                model
            }
        }
    }

}

