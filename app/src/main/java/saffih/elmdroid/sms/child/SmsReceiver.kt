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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import saffih.elmdroid.post


open class SMSReceiverAdapter(val hook: (Array<out SmsMessage?>) -> Unit,
                              open val priority: Int? = null
) : BroadcastReceiver() {

    fun meRegister(me: Context) {
        val filter = IntentFilter()
        filter.priority = priority ?: filter.priority

        filter.addAction("android.provider.Telephony.SMS_RECEIVED")
        me.registerReceiver(this, filter)
    }

    fun meUnregister(me: Context) {
        me.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        context.post {
            hook(extractSms(intent))
        }
    }


    companion object {

        private fun constructSmsFromPDUs(rawPduData: Array<*>): Array<SmsMessage?> {
            val smsMessages = arrayOfNulls<SmsMessage>(rawPduData.size)
            for (n in rawPduData.indices) {
                smsMessages[n] = SmsMessage.createFromPdu(rawPduData[n] as ByteArray)
            }
            return smsMessages.filterNotNull().toTypedArray()
        }

        fun extractSms(intent: Intent): Array<out SmsMessage?> {
            val smsMessages = if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                Telephony.Sms.Intents.getMessagesFromIntent(intent)
            } else {
                constructSmsFromPDUs(intent.extras?.get("pdus") as Array<*>)
            }
            return smsMessages
        }
    }
}

