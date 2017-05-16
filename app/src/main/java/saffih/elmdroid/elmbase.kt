package saffih.elmdroid

/**
 *
 * Copyright Joseph Hartal (Saffi)  23/04/17.
 */


import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.support.v4.content.ContextCompat

/********************************/
// que Que

data class Que<T>(val lst: List<T>) : Iterable<T> {
    constructor() : this(listOf<T>())

    override fun iterator(): Iterator<T> = lst.iterator()
    fun join(another: List<T>) = Que<T>(lst + another)
    fun join(msg: T?) = if (msg == null) this else Que<T>(lst + msg)
    fun join(que: Que<T>?) = if (que == null) this else join(que.lst)

    fun split() = Pair(
            // Msg part
            if (lst.isEmpty()) null else lst.first(),
            // Que smsg part. "batch" list
            if (lst.isEmpty()) this else Que(lst.drop(1)))

    operator fun plus(another: List<T>) = this.join(another)
    operator fun plus(que: Que<T>?) = this.join(que)
    operator fun plus(msg: T) = this.join(msg)
}

fun <T> T.que(): Que<T> = Que(lst = listOf(this))


/**
 * ElmBase - Extending the POC.
 * Requires Msg Modle and implementation of the 3 methods:
 * init update and view
 * having Activity started and providing it.
 * M generic type should be immutable all the way.
 * MSG should be nested sealed classes with data class leafs (may use but not recommended using object instance)
 */

//class QueRef<T>(var ref: Que<T>)


class MsgQue<MSG> {
    // empty typed lists (immutable).
    val noneQue = Que(listOf<MSG>())
    //    var q = QueRef(noneQue)
    var q = noneQue

//    fun dispatch(msg: MSG? = null) {
//        dispatch(msg?.que() ?: noneQue)
//    }
//
//    fun dispatch(lst: List<MSG>) {
//        dispatch(Que(lst))
//    }

//    abstract fun dispatch()

//    fun dispatch(que: Que<MSG>) {
//        addPending(que)
//        dispatch()
//    }

    fun flushed(): Que<MSG> {
        val res = q
        q = noneQue
        return res
    }

    fun addPending(que: Que<MSG>) {
        q += que
    }

    fun addPending(msg: MSG) {
        q += msg
    }

}


interface StatePattern<M, MSG> {

    // Update helper - for Iterable a. delegate updates b. chain the commands in que
    fun <M, MSG, SMSG : MSG> update(msg: SMSG,
                                    iterable: Iterable<M>,
                                    updateElement: (SMSG, M) -> Pair<M, Que<MSG>>

    ): Pair<List<M>, Que<MSG>> {
        val (mIt, qIt) = iterable.map({ updateElement(msg, it) }).unzip()
        val que: Que<MSG> = qIt.reduce({ acc, q -> acc.join(q.lst) })
        return mIt to que
    }

    // return myModel parts - reduced.
    fun <T> ret(m: T, que: Que<MSG>) = m to que

    fun <T> ret(m: T, msgs: List<MSG>) = m to Que<MSG>().join(msgs)
    fun <T> ret(m: T, msg: MSG) = m to msg.que()
    // Empty list of commands
    fun <T> ret(m: T) = m to Que<MSG>()

    fun dispatch()
    fun addPending(msg: MSG) = addPending(Que<MSG>() + msg)
    fun addPending(pending: Que<MSG>)
    fun takePending(): Que<MSG>

    fun dispatch(msg: MSG? = null) {
        dispatch(msg?.que() ?: Que<MSG>())
    }

    fun dispatch(lst: List<MSG>) {
        dispatch(Que(lst))
    }

    fun dispatch(que: Que<MSG>) {
        addPending(que)
        dispatch()
    }

    // Mandatory methods
    // Elm Init - init : (Model, Que Msg)
    fun init(): Pair<M, Que<MSG>>

    // In Elm - update : Msg -> Model -> (Model, Que Msg)
    fun update(msg: MSG, model: M): Pair<M, Que<MSG>>


    fun onCreate()
    fun onDestroy()
}


interface Viewable<M> {
    fun view(model: M, pre: M?)
    fun <TM> checkView(setup: () -> Unit, model: TM, pre: TM?, render: () -> Unit) {
        if (model === pre) return
        if (pre === null) {
            setup()
        }
        render()
    }
}


