package elmdroid.elmdroid.example1

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.ToggleButton
import elmdroid.elmdroid.ElmBase
import elmdroid.elmdroid.Que

import elmdroid.elmdroid.R

// POJO
class Greating(val greet:String)
val intialGreating = Greating("Hello")

// MODEL
data class Model (val activity : MActivity= MActivity())
data class MActivity (val greeting: Greating= intialGreating)

// MSG
sealed class Msg {
    object Init: Msg()
    sealed class Activity : Msg(){
        class Greated(val v:Greating): Activity()
        class GreatedToggle : Activity()
    }
}

class ElmApp(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me) {
    override fun init(savedInstanceState: Bundle?) = ret(Model(), Msg.Init)

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> ret(model)
            is Msg.Activity -> {
                // several updates
                val (sm, sc) = update(msg, model.activity)

                ret(model.copy(activity= sm), sc)
            }
        }
    }

    fun update(msg: Msg.Activity, model: MActivity): Pair<MActivity, Que<Msg>> {
        return when (msg) {
            is Msg.Activity.Greated -> {
                ret(model.copy(greeting = msg.v))
            }
            is Msg.Activity.GreatedToggle -> {
                val v = if (model.greeting==intialGreating) Greating("world") else intialGreating
                ret(model, Msg.Activity.Greated(v))
            }
        }
    }

    /**
     * Render view, app delegates model review changes to children
     */
    override fun view(model: Model, pre: Model?) {
        val setup = { }
        checkView(setup, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    /**
     * Activity child setup the layout view. if has changes delegate render to child views
     */
    private fun  view(model: MActivity, pre: MActivity?) {
        val setup = { me.setContentView(R.layout.activity_example) }

        checkView(setup, model, pre) {
            view(model.greeting, pre?.greeting)
        }
    }

    private fun  view(model: Greating, pre: Greating?) {
        val setup = {
            val v = me.findViewById(R.id.greetingToggleButton) as ToggleButton
            v.setOnClickListener { v -> dispatch(Msg.Activity.GreatedToggle()) }
        }
        checkView(setup, model, pre) {
            val view=me.findViewById(R.id.greetingText) as TextView
            view.text = model.greet
        }
    }
}

class ExampleActivity : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.start(savedInstanceState)
    }


//    override fun onSaveInstanceState( outState:Bundle? )
//    {
//        // call superclass to save any view hierarchy
//        super.onSaveInstanceState(out)
//    }
}
