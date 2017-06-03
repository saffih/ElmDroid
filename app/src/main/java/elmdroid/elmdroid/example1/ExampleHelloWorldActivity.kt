package elmdroid.elmdroid.example1

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsMessage
import android.widget.TextView
import android.widget.ToggleButton
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example1.hello.Turtle
import saffih.elmdroid.*
import saffih.elmdroid.sms.child.SmsChild
import elmdroid.elmdroid.example1.hello.Model as HelloModel
import elmdroid.elmdroid.example1.hello.Msg as HelloMsg


// POJO
class Greeting(val greet: String)

val intialGreating = Greeting("Hello")

// MODEL
data class Model(val activity: MActivity = MActivity())

data class MActivity(val greeting: Greeting = intialGreating,
                     val turtle: HelloModel = HelloModel(),
                     val sms: MSms = MSms()
)

typealias MSms = saffih.elmdroid.sms.child.Model
typealias SmsMsg = saffih.elmdroid.sms.child.Msg

// MSG
sealed class Msg {
    object Init : Msg()
    sealed class Activity : Msg() {
        class Greeted(val v: Greeting) : Activity()
        class GreetedToggle : Activity()
        // poc with sub modules.
        class Turtle(val smsg: HelloMsg) : Activity()

        class Sms(val smsg: SmsMsg
        ) : Activity()
    }
}


class ElmApp(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me) {
    private val sms = bindState(object : SmsChild(me) {
        override fun onSmsArrived(sms: List<SmsMessage>) {
            sms.forEach { onSmsArrived(it) }
        }

        fun onSmsArrived(sms: SmsMessage) =
                post { dispatch(Msg.Activity.Greeted(Greeting(sms.messageBody))) }
    }) { Msg.Activity.Sms(it) }

    val turtle = bind(Turtle(me)) { Msg.Activity.Turtle(it) }

    override fun init() = ret(Model(), Msg.Init)

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                val (m, c) = update(msg, model.activity)
                ret(model.copy(activity = m), c)
            }
            is Msg.Activity -> {
                // several updates
                val (sm, sc) = update(msg, model.activity)

                ret(model.copy(activity = sm), sc)
            }
        }
    }

    private fun update(msg: Msg.Init, model: MActivity): Pair<MActivity, Que<Msg>> {
        val (m, c) = turtle.update(HelloMsg.Init(), model.turtle)
        return ret(model.copy(turtle = m), c)
    }

    fun update(msg: Msg.Activity, model: MActivity): Pair<MActivity, Que<Msg>> {
        return when (msg) {
            is Msg.Activity.Greeted -> {
                ret(model.copy(greeting = msg.v))
            }
            is Msg.Activity.GreetedToggle -> {
                val v = if (model.greeting == intialGreating) Greeting("world") else intialGreating
                ret(model, Msg.Activity.Greeted(v))
            }
            is Msg.Activity.Turtle -> {
                val (m, c) = turtle.update(msg.smsg, model.turtle)
                ret(model.copy(turtle = m), c)
            }
            is Msg.Activity.Sms -> {
                val (m, c) = sms.update(msg.smsg, model.sms)
                ret(model.copy(sms = m), c)
            }
        }
    }


    /**
     * Render view, app delegates myModel review changes to children
     */
    override fun view(model: Model, pre: Model?) {
        val setup = { }
        checkView(setup, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    /**
     * Activity impl setup the layout view. if has changes delegate render to impl views
     */
    private fun view(model: MActivity, pre: MActivity?) {
        val setup = { me.setContentView(R.layout.activity_helloworld) }

        checkView(setup, model, pre) {
            view(model.greeting, pre?.greeting)
            turtle.impl.view(model.turtle, pre?.turtle)
        }
    }

    private fun view(model: Greeting, pre: Greeting?) {
        val setup = {
            val v = me.findViewById(R.id.greetingToggleButton) as ToggleButton
            v.setOnClickListener { v -> dispatch(Msg.Activity.GreetedToggle()) }
        }
        checkView(setup, model, pre) {
            val view = me.findViewById(R.id.greetingText) as TextView
            view.text = model.greet
        }
    }

    fun onPause() {
        sms.impl.onDestroy()
    }


    fun onResume() {
        sms.impl.onCreate()
    }
}

class ExampleHelloWorldActivity : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCheckForPermission(this, "android.permission.RECEIVE_SMS", 1)
        activityCheckForPermission(this, "android.permission.READ_SMS", 1)
//        activityCheckForPermission(this, "android.permission.SEND_SMS", 1)

        app.onCreate()
    }

    override fun onResume() {
        super.onResume()
        app.onResume()


    }

    override fun onPause() {
        super.onPause()
        app.onPause()

    }

    //
    override fun onDestroy() {
        app.onDestroy()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        app.onCreate()
    }
//
//    override fun onResumeFragments() {
//        super.onResumeFragments()
//    }
//
//
//
//    override fun onStop() {
//        super.onStop()
//    }
}



