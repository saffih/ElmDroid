package elmdroid.elmdroid.example1.hello

import elmdroid.elmdroid.example1.ExampleHelloWorldActivity
import kotlinx.android.synthetic.main.activity_helloworld.*
import saffih.elmdroid.ElmChild

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


abstract class Turtle(val me: ExampleHelloWorldActivity) :
        ElmChild<Model, Msg>() {
    override fun init(): Model{
        dispatch( Msg.Init())
        return Model() 
    }

    private fun update(msg: Msg.Init, model: MSpeed): MSpeed{
        return model

    }

    private fun update(msg: Msg.ChangeSpeed, model: MSpeed): MSpeed{
        return when (msg) {
            is Msg.ChangeSpeed.Increase -> model.copy(speed = model.speed.next())
            is Msg.ChangeSpeed.Decrease -> model.copy(speed = model.speed.prev())
            is Msg.ChangeSpeed.CycleUp -> model.copy(speed = model.speed.cnext())
            is Msg.ChangeSpeed.Night -> model.copy(speed = ESpeed.sleep)
        }
    }

    override fun update(msg: Msg, model: Model)  : Model{
        return when (msg) {
            is Msg.Init -> {
                val m  = update(msg, model.speed)
                val mu = update(msg, model.ui)
                model.copy(speed = m, ui = mu)
            }
            is Msg.ChangeSpeed -> {
                val m  = update(msg, model.speed)
                model.copy(speed = m)
            }
            is Msg.UI -> {
                val m  = update(msg, model.ui)
                model.copy(ui = m)
            }
        }
    }

    private fun update(msg: Msg.UI, model: MUIMode)  : MUIMode{
//        return model)
        return when (msg) {

            is Msg.UI.ToggleClicked -> model.copy(faster = !model.faster)
            is Msg.UI.NextSpeedClicked -> {
                dispatch ( if (model.faster)   Msg.ChangeSpeed.Increase() else Msg.ChangeSpeed.Decrease())
                model
            }

        }
    }

    private fun update(msg: Msg.Init, model: MUIMode)  : MUIMode{
        return model
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
