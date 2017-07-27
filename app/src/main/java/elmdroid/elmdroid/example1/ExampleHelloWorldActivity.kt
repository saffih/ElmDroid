package elmdroid.elmdroid.example1

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsMessage
import android.widget.TextView
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example1.hello.Turtle
import kotlinx.android.synthetic.main.activity_helloworld.*
import saffih.elmdroid.ElmBase
import saffih.elmdroid.activityCheckForPermission
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


class ElmApp(override val me: ExampleHelloWorldActivity) : ElmBase<Model, Msg>(me) {
    private val sms = object : SmsChild(me) {
        override fun handleMSG(cur: saffih.elmdroid.sms.child.Msg) {
            dispatch(Msg.Activity.Sms(cur))
        }

        override fun onSmsArrived(sms: List<SmsMessage>) {
            sms.forEach { onSmsArrived(it) }
        }

        fun onSmsArrived(sms: SmsMessage) =
                post { dispatch(Msg.Activity.Greeted(Greeting(sms.messageBody))) }
    }

    val turtle = object:Turtle(me) {
        override fun handleMSG(cur: elmdroid.elmdroid.example1.hello.Msg) {
            dispatch(Msg.Activity.Turtle(cur))
        }
    }

    override fun init():Model {
        dispatch(Msg.Init)
        return Model()
    }

        override fun update(msg: Msg, model: Model): Model{
        return when (msg) {
            is Msg.Init -> {
                val m = update(msg, model.activity)
                model.copy(activity = m)
            }
            is Msg.Activity -> {
                // several updates
                val sm = update(msg, model.activity)

                model.copy(activity = sm)
            }
        }
    }

    private fun update(msg: Msg.Init, model: MActivity): MActivity{
        val m = turtle.update(HelloMsg.Init(), model.turtle)
        return model.copy(turtle = m)
    }

    fun update(msg: Msg.Activity, model: MActivity): MActivity{
        return when (msg) {
            is Msg.Activity.Greeted -> {
                model.copy(greeting = msg.v)
            }
            is Msg.Activity.GreetedToggle -> {
                val v = if (model.greeting == intialGreating) Greeting("world") else intialGreating
                dispatch(Msg.Activity.Greeted(v))
                model
            }
            is Msg.Activity.Turtle -> {
                val m = turtle.update(msg.smsg, model.turtle)
                model.copy(turtle = m)
            }
            is Msg.Activity.Sms -> {
                val m = sms.update(msg.smsg, model.sms)
                model.copy(sms = m)
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
            turtle.view(model.turtle, pre?.turtle)
        }
    }

    private fun view(model: Greeting, pre: Greeting?) {
        val setup = {
            val v = me.greetingToggleButton
            v.setOnClickListener { v -> dispatch(Msg.Activity.GreetedToggle()) }
        }
        checkView(setup, model, pre) {
            val view = me.greetingText as TextView
            me.greetingText.text.clear();
            me.greetingText.text.append(model.greet)
        }
    }

    fun onPause() {
        sms.onDestroy()
    }


    fun onResume() {
        sms.onCreate()
    }
}

class ExampleHelloWorldActivity : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCheckForPermission(listOf(Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS), 1)

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



