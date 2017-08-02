package elmdroid.hello

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 2/08/17.
 */

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import elmdroid.elmdroid.R
import kotlinx.android.synthetic.main.activity_helloworld.*
import saffih.elmdroid.ElmBase


// POJO
class Greeting(val greet: String)

val intialGreating = Greeting("Hello")

// MODEL
data class Model(val activity: MActivity = MActivity())

data class MActivity(val greeting: Greeting = intialGreating)

// MSG
sealed class Msg {
    object Init : Msg()
    sealed class Activity : Msg() {
        class Greeted(val v: Greeting) : Activity()
        class GreetedToggle : Activity()
    }
}


class ElmApp(override val me: HelloWorldActivity) : ElmBase<Model, Msg>(me) {
    override fun init(): Model {
        dispatch(Msg.Init)
        return Model()
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> { // init delegation to components...
                model
            }
            is Msg.Activity -> {
                val m = update(msg, model.activity)
                model.copy(activity = m)
            }
        // other group updates if had.
        }
    }

    fun update(msg: Msg.Activity, model: MActivity): MActivity {
        return when (msg) {
            is Msg.Activity.Greeted -> {
                model.copy(greeting = msg.v)
            }
            is Msg.Activity.GreetedToggle -> {
                val v = if (model.greeting == intialGreating) Greeting("world") else intialGreating
                // simpler code which does not show the use of dispatch from within an update
                // would have been:
                //     model.copy(greeting = v)
                // but instead we do a dispatch that would be hanled later by the when branch above.
                dispatch(Msg.Activity.Greeted(v))
                model
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
        }
    }

    private fun view(model: Greeting, pre: Greeting?) {
        val setup = {
            me.greetingToggleButton.setOnClickListener {
                v ->
                dispatch(Msg.Activity.GreetedToggle())
            }
        }
        checkView(setup, model, pre) {
            me.greetingText.text.clear()
            me.greetingText.text.append(model.greet)
        }
    }
}

class HelloWorldActivity : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.onCreate()
    }

    override fun onDestroy() {
        app.onDestroy()
        super.onDestroy()
    }
}



