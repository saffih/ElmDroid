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

    fun setContentViewIn(me: Activity) =
            when(this){
                is ViewNoParams -> me.setContentView(this.v)
                is ViewParams -> me.setContentView(this.v, this.lp)
                is ResId -> me.setContentView(this.resId)
                is Done -> {}
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
    private val mainHandler by lazy { Handler(me?.mainLooper) }

    // empty typed lists.
    // we use that as defaults, can be used to compare as well
    val noneQue = Que(listOf<MSG>())
//    val subNone = Sub(listOf<MSG>())

    // return model parts - reduced.
    fun <T>ret(m:T, useQueNonePlusMsgs: Que<MSG>) = Pair<T, Que<MSG>>(m, useQueNonePlusMsgs)
    fun <T>ret(m:T, msg:MSG) = Pair<T, Que<MSG>>(m, msg.que())
    fun <T>ret(m:T) = Pair<T, Que<MSG>>(m, noneQue)



    // Mandatory methods
    // Elm Init - init : (Model, Que Msg)
    abstract fun init( ) : Pair<M, Que<MSG>>

    // In Elm - update : Msg -> Model -> (Model, Que Msg)
    abstract fun update(msg: MSG, model: M) : Pair<M, Que<MSG>>

    // In Elm - sub : subscriptions : Model -> Sub Msg

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
    var mc: Pair<M, Que<MSG>>?=null
    var model_viewed:M?=null

    val model:M get () {
        val model=mc!!.first
        return model
    }

    private fun callView(model:M){
        view(model)
        model_viewed = model
    }

    // delegate to user update.
    private fun updateWrap(msg: MSG, model: M): Pair<M, Que<MSG>> {
        val res =  update(msg, model)
        print("Msg: $msg \n Model: $model \n ===> $res")
        return res
    }

    // act with msg
    private fun cycleMsg(mc: Pair<M, Que<MSG>>, msg: MSG): Pair<M, Que<MSG>> {
        val (model, cmdQue) = mc
        val (updateModel, newQue) = updateWrap(msg, model)

        return Pair<M, Que<MSG>>(updateModel, cmdQue + newQue)
    }

    private fun consumeFromQue(mc: Pair<M, Que<MSG>>): Pair<M, Que<MSG>> {
        val (model,cmdQue) = mc
        val (msg, restQue) = cmdQue.split()
        val mc2 = Pair(model, restQue)
        val res = if (msg==null) mc2 else cycleMsg(mc2, msg)
        return res
    }


    public fun postDispatch ( msg: MSG) {
        mainHandler.post({dispatch(msg)})
    }

    // no locks - done in single view thread
    // it should be "locked" single inner loop and dispatch at a time.
    fun dispatch(msg: MSG?=null){
        dispatch( msg?.que()?: noneQue )
    }

    private fun dispatch(que: Que<MSG>){
        val newMC = mainCompute(que, mc!!)
        val model = newMC.first
        callView(model)
        mc=newMC
    }

    private var cnt=0
    private fun mainCompute(que: Que<MSG>, mc:Pair<M, Que<MSG>>): Pair<M, Que<MSG>> {
        if (cnt != 0) throw RuntimeException("concurrent innerloop! dispatch was called instead of postDispatch")
        cnt += 1
        val (model, que0) = mc
        var mc2 = ret(model, que0 + que)
        // consume commands
        val act = block@ {
            for (i in 0..1000) {
                val que2 = mc2.second
                if (que2.lst.isEmpty()) {
                    return@block false
                }
                mc2 = consumeFromQue(mc2)
            }
            return@block true
        }


        val tooLong = act()
        if (tooLong) throw RuntimeException("too many commands " + mc2.second)

        cnt -= 1

        return mc2
    }

    public fun start(): ElmBase<M, MSG> {
        assert(mc==null) { "Check if started more then once." }
        mc=init()
        dispatch()
        return this
    }
}
