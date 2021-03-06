package elmdroid.elmdroid.example5.detail

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example5.ItemDetailActivity
import elmdroid.elmdroid.example5.ItemDetailFragment
import kotlinx.android.synthetic.main.activity_item_detail.*
import saffih.elmdroid.ElmBase

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 28/04/17.
 */

enum class ItemOption(val id: Int) {
    settings(R.id.action_settings);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}


sealed class Msg {
    class Init : Msg()

    sealed class Fab : Msg(){
        class Clicked(val v: MFabClicked) : Fab()
    }
    sealed class Action:Msg(){
        class DoSomething:Action()
        class UIToast(val txt: String, val duration: Int = Toast.LENGTH_SHORT) : Action()
    }

    sealed class Option : Msg() {
//        class Navigation(val item: MNavOption) : Option()
class ItemSelected(val item: MenuItem) : Option()
//        class Drawer(val item: DrawerOption = DrawerOption.opened) : Option()
    }
}


fun Msg.Action.UIToast.show(me: Context) {
    val toast = Toast.makeText(me, txt, duration)
    toast.show()
}

/**
 * Model representing the state of the system
 * All Model types are Prefixed with M
 */
data class Model(val activity: MActivity = MActivity(), val hadSavedState: Boolean = false)
data class MActivity (val toolbar : MToolbar= MToolbar(),
                      val fab: MFab = MFab(),
                      val options: MOptions = MOptions()
)
data class MToolbar (val show : Boolean = false)
data class MFab (val clicked: MFabClicked?=null,
                 val snackbar:MSnackbar= MSnackbar())
data class MFabClicked(val clicked : View)
data class MSnackbar (val txt: String="Snack bar message",
                      val action: MSnackbarAction= MSnackbarAction())
data class MSnackbarAction (val name : String="Action name",
                            val msg:Msg.Action = Msg.Action.DoSomething())

data class MOptions(val itemOption: MItemOption = MItemOption())
data class MItemOption(val handled: Boolean = true, val item: ItemOption? = null)

class ItemDetailElm(override val me: ItemDetailActivity) : ElmBase<Model, Msg>(me) {
    var savedInstanceState: Bundle? = null
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        this.savedInstanceState = savedInstanceState
    }

    override fun init():Model {
        dispatch(Msg.Init())
        return  Model().copy(hadSavedState = (savedInstanceState != null))
                
    }

    override fun update(msg: Msg, model: Model):Model {
        return when (msg) {
            is Msg.Init -> {
                // savedInstanceState is non-null when there is fragment state
                // saved from previous configurations of this activity
                // (e.g. when rotating the screen from portrait to landscape).
                // In this case, the fragment will automatically be re-added
                // to its container so we don't need to manually add it.
                // For more information, see the Fragments API guide at:
                //
                // http://developer.android.com/guide/components/fragments.html
                //
                if (!model.hadSavedState) { // create initial one
                    // Create the detail fragment and add it to the activity
                    // using a fragment transaction.
                    val arguments = Bundle()
                    arguments.putString(ItemDetailFragment.ARG_ITEM_ID,
                            me.intent.getStringExtra(ItemDetailFragment.ARG_ITEM_ID))
                    val fragment = ItemDetailFragment()
                    fragment.arguments = arguments
                    me.supportFragmentManager.beginTransaction()
                            .add(R.id.item_detail_container, fragment)
                            .commit()
                }
                model
            }
            is Msg.Fab -> {
                val activity = update(msg, model.activity)
                model.copy(activity = activity)
            }
            is Msg.Action -> {
                val activity = update(msg, model.activity)
                model.copy(activity = activity)
            }
            is Msg.Option -> {
                val activity = update(msg, model.activity)
                model.copy(activity = activity)
            }
        }
    }

    private fun update(msg: Msg.Fab, model: MActivity):MActivity {
        return when (msg) {
            is Msg.Fab.Clicked -> {
                val mFab = update(msg, model.fab)
                model.copy(fab = mFab)
            }
        }
    }

    private fun update(msg: Msg.Action, model: MActivity):MActivity {
        return when (msg) {
            is Msg.Action.DoSomething -> {
                model
            }
            is Msg.Action.UIToast -> {
                msg.show(me)
                model
            }
        }
    }

    private fun update(msg: Msg.Option, model: MActivity):MActivity {
        return when (msg) {
            is Msg.Option -> {
                val m = update(msg, model.options)
                model.copy(options = m)
            }
        }
    }

    fun update(msg: Msg.Option, model: MOptions):MOptions {
//        return myModel)
        return when (msg) {

            is Msg.Option.ItemSelected -> {
                val m = update(msg, model.itemOption)
                model.copy(itemOption = m)
            }
        }
    }

    private fun update(msg: Msg.Option.ItemSelected, model: MItemOption):MItemOption {
//        return myModel)
        val itemOption = ItemOption.byId(msg.item.itemId)
        return if (itemOption == null) MItemOption(handled = false)
        else when (itemOption) {
            ItemOption.settings -> {
                dispatch(Msg.Action.UIToast("Setting was clicked"))
                MItemOption(item = itemOption)}
        }
    }

    private fun update(msg: Msg.Fab, model: MFab):MFab {
        return when (msg) {
            is Msg.Fab.Clicked -> {
                model.copy(clicked = msg.v)
            }
        }
    }


    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {

        val setup = {
            me.setContentView(R.layout.activity_item_detail)
            // Show the Up button in the action bar.
            val actionBar = me.supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(true)
            Unit
        }
        checkView(setup, model, pre) {
            view(model.toolbar, pre?.toolbar)
            view(model.fab, pre?.fab)
        }
    }

    private fun view(model: MFab, pre: MFab?) {
        val setup = {
            val fab = me.fab
            fab.setOnClickListener { view -> dispatch(Msg.Fab.Clicked(MFabClicked(view)))}
        }
        checkView(setup, model, pre){
            view(model.snackbar, pre?.snackbar)
            view(model.clicked, pre?.clicked)

        }
    }

    private fun  view(model: MFabClicked?, pre: MFabClicked?) {
        val setup = {
        }
        checkView(setup, model, pre) {
            if (model!=null){
                Snackbar.make(model.clicked, "exit ?", Snackbar.LENGTH_LONG)
                        .setAction("Exit", { me.finish() }).show()
            }
        }
    }

    private fun view(model: MSnackbar, pre: MSnackbar?) {
        val setup = {
        }
        checkView(setup, model, pre) {
            //            view(myModel.x, pre?.x)
        }
    }

    private fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.detail_toolbar
            me.setSupportActionBar(toolbar)
        }

        checkView(setup, model, pre)
        {
            //            view(myModel., pre.)
        }
    }
//        val setup = {
//        }
//        checkView(setup, myModel, pre) {
//            view(myModel.x, pre?.x)
//        }


//
//    }


//    val setup = {
//    }
//    checkView(setup, myModel, pre){
////            view(myModel., pre.)
//    }


}