interface ElmPattern<M, MSG> : StatePattern<M, MSG>, Viewable<M>

//inline fun <reified T : Activity> Activity.startActivity() {
//    startActivity(Intent(this, T::class.java))
//}

//inline fun <reified T :
//
//        fun <PM, PMSG,M, MSG> ElmPattern<PM, PMSG>.bind(Child:<reified T:ElmPattern<M, MSG>>)
// <reified T>

abstract class StateChild<M, MSG> : StatePattern<M, MSG> {
    val q = MsgQue<MSG>()
    override fun takePending() = q.flushed()

    internal var dispatcher: (() -> Unit)? = null
    override fun dispatch() {
        dispatcher!!()
    }

    override fun addPending(pending: Que<MSG>) = q.addPending(pending)


    override fun onCreate() {
        assert(dispatcher != null)
        assert(takePending().lst.isEmpty())
    }

    override fun onDestroy() {
        assert(takePending().lst.isEmpty())
    }

}


/**
 * Glue with parent delegate all to impl by calling bind on impl providing  message
 */
open class StateBound<PM, PMSG, M, MSG, CHILD : StateChild<M, MSG>>(open val impl: CHILD,
                                                                    private val parent: StatePattern<PM, PMSG>,
                                                                    private val toMsg: (MSG) -> PMSG) {
    fun init(): Pair<M, Que<MSG>> = impl.init()

    fun addPending() {
        parent.addPending(pending())
    }

    fun dispatch() {
        addPending()
        parent.dispatch()
    }

    fun pending() = Que(impl.takePending().map { toMsg(it) })

    fun update(msg: MSG, model: M): Pair<M, Que<PMSG>> {
        val (m, c) = impl.update(msg, model)
        impl.addPending(c)
        return m to pending()
    }

    fun onCreate() = impl.onCreate()
    fun onDestroy() = impl.onDestroy()
}


/**
 * bind method - providing impl parent msd conversion,
 * and dispatch delegaton to the parent
 */
fun <PM, PMSG, M, MSG, CHILD : StateChild<M, MSG>> StatePattern<PM, PMSG>.bind(
        child: CHILD,
        toMsg: (MSG) -> PMSG): StateBound<PM, PMSG, M, MSG, CHILD> {
    val res = StateBound(
            child,
            parent = this,
            toMsg = toMsg)
    child.dispatcher = { res.dispatch() }
    return res
}

//
///**
// * Glue with parent delegate all to impl by calling bind on impl providing  message
// */
//class ElmBound<PM, PMSG, M, MSG, CHILD : ElmChild<M, MSG>>(override val impl: CHILD,
//                                                           private val parent: ElmPattern<PM, PMSG>,
//                                                           private val toMsg: (MSG) -> PMSG) :
//        StateBound<PM, PMSG, M, MSG, CHILD>(impl, parent, toMsg) {
//    //    fun init(): Pair<M, Que<MSG>> = impl.init()
//    fun view(model: M, pre: M?) = impl.view(model, pre)
//}


//fun <PM, PMSG, M, MSG, CHILD : ElmChild<M, MSG>> ElmPattern<PM, PMSG>.bind(
//        child: CHILD,
//        toMsg: (MSG) -> PMSG): ElmBound<PM, PMSG, M, MSG, CHILD> {
//    val res = ElmBound(
//            child,
//            parent = this,
//            toMsg = toMsg)
//    child.dispatcher = { res.dispatch() }
//    return res
//}


abstract class ElmChild<M, MSG> : StateChild<M, MSG>(), Viewable<M>

class ElmChildAdapter<M, MSG>(val delegate: ElmPattern<M, MSG>) : ElmChild<M, MSG>() {
    override fun onCreate() = delegate.onCreate()
    override fun onDestroy() = delegate.onDestroy()

    override fun init() = delegate.init()
    override fun update(msg: MSG, model: M) = delegate.update(msg, model)
    override fun view(model: M, pre: M?) = delegate.view(model, pre)
}


abstract class StateEngine<M, MSG> : StatePattern<M, MSG> {
    val noneQue = Que(listOf<MSG>())

    open val q = MsgQue<MSG>()
    override fun addPending(pending: Que<MSG>) = q.addPending(pending)
    override fun takePending() = q.flushed()

