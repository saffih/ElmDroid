
/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 4/05/17.
 */

package saffih.elmdroid.sms.child

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import saffih.elmdroid.activityCheckForPermission


open class SMSReceiverAdapter(val hook: (Array<out SmsMessage?>) -> Unit)  : BroadcastReceiver() {

    fun meRegister(me: Context ){
        val filter = IntentFilter()
        filter.priority = 18
        filter.addAction("android.provider.Telephony.SMS_RECEIVED")
        me.registerReceiver(this, filter)
    }

    fun meUnregister(me: Context ){
        me.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        hook(extractSms(intent))
    }




    companion object {
        fun checkPermission(me: Activity, code: Int = 1) {
            activityCheckForPermission(me, Manifest.permission.RECEIVE_SMS, code)
            activityCheckForPermission(me, Manifest.permission.READ_SMS, code)
            activityCheckForPermission(me, Manifest.permission.SEND_SMS, code)

        }

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

