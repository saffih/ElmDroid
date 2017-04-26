package elmdroid.elmdroid

/**
 *
 * Copyright Joseph Hartal (Saffi)  23/04/17.
 */

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

/********************************/
// Cmd

data class Cmd<T>(val lst:List<T>) : Iterable<T>{
    override fun iterator(): Iterator<T> = lst.iterator()
    fun join(cmd: Cmd<T>?) = if (cmd == null) this else Cmd<T>(lst + cmd.lst)
    fun join(msg: T?) = if (msg == null) this else Cmd<T>(lst + msg)

    fun split() = Pair(
            // Msg part
            if (lst.isEmpty()) null else lst.first(),
            // Cmd msg part. "batch" list
            if (lst.isEmpty()) this else Cmd(lst.drop(1)))

    operator fun plus(cmd: Cmd<T>?) = this.join(cmd)
    operator fun plus(msg: T) = this.join(msg)
}
fun <T>T.Cmd(): Cmd<T> = Cmd(lst=listOf(this))


//Wmodel / wraps proxy the model,  change the propery as usual. it would also hold
//fun <M,  MSG>Pair<M, Cmd<MSG>>.join(other:Pair<M, Cmd<MSG>>) = Pair<M, Cmd<MSG>>()

/********************************/
//// Sub
//data class Sub<T>(val lst:List<T>) : Iterable<T>{
//    override fun iterator(): Iterator<T> = lst.iterator()
//    fun join(msg: T?) = if (msg == null) this else Sub<T>(lst + msg)
//    fun join(Sub: Sub<T>?) = if (Sub == null) this else Sub<T>(lst + Sub.lst)
//
//    fun split() = Pair(
//            if (lst.isEmpty()) null else lst.first(),
//            if (lst.isEmpty()) this else Sub(lst.drop(1)))
//
//    operator fun plus(msg: T) = this.join(msg)
//    operator fun plus(Sub: Sub<T>?) = this.join(Sub)
//}
//fun <T>T.Sub(): Sub<T> = Sub(lst=listOf(this))


/*************************
 * Droid type (polymorphism) adapters
 */
sealed class ContentView
{
    data class ViewNoParams(val v: View) : ContentView()
    data class ViewParams(val v: View, val lp: ViewGroup.LayoutParams) : ContentView()
    data class ResId(val resId: Int) :  ContentView()
    object Done : ContentView()

    fun setContentViewIn(me:Activity) =
            when(this){
                is ViewNoParams -> me.setContentView(this.v)
                is ViewParams -> me.setContentView(this.v, this.lp)
                is ResId -> me.setContentView(this.resId)
                is ContentView.Done -> {}
            }
}


// MC for internal use.
typealias MC<M, MSG> = Pair<M, Cmd<MSG>>

/**
 * ElmBase - Extending the POC.
 * Requires Msg Modle and implementation of the 3 methods:
 * init update and view
 * having Activity started and providing it.
 */
abstract class ElmBase<M, MSG> (open val me: Context?){

    // Get a handler that can be used to post to the main thread
    // it is lazy since it is created after the view exist.
    val mainHandler by lazy { Handler(me?.mainLooper) }

    // empty typed lists.
    // we use that as defaults, can be used to compare as well
    val cmdNone = Cmd(listOf<MSG>())
//    val subNone = Sub(listOf<MSG>())

    // retModelCmd convert the Msg to Cmd tag
    fun retModelCmd(m:M, cmd:Cmd<MSG>) = MC(m, cmd )
    fun retModelCmd(m:M, msg:MSG) = MC(m, msg.Cmd() )
    fun retModelCmd(m:M) = MC<M,MSG>(m, cmdNone)

    // return model parts - reduced.
    fun <T>ret(m:T, useCmdNonePlusMsgs:Cmd<MSG>) = MC<T,MSG>(m, useCmdNonePlusMsgs)
    fun <T>ret(m:T, msg:MSG) = MC<T,MSG>(m, msg.Cmd())
    fun <T>ret(m:T) = MC<T,MSG>(m, cmdNone)



    // Mandatory methods
    // Elm Init - init : (Model, Cmd Msg)
    abstract fun init( ) : MC<M,MSG>

    // In Elm - update : Msg -> Model -> (Model, Cmd Msg)
    abstract fun update(msg: MSG, model: M) : MC<M,MSG> //= retModelCmd(model)

    // In Elm - sub : subscriptions : Model -> Sub Msg
//    open fun subscriptions(model: M) = subNone

    //In Elm - view : Model -> Html Msg
    open fun view(model: M) {
        view(model, model_viewed)
    }

    abstract fun  view(model: M, pre: M?)


    inline fun <TM>checkView(setup:()->Unit, model:TM, pre:TM?, render:()->Unit){
        if (model === pre) return
        if (pre === null) {
            setup()
        }
        render()
    }

    // implementaton
    var mc:MC<M, MSG>?=null
    var model_viewed:M?=null

    val model:M get () {
        val model=mc!!.first
        return model
    }

    fun callView(model:M){
        view(model)
        model_viewed = model
    }

    // delegate to user update.
    fun updateWrap(msg: MSG, model: M): MC<M, MSG> {
        val res =  update(msg, model)
        print("Msg: $msg \n Model: $model \n ===> $res")
        return res
    }

    // act with msg
    fun cycleMsg(mc: MC<M, MSG>, msg: MSG): MC<M, MSG> {
        val (model, cmd) = mc
        val (updateModel, newCmd) = updateWrap(msg, model)
        callView(updateModel)
        return MC<M, MSG>(updateModel, cmd+newCmd)
    }

    fun consumeCmd(mc: MC<M, MSG>): MC<M, MSG> {
        val (model,cmd)=mc
        val (msg, restCmd) = cmd.split()
        val nmc = retModelCmd(model, restCmd)
        val res = if (msg==null)  nmc  else cycleMsg(nmc, msg)
        return res
    }

    // no locks - done in single view thread
    fun dispatch( msg: MSG) {
        innerLoop(msg)
    }

    fun postDispatch ( msg: MSG) {
        mainHandler.post({dispatch(msg)})
    }

    // called by: mainloop, dispatch (form view or externaly via handler.)
    fun innerLoop(cbMsg: MSG?=null){
        val mc0=mc!!

        val mc1 = if (cbMsg!=null) cycleMsg(mc0 , cbMsg) else mc0
        var mc2 = mc1

        val act =  block@{
            for (i in 0..1000){
                val cmd = mc2.second
                if (cmd.lst.isEmpty()){
                    return@block false
                }
                mc2=consumeCmd(mc2)
            }
            return@block true
        }

        val tooLong = act()
        if (tooLong)  throw RuntimeException("too many commands "+mc2.second)

        mc = mc2
    }

    fun mainLoop(): ElmBase<M, MSG> {
        mc=init()

        innerLoop()
        return this
    }
}
