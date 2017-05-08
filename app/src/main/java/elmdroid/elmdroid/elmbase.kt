package elmdroid.elmdroid

/**
 *
 * Copyright Joseph Hartal (Saffi)  23/04/17.
 */


import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat

/********************************/
// que Que

data class Que<T>(val lst:List<T>) : Iterable<T>{
    override fun iterator(): Iterator<T> = lst.iterator()
    fun join(another: List<T>) = Que<T>(lst + another)
    fun join(msg: T?) = if (msg == null) this else Que<T>(lst + msg)
    fun join(que: Que<T>?) = if (que == null) this else join(que.lst)

    fun split() = Pair(
            // Msg part
            if (lst.isEmpty()) null else lst.first(),
            // Que msg part. "batch" list
            if (lst.isEmpty()) this else Que(lst.drop(1)))

    operator fun plus(another: List<T>) = this.join(another)
    operator fun plus(que: Que<T>?) = this.join(que)
    operator fun plus(msg: T) = this.join(msg)
}
fun <T>T.que(): Que<T> = Que(lst=listOf(this))



/**
 * ElmBase - Extending the POC.
 * Requires Msg Modle and implementation of the 3 methods:
 * init update and view
 * having Activity started and providing it.
 * M generic type should be immutable all the way.
 * MSG should be nested sealed classes with data class leafs (may use but not recommended using object instance)
 */


abstract class ElmEngine<M, MSG> (open val me: Context?){
    // empty typed lists (immutable).
    val noneQue = Que(listOf<MSG>())
//    val subNone = Sub(listOf<MSG>())

    // return model parts - reduced.
    fun <T> ret(m: T, que: Que<MSG>) = m to que

    fun <T> ret(m: T, msgs: List<MSG>) = m to noneQue.join(msgs)
    fun <T> ret(m: T, msg: MSG) = m to msg.que()
    // Empty list of commands
    fun <T> ret(m: T) = m to noneQue


    // Mandatory methods
    // Elm Init - init : (Model, Que Msg)
    abstract fun init(savedInstanceState: Bundle?): Pair<M, Que<MSG>>

    // In Elm - update : Msg -> Model -> (Model, Que Msg)
    abstract fun update(msg: MSG, model: M) : Pair<M, Que<MSG>>

    // Update helper - for Iterable a. delegate updates b. chain the commands in que
    inline fun <M, SMSG : MSG> update(msg: SMSG,
                                      iterable: Iterable<M>,
                                      updateElement: (SMSG, M) -> Pair<M, Que<MSG>>

    ): Pair<List<M>, Que<MSG>> {
        val (mIt, qIt) = iterable.map({ updateElement(msg, it) }).unzip()
        val que: Que<MSG> = qIt.reduce({ acc, q -> acc.join(q.lst) })
        return mIt to que
    }

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

    // implementation vars - the latest state reference.
    private var mc: Pair<M, Que<MSG>>? = null
    fun notStarted()= mc===null
    private var model_viewed: M? = null

    // expose our immutable model
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

    // no locks - done in single view thread
    // it should be "locked" single inner loop and dispatch at a time.
    fun dispatch(msg: MSG?=null){
        dispatch( msg?.que()?: noneQue )
    }

    fun dispatch(que: Que<MSG>){
        // todo - fail early. add code for checking the thread identity
        val newMC = mainCompute(que, mc!!)
        val model = newMC.first
        mc=newMC
        callView(model)
    }

    // sanity cnt - assert no concurrent modification.
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
        if (tooLong) throw RuntimeException("Do we have a loop ?, too many commands " + mc2.second)

        cnt -= 1

        return mc2
    }

    fun start(savedInstanceState: Bundle? = null): ElmEngine<M, MSG> {
        assert(mc==null) { "Check if started more then once." }
        mc=init(savedInstanceState)
        dispatch()
        return this
    }
}

/**
 * For Activities having main Handler and dispatch.
 */
abstract class ElmBase<M, MSG> (override val me: Context?):ElmEngine<M, MSG>(me){

    // Get a handler that can be used to post to the main thread
    // it is lazy since it is created after the view exist.
    private val mainHandler by lazy { Handler(me?.mainLooper) }

    // cross thread communication
    fun postDispatch(msg: MSG) {
        mainHandler.post({dispatch(msg)})
    }
}


fun activityCheckForPermission(me: Activity, perm: String, code: Int, showExplanation: () -> Unit = {}): Boolean {
    val permissionCheck = ContextCompat.checkSelfPermission(me, perm)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (me.shouldShowRequestPermissionRationale(perm)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showExplanation()
            } else {
                // No explanation needed, we can request the permission.
                me.requestPermissions(listOf(perm).toTypedArray(), code)
            }
        }
    }
    val recheck = ContextCompat.checkSelfPermission(me, perm)
    return recheck == PackageManager.PERMISSION_GRANTED

}