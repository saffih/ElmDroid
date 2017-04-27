package elmdroid.elmdroid

/**
 *
 * Copyright Joseph Hartal (Saffi)  23/04/17.
 */

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.ViewGroup

/********************************/
// que Que

data class Que<T>(val lst:List<T>) : Iterable<T>{
    override fun iterator(): Iterator<T> = lst.iterator()
    fun join(que: Que<T>?) = if (que == null) this else Que<T>(lst + que.lst)
    fun join(msg: T?) = if (msg == null) this else Que<T>(lst + msg)

    fun split() = Pair(
            // Msg part
            if (lst.isEmpty()) null else lst.first(),
            // Que msg part. "batch" list
            if (lst.isEmpty()) this else Que(lst.drop(1)))

    operator fun plus(que: Que<T>?) = this.join(que)
    operator fun plus(msg: T) = this.join(msg)
}
fun <T>T.que(): Que<T> = Que(lst=listOf(this))


//Wmodel / wraps proxy the model,  change the propery as usual. it would also hold
//fun <M,  MSG>Pair<M, Que<MSG>>.join(other:Pair<M, Que<MSG>>) = Pair<M, Que<MSG>>()

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
typealias MC<M, MSG> = Pair<M, Que<MSG>>

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
    val noneQue = Que(listOf<MSG>())
//    val subNone = Sub(listOf<MSG>())

    // retModelQue convert the Msg to Que tag
    fun retModelQue(m:M, que: Que<MSG>) = MC(m, que)
    fun retModelQue(m:M, msg:MSG) = MC(m, msg.que() )
    fun retModelQue(m:M) = MC<M,MSG>(m, noneQue)

    // return model parts - reduced.
    fun <T>ret(m:T, useQueNonePlusMsgs: Que<MSG>) = MC<T,MSG>(m, useQueNonePlusMsgs)
    fun <T>ret(m:T, msg:MSG) = MC<T,MSG>(m, msg.que())
    fun <T>ret(m:T) = MC<T,MSG>(m, noneQue)



    // Mandatory methods
    // Elm Init - init : (Model, Que Msg)
    abstract fun init( ) : MC<M,MSG>

    // In Elm - update : Msg -> Model -> (Model, Que Msg)
    abstract fun update(msg: MSG, model: M) : MC<M,MSG> //= retModelQue(model)

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
    var mc: MC<M, MSG>?=null
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
        val (model, cmdQue) = mc
        val (updateModel, newQue) = updateWrap(msg, model)
        callView(updateModel)
        return MC<M, MSG>(updateModel, cmdQue + newQue)
    }

    fun consumeFromQue(mc: MC<M, MSG>): MC<M, MSG> {
        val (model,cmdQue)=mc
        val (msg, restQue) = cmdQue.split()
        val mc2 = retModelQue(model, restQue)
        val res = if (msg==null)  mc2  else cycleMsg(mc2, msg)
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
                val que = mc2.second
                if (que.lst.isEmpty()){
                    return@block false
                }
                mc2= consumeFromQue(mc2)
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
