package elmdroid.elmdroid

import org.junit.Assert.assertEquals
import org.junit.Test
import saffih.elmdroid.ElmBase
import saffih.elmdroid.MsgQue

/**
 * Example local unit test, which will execute on the development machine (host).

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class FiboElmBaseTest {

    @Test
    @Throws(Exception::class)
    fun testFibo() {
        val app = App()
        app.onCreate()
        assertEquals(-100, app.last()) //before reset
        app.dispatch(msg = Msg.Next())
        assertEquals(1,app.last())
        app.dispatch(msg = Msg.Next())
        assertEquals(1,app.last())
        app.dispatch(msg = Msg.Next())
        assertEquals(2,app.last())
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 3)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 5)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 8)
        app.dispatch(msg = Msg.Next())
        assertEquals(app.last(), 13)
        // there is one not consumed but we do reset

        app.dispatch(msg = Msg.Reset())
        app.dispatch(msg = Msg.Next(4))
        app.consume() // we wait...
        assertEquals(5, app.last())
        app.dispatch(msg = Msg.Reset())
        app.consume() // we wait...
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
        var l = listOf<Msg>()

        override val que: MsgQue<Msg>
            get() = object : MsgQue<Msg>(null, 0) {
                override fun handleMSG(cur: Msg) {
                    this@App.handleMSG(cur)
                }

                override fun dispatch(msg: Msg) {
                    l = l + msg
                    consume()

                }
            }
        var busy = false
        fun consume() {
            if (busy) return
            busy = true
            while (true) {
                val l2 = l
                l = listOf()
                if (l2.isEmpty()) break
                l2.map {
                    handleMSG(it)
                }
            }
            busy = false
        }

        override fun dispatch(msg: Msg) {
            // consume if first if not just append
            consume()
            l += msg
        }

        override fun hasMessages(): Boolean {
            return l.isNotEmpty()
        }

        override fun init(): Model {
            //                Model(A(0),B(1)))
            dispatch(Msg.Reset())
            return Model()
        }

        var res: Int = -1

        fun last() = res

        override fun update(msg: Msg, model: Model): Model {
            return when (msg) {
                is Msg.Next -> {
                    if (msg.steps <= 0) {
                        model
                    } else {
                        dispatch(Msg.Update(model))
                        dispatch(Msg.Next(msg.steps - 1))
                        model
                    }
                }
                is Msg.Update -> {
                    val modelA = update(msg, model.a)
                    val modelB = update(msg, model.b)
                    model.copy(a = modelA, b = modelB)
                }
                is Msg.Reset -> Model(a = A(0), b = B(1))
            }
        }

        private fun update(msg: Msg.Update, model: A) = model.copy(v = msg.model.b.v)

        private fun update(msg: Msg.Update, model: B) = model.copy(v = msg.model.a.v + model.v)


        override fun view(model: Model, pre: Model?) {
            val setup = { res = 0 }
            checkView(setup, model, pre) {
                res = model.b.v
            }
        }
    }
}
