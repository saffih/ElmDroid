package elmdroid.elmdroid.example2

import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import elmdroid.elmdroid.ElmBase
import elmdroid.elmdroid.MC
import elmdroid.elmdroid.R

/**
* Copyright Joseph Hartal (Saffi)
* Created by saffi on 24/04/17.
*/

/**
 * Messages:
 * Init - inital start of the app, default model.
 * the drawer property default opened = false therefor the start with the drawer closed.
 *
 * Fab - is the FloatActionBar which is activate by click
 *
 */
sealed class Msg {
    object Init : Msg()

    sealed class Fab : Msg(){
        class Clicked(val v: ViewId?) : Fab()
        class ClickedDone(val v: Clicked) : Fab()
    }

    sealed class Option : Msg() {
        class Navigation(val item: NavOption) : Option()
        class ItemSelected(val item: ItemOption) : Option()
        class Drawer(val item: DrawerOption  = DrawerOption.opened) : Option()
    }

    sealed class Action:Msg(){
        class DoSomething :Action()
    }
}


/**
 * Types used as in message properties
 */
class ViewId(val id:Int)

sealed class DrawerOption {
    object opened : DrawerOption()
    object closed : DrawerOption()
}

sealed class ItemOption {
    object settings : ItemOption()
}

sealed class NavOption {
    object nav_camera : NavOption()
    object nav_gallery : NavOption()
    object nav_slideshow : NavOption()
    object nav_manage : NavOption()
    object nav_share : NavOption()
    object nav_send : NavOption()
}


/**
 * Model representing the state of the system
 * All Model types are Prefixed with M
 */
data class Model (val activity : MActivity= MActivity())
data class MActivity (val toolbar : MToolbar= MToolbar(),
                      val fab:MFab= MFab(),
                      val options:MOptions= MOptions())
data class MToolbar (val show : Boolean = false)
data class MFab (val clicked : ViewId?=null,
                 val snackbar:MSnackbar= MSnackbar())
data class MSnackbar (val txt: String="Snack bar message",
                      val action: MSnackbarAction= MSnackbarAction())
data class MSnackbarAction (val name : String="Action name",
                            val msg:Msg.Action = Msg.Action.DoSomething())

data class MOptions(val drawer: MDrawer = MDrawer(),
                    val navOption: NavOption?=null,
                    val itemOption: ItemOption?=null
                    )

data class MDrawer (val i: DrawerOption = DrawerOption.closed)



class ElmApp(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me) , NavigationView.OnNavigationItemSelectedListener {
    override fun init(): MC<Model, Msg> = ret(Model(), Msg.Init)

    override fun update(msg: Msg, model: Model): MC<Model, Msg> {
        return when (msg) {
            is Msg.Init -> ret(model)
            else -> {
                val (sm, sc) = update(msg, model.activity)
                ret(model.copy(activity = sm), sc)
            }
        }
    }


    fun update(msg: Msg, model: MActivity): MC<MActivity, Msg> {
        return when (msg){
            Msg.Init -> ret(model)

            is Msg.Fab -> {
                val (sm, sc) = update(msg, model.fab)
                ret(model.copy(fab= sm), sc)
            }
            is Msg.Option -> {
                val (sm, sc) = update(msg, model.options)
                ret(model.copy(options= sm), sc)
            }

            is Msg.Action.DoSomething -> ret(model)
        }
    }

    fun  update(msg: Msg.Option, model: MOptions):MC<MOptions, Msg> {
        return when (msg){
            is Msg.Option.Navigation ->
                ret (model.copy(navOption = msg.item))
            is Msg.Option.ItemSelected ->
                ret (model.copy(itemOption= msg.item))
            is Msg.Option.Drawer -> {
                val (sm, sc) = update(msg, model.drawer)
                ret(model.copy(drawer= sm), sc)
            }
        }
    }

    fun update(msg: Msg.Option.Drawer, model: MDrawer) : MC<MDrawer, Msg> {
        return when(msg.item){
            DrawerOption.opened -> ret(model.copy(i = DrawerOption.opened))
            DrawerOption.closed -> ret(model.copy(i = DrawerOption.closed))
        }
    }

    fun update(msg: Msg.Fab, model:MFab ): MC<MFab, Msg> {
        return when (msg){
            is Msg.Fab.Clicked -> {
                ret(model.copy(clicked = msg.v),
                        Msg.Fab.ClickedDone(msg))
            }
            is Msg.Fab.ClickedDone ->
                ret(model.copy(clicked = null))
        }
    }