    // implementation vars - the latest state reference.
    var mc: Pair<M, Que<MSG>>? = null
    fun notStarted() = mc === null
    override fun onCreate() {

        if (notStarted())
            start()
    }

    override fun onDestroy() {
        mc = null
    }

    // expose our immutable myModel
    val myModel: M get () {
        val model = mc!!.first
        return model
    }

    // delegate to user update.
    private fun updateWrap(msg: MSG, model: M): Pair<M, Que<MSG>> {
        val res = update(msg, model)
        print("Msg: $msg \n Model: $model \n ===> $res")
        return res
    }

    // act with smsg
    private fun cycleMsg(mc: Pair<M, Que<MSG>>, msg: MSG): Pair<M, Que<MSG>> {
        val (model, cmdQue) = mc
        val (updateModel, newQue) = updateWrap(msg, model)

        return Pair<M, Que<MSG>>(updateModel, cmdQue + newQue)
    }

    private fun consumeFromQue(mc: Pair<M, Que<MSG>>): Pair<M, Que<MSG>> {
        val (model, cmdQue) = mc
        val (msg, restQue) = cmdQue.split()
        val mc2 = Pair(model, restQue)
        val res = if (msg == null) mc2 else cycleMsg(mc2, msg)
        return res
    }

    // no locks - done in single view thread
    // it should be "locked" single inner loop and dispatch at a time.


    override fun dispatch() {
        val que = takePending()
        // todo - fail early. add code for checking the thread identity
        val newMC = mainCompute(que, mc!!)
        mc = newMC
    }

    // sanity cnt - assert no concurrent modification.
    private var cnt = 0

    private fun mainCompute(que: Que<MSG>, mc: Pair<M, Que<MSG>>): Pair<M, Que<MSG>> {
        if (cnt != 0) throw RuntimeException("concurrent innerloop! dispatch was called instead of postDispatch")
        cnt += 1
        val (model, que0) = mc
        var mc2 = ret(model, que0 + que)
        // consume commands
        val act = block@ {
            for (i in 0..1000) {
                val que2 = mc2.second //+this.flushed()
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

    /**
     * If overriden - must be called to super
     */
    fun start(): StateEngine<M, MSG> {
        assert(mc == null) { "Check if started more then once." }
        mc = init()
        dispatch()
        return this
    }
}

abstract class ElmEngine<M, MSG> : StateEngine<M, MSG>(), Viewable<M> {
    override fun onCreate() {
        super.onCreate()
        val model = myModel
        callView(model)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private var model_viewed: M? = null

    //In Elm - view : Model -> Html Msg
    open fun view(model: M) {
        view(model, model_viewed)
    }

    private fun callView(model: M) {
        view(model)
        model_viewed = model
    }

    override fun dispatch() {
        super.dispatch()
        callView(myModel)
    }
}
//
///**
// * For Activities having main Handler and dispatch.
// */
//abstract class StateBase<M, MSG>(open val me: Context?) : StateEngine<M, MSG>() {
//    // Get a handler that can be used to post to the main thread
//    // it is lazy since it is created after the view exist.
//    private val mainHandler by lazy { Handler(me?.mainLooper) }
//
//    // cross thread communication
//    fun postDispatch(msg: MSG) {
//        mainHandler.post({ dispatch(msg) })
//    }
//}

/**
 * For Activities having main Handler and dispatch.
 */
abstract class ElmBase<M, MSG>(open val me: Context?) : ElmEngine<M, MSG>() {

    // Get a handler that can be used to post to the main thread
    // it is lazy since it is created after the view exist.
    private val mainHandler by lazy { Handler(me?.mainLooper) }

    // cross thread communication
    fun postDispatch(msg: MSG) {
        mainHandler.post({ dispatch(msg) })
    }


}


fun activityCheckForPermission(me: Activity, perm: String, code: Int,
                               showExplanation: () -> Unit = {}): Boolean {

    val pm = me.packageManager
    val hasPerm = pm.checkPermission(perm, me.packageName)
    if (hasPerm == PackageManager.PERMISSION_GRANTED) {
        return true
    }
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
    val res = (recheck == PackageManager.PERMISSION_GRANTED)
    return res

}
