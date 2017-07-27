package elmdroid.elmdroid.example1.hello

import elmdroid.elmdroid.example1.ExampleHelloWorldActivity
import kotlinx.android.synthetic.main.activity_helloworld.*
import saffih.elmdroid.ElmChild
import saffih.elmdroid.Que

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 9/05/17.
 */

// POJO

enum class ESpeed {
    sleep,
    slow,
    fast,
    rocket;

    fun step(by: Int) = if (ordIn(ordinal + by)) ESpeed.values()[ordinal + by] else this
    fun next() = step(1)
    fun prev() = step(-1)
    fun cnext() = cycle(ordinal + 1)
    fun cprev() = cycle(ordinal - 1)

    companion object {
        fun ordIn(i: Int) = (i >= 0 && i < ESpeed.values().size)
        fun cycle(i: Int) = ESpeed.values()[i % ESpeed.values().size]
    }
}

// MODEL
data class Model(val speed: MSpeed = MSpeed(), val ui: MUIMode = MUIMode())

data class MSpeed(val speed: ESpeed = ESpeed.slow)
data class MUIMode(val faster: Boolean = true)
// MSG
sealed class Msg {
    class Init : Msg()
    sealed class ChangeSpeed : Msg() {
        class Increase : ChangeSpeed()
        class Decrease : ChangeSpeed()
        class CycleUp : ChangeSpeed()
        class Night : ChangeSpeed()
    }

    sealed class UI : Msg() {
        class ToggleClicked : UI()
        class NextSpeedClicked : UI()
    }
}


class Turtle(val me: ExampleHelloWorldActivity) :
        ElmChild<Model, Msg>() {
    override fun init(): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }

    private fun update(msg: Msg.Init, model: MSpeed): Pair<MSpeed, Que<Msg>> {
        return ret(model)

    }

    private fun update(msg: Msg.ChangeSpeed, model: MSpeed): Pair<MSpeed, Que<Msg>> {
        return when (msg) {
            is Msg.ChangeSpeed.Increase -> ret(model.copy(speed = model.speed.next()))
            is Msg.ChangeSpeed.Decrease -> ret(model.copy(speed = model.speed.prev()))
            is Msg.ChangeSpeed.CycleUp -> ret(model.copy(speed = model.speed.cnext()))
            is Msg.ChangeSpeed.Night -> ret(model.copy(speed = ESpeed.sleep))
        }
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                val (m, c) = update(msg, model.speed)
                val (mu, cu) = update(msg, model.ui)
                ret(model.copy(speed = m, ui = mu), c + cu)
            }
            is Msg.ChangeSpeed -> {
                val (m, c) = update(msg, model.speed)
                ret(model.copy(speed = m), c)
            }
            is Msg.UI -> {
                val (m, c) = update(msg, model.ui)
                ret(model.copy(ui = m), c)
            }
        }
    }

    private fun update(msg: Msg.UI, model: MUIMode): Pair<MUIMode, Que<Msg>> {
//        return ret(model)
        return when (msg) {

            is Msg.UI.ToggleClicked -> ret(model.copy(faster = !model.faster))
            is Msg.UI.NextSpeedClicked -> ret(model,
                    if (model.faster) Msg.ChangeSpeed.Increase() else Msg.ChangeSpeed.Decrease()
            )
        }
    }

    private fun update(msg: Msg.Init, model: MUIMode): Pair<MUIMode, Que<Msg>> {
        return ret(model)
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model, pre) {
            view(model.speed, pre?.speed)
            view(model.ui, pre?.ui)
        }
    }

    private fun view(model: MUIMode, pre: MUIMode?) {
        val setup = {}
        checkView(setup, model, pre) {
            val mode = me.turtleFaster
            mode.text = if (model.faster) "faster" else "slower"
            mode.setOnClickListener { dispatch(Msg.UI.ToggleClicked()) }
        }
    }

    private fun view(model: MSpeed, pre: MSpeed?) {
        val setup = {}
        checkView(setup, model, pre) {
            val v = me.turtleSpeed
            v.text = model.speed.name
            v.setOnClickListener { dispatch(Msg.UI.NextSpeedClicked()) }
        }
    }

}