    override fun view(model: Model, pre:Model?) {
        checkView( {} , model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = {
            me.setContentView(R.layout.nav_activity_example2)
        }
        checkView( setup , model, pre) {
            view(model.toolbar, pre?.toolbar)
            view(model.fab, pre?.fab)
            view(model.options, pre?.options)
        }
    }

    private fun  view(model: MOptions, pre: MOptions?) {
        val setup = {
            val navigationView = me.findViewById(R.id.nav_view) as NavigationView
            navigationView.setNavigationItemSelectedListener(this)
        }
        checkView( setup , model, pre) {
            view(model.drawer, pre?.drawer)
            view(model.itemOption, pre?.itemOption)
            view(model.navOption, pre?.navOption)
        }
    }

    private fun  view(model: MDrawer, pre: MDrawer?) {
        val setup = {
            val drawer = me.findViewById(R.id.drawer_layout) as DrawerLayout

            val toolbar = me.findViewById(R.id.toolbar) as Toolbar

            val toggle = ActionBarDrawerToggle(
                    me, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer.addDrawerListener(toggle)
            val openListener = OpenedDrawerListener({
                dispatch(Msg.Option.Drawer(DrawerOption.opened))
            })
            val closeListener = ClosedDrawerListener({
                dispatch(Msg.Option.Drawer(DrawerOption.closed))
            })
            closeListener.registerAt(drawer)
            openListener.registerAt(drawer)
            toggle.syncState()
        }

        checkView( setup , model, pre) {
            val drawer = me.findViewById(R.id.drawer_layout) as DrawerLayout
            when (model.i) {
                DrawerOption.opened -> drawer.openDrawer(GravityCompat.START)
                DrawerOption.closed -> drawer.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun  view(model: NavOption?, pre: NavOption?) {
        checkView( {} , model, pre) {

        }
    }

    private fun  view(model: ItemOption?, pre: ItemOption?) {
        checkView( {} , model, pre) {
        }
    }

    fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.findViewById(R.id.toolbar) as Toolbar
            me.setSupportActionBar(toolbar)
        }
        checkView( setup , model, pre) {
        }

    }

    fun view(model: MFab, pre: MFab?) {
        val setup = {
            val fab = me.findViewById(R.id.fab) as FloatingActionButton
            // Listen for a click on a FAB view and send it's id.
            fab.setOnClickListener {
                dispatch(Msg.Fab.Clicked(v=ViewId(it.id)))}
        }
        checkView( setup , model, pre) {
            // create and show the snack
            if (model.clicked!=null){
                val v = me.findViewById(model.clicked.id)

                Snackbar.make(v, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", {dispatch(Msg.Action.DoSomething())}).show()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        if (id == R.id.nav_camera) {
            // Handle the camera action
            dispatch(Msg.Option.Navigation(NavOption.nav_camera))
        } else if (id == R.id.nav_gallery) {
            dispatch(Msg.Option.Navigation(NavOption.nav_gallery))
        } else if (id == R.id.nav_slideshow) {
            dispatch(Msg.Option.Navigation(NavOption.nav_slideshow))
        } else if (id == R.id.nav_manage) {
            dispatch(Msg.Option.Navigation(NavOption.nav_manage))
        } else if (id == R.id.nav_share) {
            dispatch(Msg.Option.Navigation(NavOption.nav_share))
        } else if (id == R.id.nav_send) {
            dispatch(Msg.Option.Navigation(NavOption.nav_send))
        }
        // close the drawer
        dispatch(Msg.Option.Drawer(DrawerOption.closed))

        return true
    }

}


/**
 * Nicer Listener API for the drawer
 */
open class BlankDrawerListener : DrawerLayout.DrawerListener {
    override fun onDrawerSlide(drawerView: View, slideOffset: Float){
    }

    override fun onDrawerOpened(drawerView: View) {
    }

    override fun onDrawerClosed(drawerView: View) {
    }

    override fun onDrawerStateChanged(newState: Int) {
    }
    fun  registerAt(drawerLayout: DrawerLayout) {drawerLayout.addDrawerListener(this)}
    operator fun  invoke(drawerLayout: DrawerLayout) {registerAt(drawerLayout)}
}

open class OpenedDrawerListener(val f:(View)->Unit) : BlankDrawerListener(){
    override fun onDrawerOpened(drawerView: View) {
        f(drawerView)
    }
}

open class ClosedDrawerListener(val f:(View)->Unit) : BlankDrawerListener(){
    override fun onDrawerClosed(drawerView: View) {
        f(drawerView)
    }

}

