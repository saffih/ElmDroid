package elmdroid.elmdroid.example1

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.widget.ToggleButton
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example1.hello.Turtle
import saffih.elmdroid.ElmBase
import saffih.elmdroid.Que
import elmdroid.elmdroid.example1.hello.Model as HelloModel
import elmdroid.elmdroid.example1.hello.Msg as HelloMsg


// POJO
class Greating(val greet:String)
val intialGreating = Greating("Hello")

// MODEL
data class Model (val activity : MActivity= MActivity())

data class MActivity(val greeting: Greating = intialGreating,
                     val turtle: HelloModel = HelloModel()
)

// MSG
sealed class Msg {
    object Init: Msg()
    sealed class Activity : Msg(){
        class Greated(val v:Greating): Activity()
        class GreatedToggle : Activity()
        // poc with sub modules.
        class Turtle(val smsg: HelloMsg) : Activity()
    }
}

// convert to API func
fun HelloMsg.toApi() = Msg.Activity.Turtle(this)

class ElmApp(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me) {

    val turtle = Turtle(me, {
        dispatch(
                it.lst.map {
                    Msg.Activity.Turtle(it)
                })
    })
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

                ret(model.copy(activity= sm), sc)
            }
        }
    }

    private fun update(msg: Msg.Init, model: MActivity): Pair<MActivity, Que<Msg>> {
        val (m, c) = turtle.update(HelloMsg.Init(), model.turtle)
        return ret(model.copy(turtle = m), c.map { it.toApi() })
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
            is Msg.Activity.Turtle -> {
                val (m, c) = turtle.update(msg.smsg, model.turtle)
                // todo react
                ret(model.copy(turtle = m), c.map { it.toApi() })
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
     * Activity child setup the layout view. if has changes delegate render to child views
     */
    private fun  view(model: MActivity, pre: MActivity?) {
        val setup = { me.setContentView(R.layout.activity_helloworld) }

        checkView(setup, model, pre) {
            view(model.greeting, pre?.greeting)
            turtle.view(model.turtle, pre?.turtle)
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

class ExampleHelloWorldActivity : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.start()
    }

}
