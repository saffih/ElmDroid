package saffih.elmdroid.sms.child



import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Toast
import saffih.elmdroid.ElmChild
import saffih.elmdroid.Que

//import saffih.elmdroid.sms.toast


sealed class Msg {
    companion object {
        fun received(received: List<SmsMessage>) = Msg.Sms.Received(received)
    }

    //    fun isReply() = this is Api.Reply
    class Init : Msg()
    sealed class Sms : Msg() {
        data class Received(val received: List<SmsMessage>) : Sms()
        class Notified(arrived: List<SmsMessage>) : Sms()
    }
//    sealed class Api : Msg() {
//        companion object {
//            fun mSms(destinationAddress: String, text: String) = MSms(destinationAddress, text)
//            fun sms(data: MSms) = Request.SmsSend(data)
//            fun sms(destinationAddress: String, text: String) = sms(mSms(destinationAddress, text))
//        }
//
//        sealed class Request : Api() {
//            data class SmsSend(val data:MSms) : Request()
//        }
//
//        sealed class Reply : Api() {
//            data class SmsArrived(val data:SmsMessage) : Reply()
//        }
//    }
}



/**
 * For Remoting But not used as such for now.
 */
/**
enum class API {
    SendSms,
    SmsArrived
}


fun Msg.Api.toMessage(): Message {
    return when (this) {
        is Msg.Api.Request.SmsSend -> Message.obtain(null, API.SendSms.ordinal, data)
        is Msg.Api.Reply.SmsArrived-> Message.obtain(null, API.SmsArrived.ordinal, data)
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
 */
///////////////////////////////////


data class Model(
        val state: MState = MState()
)

data class MState(val arrived: List<SmsMessage> = listOf())
data class MSms(val address: String, val text: String)

abstract class ElmSmsChild(val me: Context) : ElmChild<Model, Msg>() {
    abstract fun onSmsArrived(sms: List<SmsMessage>)

    val smsReceiver = SMSReceiverAdapter(
            hook = { arr: Array<out SmsMessage?> -> dispatch(Msg.received(arr.filterNotNull())) })

    //    for services
    override fun onCreate() {
        super.onCreate()
        register()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }

    private fun register() {
        smsReceiver.meRegister(me)
    }

    private fun unregister() {
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
            is Msg.Sms-> {
                val (m, c) = update(msg, model.state)
                ret(model.copy(state = m), c)
            }
        }
    }

    private fun update(msg: Msg.Sms, model: MState): Pair<MState, Que<Msg>> {
        return when (msg) {
            is Msg.Sms.Received -> {
                val arrived = model.arrived + msg.received
                ret(model.copy(arrived = arrived))
            }
            is Msg.Sms.Notified -> ret(model.copy(arrived = listOf()))

        }
    }


    val smsManager = SmsManager.getDefault()

    fun sendSms(data: MSms) {
        smsManager.sendTextMessage(
                data.address,
                null,
                data.text, null, null)
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model.state, pre?.state) {
            toast("state changed ${model}")

            view(model.state, pre?.state)
        }
    }

    private fun view(model: MState, pre: MState?) {

        val setup = {}
        checkView(setup, model.arrived, pre?.arrived) {
            if (!model.arrived.isEmpty()) {
                onSmsArrived(model.arrived)
                dispatch(Msg.Sms.Notified(model.arrived))
            }

        }
    }
}

fun ElmSmsChild.toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
    val handler = Handler(Looper.getMainLooper())
    handler.post({ Toast.makeText(me, txt, duration).show() })
}

