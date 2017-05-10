
/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 4/05/17.
 */

package saffih.elmdroid.sms.child

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage


open class SMSReceiverAdapter(val hook: (Array<out SmsMessage?>) -> Unit)  : BroadcastReceiver() {

    fun meRegister(me: Context ){
        val filter = IntentFilter()
//        filter.priority=18
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        me.registerReceiver(this, filter);
    }

    fun meUnregister(me: Context ){
        me.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {


        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                Telephony.Sms.Intents.getMessagesFromIntent(intent)
            } else{
                constructSmsFromPDUs(intent.extras?.get("pdus") as Array<*>)
            }
            hook(smsMessages)
        }
    }

    private fun constructSmsFromPDUs(rawPduData: Array<*>): Array<SmsMessage?> {
        val smsMessages = arrayOfNulls<SmsMessage>(rawPduData.size)
        for (n in rawPduData.indices) {
            smsMessages[n] = SmsMessage.createFromPdu(rawPduData[n] as ByteArray)
        }
        return smsMessages.filterNotNull().toTypedArray()
    }
}

