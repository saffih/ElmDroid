/*
 * By Saffi Hartal, 2017.
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package saffih.elmdroid

/**
 *
 * Copyright Joseph Hartal (Saffi)  23/04/17.
 */


import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast


val mainLooper by lazy { Looper.getMainLooper() }

abstract class MsgQue<MSG>(looper: Looper?, val what: Int) : Handler(looper) {
    constructor(what: Int) : this(mainLooper, what)
    constructor() : this(mainLooper, 0)
    override fun handleMessage(msg: Message?) {
        val cur = msg?.obj as MSG
        handleMSG(cur)
    }

    abstract fun handleMSG(cur: MSG)
    open fun dispatch(msg: MSG) {
        val m = Message.obtain(null, what, msg)
        sendMessage(m)
    }

}


interface StatePattern<M, MSG> {

    val que: MsgQue<MSG>
    // Update helper - for Iterable a. delegate updates b. chain the commands in que
    fun <M, MSG, SMSG : MSG> update(msg: SMSG,
                                    iterable: Iterable<M>,
                                    updateElement: (SMSG, M) -> M

    ): List<M> {
        return iterable.map({ updateElement(msg, it) })
    }


    fun dispatch(msg: MSG) = que.dispatch(msg)
    fun dispatch(pending: List<MSG>) = pending.map { dispatch(it) }


    // Mandatory methods
    // Elm Init - init : (Model, Que Msg)
    fun init(): M

    // In Elm - update : Msg -> Model -> (Model, Que Msg)
    fun update(msg: MSG, model: M): M


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
//        fun <PM, PMSG,M, MSG> ElmPattern<PM, PMSG>.bindState(Child:<reified T:ElmPattern<M, MSG>>)
// <reified T>

abstract class StateChild<M, MSG> : StatePattern<M, MSG> {
    override val que = object : MsgQue<MSG>() {
        override fun handleMSG(cur: MSG) {
            this@StateChild.handleMSG(cur)
        }
    }

    override fun onCreate() {
    }

    override fun onDestroy() {
    }

    abstract fun handleMSG(cur: MSG)
}

abstract class ElmChild<M, MSG> : StateChild<M, MSG>(), Viewable<M>

abstract class ElmChildAdapter<M, MSG>(val delegate: ElmPattern<M, MSG>) : ElmChild<M, MSG>() {
    override fun onCreate() = delegate.onCreate()
    override fun onDestroy() = delegate.onDestroy()

    override fun init() = delegate.init()
    override fun update(msg: MSG, model: M) = delegate.update(msg, model)
    override fun view(model: M, pre: M?) = delegate.view(model, pre)
}



abstract class StateEngine<M, MSG> : StatePattern<M, MSG> {
    private val TAG: String = StateEngine::class.java.name
    var cnt = 0
    open val what: Int = 1
    open val looper: Looper get () = mainLooper
    override val que: MsgQue<MSG> by lazy {
        object : MsgQue<MSG>(this@StateEngine.looper, this@StateEngine.what) {
            override fun handleMSG(cur: MSG) {
                this@StateEngine.handleMSG(cur)
            }
        }
    }

    fun handleMSG(cur: MSG) {
        if (halted) return
        cnt += 1

        val res = update(cur, myModel)
        val s = "Msg: $cur \n Model: $myModel \n ===> $res"
        logd(s)
        mc = res
        if (cnt > 1000) throw RuntimeException("Do we have a loop $cnt, last msg was $cur")
        if (!hasMessages()) {
            cnt = 0
            onQueIsEmpty()
        }
    }

    open protected fun logd(s: String) {
        Log.d(TAG, s)
    }

    open fun hasMessages(): Boolean {
        return que.hasMessages(what)
    }

    open fun onQueIsEmpty() {
    }

    // implementation vars - the latest state reference.
    var mc: M? = null

    fun notStarted() = mc === null
    override fun onCreate() {

        if (notStarted()) {
            start()
            onStarted()
        }
    }

    open fun onStarted() {}

    var halted = false
    override fun onDestroy() {
        mc = null
        halted = true
    }

    // expose our immutable myModel
    val myModel: M get () {
        return mc!!
    }


    /**
     * If overriden - must be called to super
     */
    fun start(): StateEngine<M, MSG> {
        assert(mc == null) { "Check if started more then once." }
        mc = init()
        return this
    }
}

abstract class ElmEngine<M, MSG> : StateEngine<M, MSG>(), Viewable<M> {
    override fun onCreate() {
        // make that done properly ques and dispatches later
        que.post {
            super.onCreate()
            val model = myModel
            callView(model)
        }
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

    override fun onQueIsEmpty() {
        // update the view
        callView(myModel)
    }
}

//
///**
// * For Activities having main Handler and dispatch.
// */
abstract class StateBase<M, MSG>(open val me: Context?) : StateEngine<M, MSG>() {
    // Get a handler that can be used to post to the main thread
    // it is lazy since it is created after the view exist.

    // cross thread communication
    protected fun post(function: () -> Unit) {
        que.post(function)
    }
}


fun Context.post(posted: () -> Unit): Boolean {
    return Handler(mainLooper).post(posted)
}

fun Context.toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
    val h = Handler(mainLooper)
    h.post({ Toast.makeText(this, txt, duration).show() })
}

fun Context.postDelayed(r: () -> Unit, delayMillis: Long) = Handler(this.mainLooper).
        postDelayed(r, delayMillis)


fun Context.removeCallbacks(r: () -> Unit) = Handler(this.mainLooper).
        removeCallbacks(r)


/**
 * For Activities having main Handler and dispatch.
 */
abstract class ElmBase<M, MSG>(open val me: Context?) : ElmEngine<M, MSG>() {

    // Get a handler that can be used to post to the main thread
    // it is lazy since it is created after the view exist.
    val mainHandler by lazy { Handler(mainLooper) }

    //     cross thread communication
    protected fun post(function: () -> Unit) {
        mainHandler.post(function)
    }

}


private class Memoize1<in T, out R>(val f: (T) -> R) : (T) -> R {
    private val values = mutableMapOf<T, R>()
    override fun invoke(x: T): R {
        return values.getOrPut(x, { f(x) })
    }
}

fun <T, R> ((T) -> R).memoize(): (T) -> R = Memoize1(this)

//// todo should use list of all perms
fun Activity.activityCheckForPermission(perm: List<String>, code: Int): Boolean {
    val me = this
    val missing = perm.filter { ContextCompat.checkSelfPermission(me, it) != PackageManager.PERMISSION_GRANTED }
    if (missing.isEmpty()) return true

//    me.requestPermissions(missing.toTypedArray(), code)
    val recheck = missing.map { ContextCompat.checkSelfPermission(me, it) != PackageManager.PERMISSION_GRANTED }.all { it == true }
    return recheck

}

fun Context.permissionGranted(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

fun Context.permissionsMissing(perms: List<String>) =
        perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }


abstract class RegisterHelper(val context: Context, val retryInterval: Long = 1000) {

    private val registerHook = { register() }


    abstract val requiredPermissions: List<String>

    fun register() {
        if (context.permissionsMissing(requiredPermissions).isEmpty()) {
            onRegister()
        } else {
            context.postDelayed(registerHook, retryInterval)
        }
    }

    abstract protected fun onRegister()

    fun unregister() {
        context.removeCallbacks(registerHook)
        onUnregister()
    }

    abstract protected fun onUnregister()
}
