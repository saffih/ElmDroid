package elmdroid.elmdroid.example5.detail

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import elmdroid.elmdroid.ElmBase
import elmdroid.elmdroid.Que
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example5.ItemDetailFragment

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 28/04/17.
 */




sealed class Msg {
    class Init(val savedInstanceState: Bundle?=null) : Msg()

    sealed class Fab : Msg(){
        class Clicked(val v: MFabClicked) : Fab()
    }
    sealed class Action:Msg(){
        class DoSomething:Action()
    }

//    sealed class Option : Msg() {
//        class Navigation(val item: NavOption) : Option()
//        class ItemSelected(val item: Any) : Option()
//        class Drawer(val item: DrawerOption = DrawerOption.opened) : Option()
//    }
}



/**
 * Types used as in message properties
 */
//class ViewId(val id:Int)
//fun Int.viewId() = ViewId(this)
//val View.viewId: ViewId get () = this.id.viewId()


/**
 * Model representing the state of the system
 * All Model types are Prefixed with M
 */
data class Model (val activity : MActivity= MActivity())
data class MActivity (val toolbar : MToolbar= MToolbar(),
                      val fab:MFab= MFab()
//                      val options:MOptions= MOptions()
)
data class MToolbar (val show : Boolean = false)
data class MFab (val clicked: MFabClicked?=null,
                 val snackbar:MSnackbar= MSnackbar())
data class MFabClicked(val clicked : View)
data class MSnackbar (val txt: String="Snack bar message",
                      val action: MSnackbarAction= MSnackbarAction())
data class MSnackbarAction (val name : String="Action name",
                            val msg:Msg.Action = Msg.Action.DoSomething())

//data class MOptions(val drawer: MDrawer = MDrawer(),
//                    val navOption: NavOption?=null,
//                    val itemOption: ItemOption?=null
//)

//data class MDrawer (val i: DrawerOption = DrawerOption.closed)

class ItemDetailElm(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me) {
    override fun init(savedInstanceState: Bundle?): Pair<Model, Que<Msg>> {
        return  ret(Model(), Msg.Init(savedInstanceState))
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        val (activity, q) = update(msg, model.activity)
        return ret(model.copy(activity = activity), q)
    }

    private fun update(msg: Msg, model: MActivity): Pair<MActivity, Que<Msg>> {
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
                if (msg.savedInstanceState == null) {
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
                ret(model)
            }
            is Msg.Fab.Clicked -> {
                val (mFab, q) =update(msg, model.fab)
                ret(model.copy(fab=mFab), q)
            }
            is Msg.Action.DoSomething -> {
                ret(model)
            }
        }
    }

    private fun update(msg: Msg.Fab, model: MFab): Pair<MFab, Que<Msg>> {
        return when (msg) {
            is Msg.Fab.Clicked -> {
                ret(model.copy(clicked = msg.v))
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
            val fab = me.findViewById(R.id.fab) as FloatingActionButton
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
                Snackbar.make(model.clicked, "Replace with your own detail action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }
    }

    private fun view(model: MSnackbar, pre: MSnackbar?) {
        val setup = {
        }
        checkView(setup, model, pre) {
//            view(model.x, pre?.x)
        }
    }

    private fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.findViewById(R.id.detail_toolbar) as Toolbar
            me.setSupportActionBar(toolbar)
        }

        checkView(setup, model, pre)
        {
            //            view(model., pre.)
        }
    }
//        val setup = {
//        }
//        checkView(setup, model, pre) {
//            view(model.x, pre?.x)
//        }


//
//    }


//    val setup = {
//    }
//    checkView(setup, model, pre){
////            view(model., pre.)
//    }


}
