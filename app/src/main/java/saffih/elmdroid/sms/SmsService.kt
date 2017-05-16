package saffih.elmdroid.sms

// unused example

import android.app.Service
import android.os.Message
import android.telephony.SmsMessage
import saffih.elmdroid.Que
import saffih.elmdroid.bind
import saffih.elmdroid.service.ElmMessengerService
import saffih.elmdroid.sms.child.ElmSmsChild
import saffih.elmdroid.sms.child.MSms
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

    private val child = bind(object : ElmSmsChild(me) {
        override fun onSmsArrived(sms: List<SmsMessage>) {
            sms.forEach { dispatchReply(Msg.replySmsArrived(it)) }
        }
    }) { Msg.Child(it) }

    override fun init(): Pair<Model, Que<Msg>> {
        val (m, c) = child.init()
        return ret(Model().copy(child = m), c.map { Msg.Child(it) })
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Child -> {
                val (m, c) = child.update(msg.sms, model.child)
                return ret(model.copy(child = m), c)

            }
            is Msg.Api.Request.SmsSend -> {
                child.impl.sendSms(msg.data)
                // currently we do not track state.
                ret(model)
            }
            is Msg.Api.Reply -> {
                // reply processed
                // currently we do not track the state
                ret(model)
            }
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model, pre) {
            child.impl.view(model.child, pre?.child)
        }
    }
}

