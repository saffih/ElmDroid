package elmdroid.elmdroid.example1

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.Menu
import android.view.MenuItem
import elmdroid.elmdroid.*



data class MToolBar (val show : Boolean = false)
data class MFloatAction (val clicked : Boolean = false)
data class MActivity (
        val tb : MToolBar = MToolBar(),
        val fab : MFloatAction = MFloatAction()
)
data class Model (val activity : MActivity = MActivity())

sealed class MainMsg : IMsg() {
    sealed class FabMsg : MainMsg(){
        class FabClicked(val view: View): FabMsg()
    }

    object Init : MainMsg()
}


class ElmApp(override val me: AppCompatActivity) : ElmBase<Model, MainMsg>(me) {
    override fun init(): MC<Model, MainMsg> = retModelCmd(Model(), MainMsg.Init)

    override fun update(msg: MainMsg, model: Model): MC<Model, MainMsg> {
        return when (msg){
            is MainMsg.Init -> {
                ret(Model())
            }

            is MainMsg.FabMsg-> {
                val (sm, sc) = update(msg, model.activity)
                ret(model.copy(activity = sm), sc)
            }
        }
    }

    fun update(msg: MainMsg, model: MActivity): MC<MActivity, MainMsg> {
        return when (msg){
            is MainMsg.FabMsg -> {
                val (sm, sc) = update(msg, model.fab)
                ret(model.copy(fab= sm), sc)
            }

            else -> {
                ret(model)
            }
        }
    }
    fun update(msg: MainMsg.FabMsg, model: MFloatAction): MC<MFloatAction, MainMsg> {
        return when (msg){
            is MainMsg.FabMsg.FabClicked -> {
                ret(model.copy(clicked = true))
            }

            else -> {
                ret(model)
            }
        }
    }


    override fun view(model: Model) {
        val pre=model_viewed
        if (model===pre) return
        view_activity(model.activity, pre?.activity)
    }

    fun view_activity(model: MActivity, pre: MActivity?){
        if (model===pre) return
        if (pre===null){
            me.setContentView(R.layout.activity_example1)
        }
        view_tb(model.tb, pre?.tb)
        view_float_action(model.fab, pre?.fab)
//        me.setSupportActionBar(me.findViewById(R.id.toolbar) as Toolbar)
    }

    fun  view_tb(model: MToolBar, pre: MToolBar?) {
        if (model===pre) return
        if (pre===null) {
            me.setSupportActionBar(me.findViewById(R.id.toolbar) as Toolbar)
        }
    }
    fun  view_float_action(model: MFloatAction, pre: MFloatAction?) {
        if (model===pre) return
        if (pre===null) {
            me.findViewById(R.id.fab).setOnClickListener { dispatch(MainMsg.FabMsg.FabClicked(it)) }
        }
        if (model.clicked) {
            Snackbar.make(me.findViewById(R.id.fab), "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

}

class Example1 : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.mainLoop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_example1, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}


