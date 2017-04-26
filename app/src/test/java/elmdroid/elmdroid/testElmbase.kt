package elmdroid.elmdroid


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 26/04/17.
 */

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.FunSpec

//
//class MyTests : FunSpec() {
//    init {
//        test("String.length should return the length of the string") {
//            "sammy".length shouldBe 5
//            "".length shouldBe 0
//        }
//    }
//
//
//}



class CmdTest: FunSpec() {
    val cmdNone = Cmd(listOf<Int>())
    init {
        val p=1.Cmd().split()
        val noneSplit=cmdNone.split()
        test("Cmd split operations ") {
            p shouldEqual  Pair(1, cmdNone)
            noneSplit shouldEqual Pair(null, cmdNone)
        }

        test("Cmd join operations ") {
            1.Cmd().lst.size shouldBe 1
            cmdNone.lst.size shouldBe 0
            (cmdNone + 1.Cmd() + 1.Cmd()).lst.size shouldBe 2
            (1.Cmd() + 1.Cmd()).lst.size shouldBe 2
            (1.Cmd() + 1.Cmd() + cmdNone).lst.size shouldBe 2
            cmdNone.lst.size shouldBe 0
        }

        test("Cmd order") {
            (1.Cmd() + 2 + 3).lst.size shouldBe 3
            (1.Cmd() + 2 + 3).lst shouldBe listOf(1,2,3)
            (3.Cmd() + 2 + 1).lst shouldBe listOf(3,2,1)
        }
    }
}
//
//class TestElmBase: FunSpec() {
//    data class A(val v: Int = -100)
//    data class B(val v: Int = -100)
//    data class Model(val a: A = A(), val b: B = B())
//    sealed class Msg {
//        class Reset() : Msg()
//        class Next(val steps: Int = 1) : Msg()
//        class Update(val model: Model) : Msg()
//    }
//
//
//    class App : ElmBase<Model, Msg>(me = null) {
//        override fun init(): MC<Model, Msg> =
//                //                ret(Model(A(0),B(1)))
//                ret(Model(), Msg.Reset())
//
//        var res: Int = -1
//
//        fun last() = res
//
//        override fun update(msg: Msg, model: Model): MC<Model, Msg> {
//            return when (msg) {
//                is Msg.Next -> {
//                    if (msg.steps <= 0) ret(model)
//                    else {
//                        ret(model, Msg.Update(model))
//                    }
//                }
//                is Msg.Update -> {
//                    val (modelA, noCmdA) = update(msg, model.a)
//                    val (modelB, noCmdB) = update(msg, model.b)
//                    assert(noCmdA == cmdNone)
//                    assert(noCmdB == cmdNone)
//                    ret(model.copy(a = modelA, b = modelB))
//                    // or
//                    // ret(model.copy(a=modelA, b=modelB, cmdNone+noCmdA+noCmdB))
//                }
//                is Msg.Reset -> ret(Model(a = A(0), b = B(1)))
//            }
//        }
//
//        private fun update(msg: Msg.Update, model: A): MC<A, Msg> {
//            return ret(model.copy(v = msg.model.b.v))
//        }
//
//        private fun update(msg: Msg.Update, model: B): MC<B, Msg> {
//            return ret(model.copy(v = msg.model.a.v + model.v))
//        }
//
//
//        override fun view(model: Model, pre: Model?) {
//            val setup = { res = 0 }
//            checkView(setup, model, pre) {
//                res = model.b.v
//            }
//        }
//
//
//    }
//
//    init {
//        val app = App()
//        app.mainLoop()
//        test("Fibonacci ") {
//            app.dispatch(msg = Msg.Reset())
//            app.dispatch(msg = Msg.Next(4))
//            app.last() shouldBe 5
//            app.dispatch(msg = Msg.Reset())
//
//            app.last() shouldBe 1
//            app.dispatch(msg = Msg.Next())
//            app.last() shouldBe 1
//            app.dispatch(msg = Msg.Next())
//            app.last() shouldBe 2
//            app.dispatch(msg = Msg.Next())
//            app.last() shouldBe 3
//            app.dispatch(msg = Msg.Next())
//            app.last() shouldBe 5
//            app.dispatch(msg = Msg.Reset())
//            app.dispatch(msg = Msg.Next(4))
//            app.last() shouldBe 5
//        }
//
//    }
//}
