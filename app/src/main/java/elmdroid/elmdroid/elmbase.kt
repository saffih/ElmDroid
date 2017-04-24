package elmdroid.elmdroid

/**
 * Created by saffi on 23/04/17.
 */

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.ViewGroup

abstract class IMsg {
}

/********************************/
// Cmd

class Cmd<T: IMsg>(val lst:List<T>) : Iterable<T>{
    override fun iterator(): Iterator<T> = lst.iterator()
    fun join(msg: T?) = if (msg == null) this else Cmd<T>(lst + msg)
    fun join(cmd: Cmd<T>?) = if (cmd == null) this else Cmd<T>(lst + cmd.lst)

    fun split() = Pair(
            // Msg part
            if (lst.isEmpty()) null else lst.first(),
            // Cmd msg part. "batch" list
            if (lst.isEmpty()) this else Cmd(lst.drop(1)))

    operator fun plus(msg: T) = this.join(msg)
    operator fun plus(cmd: Cmd<T>?) = this.join(cmd)
}
fun <T: IMsg>T.Cmd(): Cmd<T> = Cmd(lst=listOf(this))

/********************************/
// Sub
class Sub<T: IMsg>(val lst:List<T>) : Iterable<T>{
    override fun iterator(): Iterator<T> = lst.iterator()
    fun join(msg: T?) = if (msg == null) this else Sub<T>(lst + msg)
    fun join(Sub: Sub<T>?) = if (Sub == null) this else Sub<T>(lst + Sub.lst)

    fun split() = Pair(
            if (lst.isEmpty()) null else lst.first(),
            if (lst.isEmpty()) this else Sub(lst.drop(1)))

    operator fun plus(msg: T) = this.join(msg)
    operator fun plus(Sub: Sub<T>?) = this.join(Sub)
}
fun <T: IMsg>T.Sub(): Sub<T> = Sub(lst=listOf(this))


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
 * Requires IMsg Modle and implementation of the 3 methods:
 * init update and view
 * having Activity started and providing it.
 */
abstract class ElmBase<M, MSG: IMsg> (open val me: Context){
    // Get a handler that can be used to post to the main thread
    // it is lazy since it is created after the view exist.
    val mainHandler by lazy { Handler(me.getMainLooper()) }

    // empty typed lists.
    // we use that as defaults, can be used to compare as well
    val cmdNone = Cmd(listOf<MSG>())
    val subNone = Sub(listOf<MSG>())

    // retModelCmd convert the IMsg to Cmd tag
    fun retModelCmd(m:M, cmd:Cmd<MSG>) = MC(m, cmd )
    fun retModelCmd(m:M, msg:MSG) = MC(m, msg.Cmd() )
    fun retModelCmd(m:M) = MC<M,MSG>(m, cmdNone)

    // return model parts - reduced.
    fun <T>ret(m:T, cmd:Cmd<MSG>) = MC<T,MSG>(m, cmd)
    fun <T>ret(m:T, msg:MSG) = MC<T,MSG>(m, msg.Cmd())
    fun <T>ret(m:T) = MC<T,MSG>(m, cmdNone)



    // Mandatory methods
    // Elm Init - init : (Model, Cmd Msg)
    abstract fun init( ) : MC<M,MSG>

    // In Elm - update : Msg -> Model -> (Model, Cmd Msg)
    open fun update(msg: MSG, model: M) = retModelCmd(model)

    // In Elm - sub : subscriptions : Model -> Sub Msg
    open fun subscriptions(model: M) = subNone

    //In Elm - view : Model -> Html Msg
    abstract fun view(model: M)



    // implementaton
    var mc:MC<M, MSG>?=null
    var model_viewed:M?=null

    // delegate to user update.
    fun updateWrap(msg: MSG, model: M): MC<M, MSG> {
        return update(msg, model)
    }

    // act with msg
    fun cycleMsg(mc: MC<M, MSG>, msg: MSG): MC<M, MSG> {
        val (model, cmd) = mc
        val (updateModel, newCmd) = updateWrap(msg, model)
        return MC<M, MSG>(updateModel, cmd+newCmd)
    }

    fun consumeCmd(mc: MC<M, MSG>): MC<M, MSG> {
        val (model,cmd)=mc
        val (msg, restCmd) = cmd.split()
        val nmc = retModelCmd(model, restCmd)
        val res = if (msg==null)  nmc  else cycleMsg(nmc, msg)
        return res
    }

    fun consumeSub(mc: MC<M,MSG>): MC<M,MSG> {
        var curMC = mc
        val (model,cmd)=curMC
        val sub = subscriptions(model)
        if (sub.lst.isEmpty()) return curMC

        for ( msg in sub ){
            curMC = cycleMsg(curMC, msg)
        }
        return curMC
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
        var curMC = mc!!
        if (cbMsg!=null) curMC = cycleMsg(curMC , cbMsg)
        curMC=consumeCmd(curMC)
        curMC=consumeSub(curMC)
        val (model,cmd)=curMC
        view(model)
        mc = curMC
        // note what was done - time travel would have the m
        model_viewed = model
    }

    fun mainLoop() {
        mc=init()

        innerLoop()
    }
}
