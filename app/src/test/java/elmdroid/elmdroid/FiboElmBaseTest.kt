

package elmdroid.elmdroid

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class FiboElmBaseTest {

    @Test
    @Throws(Exception::class)
    fun testFibo() {
        val app = App()
        app.start()
        assertEquals(app.last(), 1)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 1)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 2)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 3)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 5)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 8)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 13)

        app.dispatch(msg = Msg.Reset())
        app.dispatch(msg = Msg.Next(4))
        assertEquals(5, app.last())
        app.dispatch(msg = Msg.Reset())
        assertEquals(app.last(), 1)

    }

    data class A(val v: Int = -100)
    data class B(val v: Int = -100)
    data class Model(val a: A = A(), val b: B = B())
    sealed class Msg {
        class Reset : Msg()
        class Next(val steps: Int = 1) : Msg()
        class Update(val model: Model) : Msg()
    }


    class App : ElmBase<Model, Msg>(me = null) {
        override fun init(savedInstanceState: Bundle?) =
                //                ret(Model(A(0),B(1)))
                ret(Model(), Msg.Reset())

        var res: Int = -1

        fun last() = res

        override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
            return when (msg) {
                is Msg.Next -> {
                    if (msg.steps <= 0) {
                        ret(model)
                    }
                    else {
                        ret(model, noneQue + Msg.Update(model)+Msg.Next(msg.steps-1))
                    }
                }
                is Msg.Update -> {
                    val (modelA, couldBeCommandsFromA) = update(msg, model.a)
                    val (modelB, couldBeCommandsFromB) = update(msg, model.b)
                    ret(model.copy(a=modelA, b=modelB),
                            // just showing that we can concatenate lots of msgs after noneQue
                            noneQue + couldBeCommandsFromA + couldBeCommandsFromB)
                }
                is Msg.Reset -> ret(Model(a = A(0), b = B(1)))
            }
        }

        private fun update(msg: Msg.Update, model: A)  = ret(model.copy(v = msg.model.b.v))

        private fun update(msg: Msg.Update, model: B) = ret(model.copy(v = msg.model.a.v + model.v))


        override fun view(model: Model, pre: Model?) {
            val setup = { res = 0 }
            checkView(setup, model, pre) {
                res = model.b.v
            }
        }
    }
}
