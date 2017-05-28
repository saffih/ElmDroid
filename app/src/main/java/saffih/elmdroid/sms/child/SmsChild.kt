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

