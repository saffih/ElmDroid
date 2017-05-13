package saffih.elmdroid.sms.child



import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Toast
import saffih.elmdroid.ElmChild
import saffih.elmdroid.Que



sealed class Msg {
    fun isReply() = this is Api.Reply
    class Init : Msg()
    sealed class Sms : Msg() {
        data class Received(val sms:SmsMessage) :Sms()
    }
    sealed class Step: Msg(){
        class Start : Step()
        class Done : Step()
    }
    sealed class Api : Msg() {
        sealed class Request : Api() {
            data class SmsSend(val data:MSms) : Request()
        }

        sealed class Reply : Api() {
            data class SmsArrived(val data:SmsMessage) : Reply()
        }
    }
}



/**
 * For Remoting
 */
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
///////////////////////////////////


data class Model(
        val state: MState = MState()
)

data class MState(val i:Int=0)
data class MSms(val destinationAddress: String, val text: String)

abstract class ElmSmsChild(val me: Context) : ElmChild<Model, Msg>() {
    abstract fun onSmsArrived(sms: List<SmsMessage>)

    fun sendSms(destinationAddress: String, text: String) {
        val data = MSms(destinationAddress, text)
        sendSms(data)
    }

    fun sendSms(data: MSms) {
        val msg = Msg.Api.Request.SmsSend(data)
        dispatch(msg)
    }

    val smsReceiver = SMSReceiverAdapter(
            hook = { arr: Array<out SmsMessage?> -> onSmsArrived(arr.filterNotNull()) })

    fun onResume(){
        smsReceiver.meRegister(me)
//        val filter = IntentFilter()
//        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
//        me.registerReceiver(smsReceiver, filter);
    }

    fun onPause(){
        smsReceiver.meUnregister(me)
//        me.unregisterReceiver(smsReceiver)
    }


    override fun init(): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }

    /**
     * The parent delegator should use the following pattern
     *     override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
     *     val (m, c) = child.update(msg, model)
     *     // send response
     *     c.lst.forEach { if (it is Msg.Api) dispatchReply(it) }
     *     // process rest
     *     return ret(m, c.lst.filter { it !is Msg.Api })
     *     }

     */
    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                ret(model)
            }
            is Msg.Sms-> {
                return when (msg) {
                    is Msg.Sms.Received-> {
//                        toast("statReceived ${msg}")
                        ret(model, Msg.Api.Reply.SmsArrived(msg.sms))
                    }
                }
            }
            is Msg.Api -> {
                val (m, c) = update(msg, model.state)
                ret(model.copy(state = m), c)
            }
            is Msg.Step -> {
                val (m, c) = update(msg, model.state)
                ret(model.copy(state = m), c)
            }

        }
    }


    private fun update(msg: Msg.Step, model: MState): Pair<MState, Que<Msg>> {
        return when(msg){
            is Msg.Step.Start -> {
                onResume()
                ret(model)
            }
            is Msg.Step.Done -> {
                onPause()
                ret(model)
            }
        }
    }

    val smsManager = SmsManager.getDefault()

    fun update(msg: Msg.Api, model: MState): Pair<MState, Que<Msg>> {
        return when (msg) {
            is Msg.Api.Request.SmsSend -> {
                // send sms
                smsManager.sendTextMessage(
                        msg.data.destinationAddress,
                        null,
                        msg.data.text, null, null)
                // todo add sent / delivery intents for reporting back the status.
                ret(model)
            }
            is Msg.Api.Reply.SmsArrived->
                // Client Ack he got my Reply
                ret(model)
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model.state, pre?.state) {
            toast("state changed ${model}")
        }
    }
}

fun ElmSmsChild.toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
    val handler = Handler(Looper.getMainLooper())
    handler.post({ Toast.makeText(me, txt, duration).show() })
}
