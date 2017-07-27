package elmdroid.elmdroid.example2

import android.content.Context
import android.content.Intent
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example2orig.Example2OrigDrawer
import kotlinx.android.synthetic.main.app_bar_drawer.*
import kotlinx.android.synthetic.main.nav_activity_drawer.*
import saffih.elmdroid.ElmBase

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 24/04/17.
 */


// UI options

enum class NavOption(val id: Int) {
    Camera(R.id.nav_camera),
    Gallery(R.id.nav_gallery),
    Slideshow(R.id.nav_slideshow),
    Manage(R.id.nav_manage),
    Share(R.id.nav_share),
    Send(R.id.nav_send);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}

enum class ItemOption(val id: Int) {
    settings(R.id.action_settings);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}


/**
 * Messages:
 * Init - inital start of the app, default myModel.
 * the drawer property default opened = false therefor the start with the drawer closed.
 *
 * Fab - is the FloatActionBar which is activate by click
 *
 */
sealed class Msg {
    object Init : Msg()

    sealed class Fab : Msg() {
        class Clicked(val v: ViewId?) : Fab()
        class ClickedDone(val v: Clicked) : Fab()
    }

    sealed class Option : Msg() {
        class Navigation(val item: MenuItem) : Option()
        class ItemSelected(val item: MenuItem) : Option()
        class Drawer(val item: DrawerOption = DrawerOption.opened) : Option()
    }

    sealed class Action : Msg() {
        class GotOrig : Action()
        class UIToast(val txt: String, val duration: Int = Toast.LENGTH_SHORT) : Action()
    }
}


fun Msg.Action.UIToast.show(me: Context) {
    val toast = Toast.makeText(me, txt, duration)
    toast.show()
}


/**
 * Types used as in message properties
 */
class ViewId(val id: Int)

sealed class DrawerOption {
    object opened : DrawerOption()
    object closed : DrawerOption()
}


/**
 * Model representing the state of the system
 * All Model types are Prefixed with M
 */
data class Model(val activity: MActivity = MActivity())

data class MActivity(val toolbar: MToolbar = MToolbar(),
                     val fab: MFab = MFab(),
                     val options: MOptions = MOptions())

data class MToolbar(val show: Boolean = false)
data class MFab(val clicked: ViewId? = null,
                val snackbar: MSnackbar = MSnackbar())

data class MSnackbar(val txt: String = "Snack bar message",
                     val action: MSnackbarAction = MSnackbarAction())

data class MSnackbarAction(val name: String = "Action name",
                           val msg: Msg.Action = Msg.Action.GotOrig())

data class MOptions(val drawer: MDrawer = MDrawer(),
                    val navOption: MNavOption = MNavOption(),
                    val itemOption: MItemOption = MItemOption()
)

data class MDrawer(val i: DrawerOption = DrawerOption.closed)
data class MNavOption(val toDisplay: Boolean = true, val nav: NavOption? = null)
data class MItemOption(val handled: Boolean = true, val item: ItemOption? = null)


class ElmApp(override val me: DrawerExample) : ElmBase<Model, Msg>(me),
        NavigationView.OnNavigationItemSelectedListener {
    override fun init(): Model {
        dispatch(Msg.Init)
        return Model()
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> model
            else -> {
                val sm = update(msg, model.activity)
                model.copy(activity = sm)
            }
        }
    }


    fun update(msg: Msg, model: MActivity): MActivity {
        return when (msg) {
            Msg.Init -> model

            is Msg.Fab -> {
                val sm = update(msg, model.fab)
                model.copy(fab = sm)
            }
            is Msg.Option -> {
                val sm = update(msg, model.options)
                model.copy(options = sm)
            }

            is Msg.Action.GotOrig -> {
                me.startActivity(
                        Intent(me, Example2OrigDrawer::class.java))
                model
            }
            is Msg.Action.UIToast -> {
                msg.show(me)
                model
            }
        }
    }


    fun update(msg: Msg.Option, model: MOptions): MOptions {
        return when (msg) {
            is Msg.Option.ItemSelected -> {
                val m = update(msg, model.itemOption)
                model.copy(itemOption = m)
            }
            is Msg.Option.Navigation -> {
                val m = update(msg, model.navOption)
                model.copy(navOption = m)
            }
            is Msg.Option.Drawer -> {
                val m = update(msg, model.drawer)
                model.copy(drawer = m)
            }
        }
    }

    fun toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
        val toast = Toast.makeText(me, txt, duration)
        toast.show()
    }

    private fun update(msg: Msg.Option.Navigation, model: MNavOption): MNavOption {
        //        return myModel)
        val item = msg.item
        // Handle navigation view item clicks here.
        val id = item.itemId
        val nav = NavOption.byId(id)
        return if (nav == null) {
            dispatch(Msg.Option.Drawer(DrawerOption.closed))
            model.copy(nav = null)
        } else {
            // either use action - more idiomatic like this
            //when (nav) {
            //    NavOption.Camera -> myModel.copy(nav = nav, toDisplay = true),
            //            Msg.Action.UIToast("${nav} not Implemented"))
            //}
            // or just toast.
            when (nav) {
                NavOption.Camera -> toast("${nav} not Implemented")
                NavOption.Gallery -> toast("${nav} not Implemented")
                NavOption.Slideshow -> toast("${nav} not Implemented")
                NavOption.Manage -> toast("${nav} not Implemented")
                NavOption.Share -> toast("${nav} not Implemented")
                NavOption.Send -> toast("${nav} not Implemented")
            }
            model.copy(nav = nav, toDisplay = true)

        }
    }

    private fun update(msg: Msg.Option.ItemSelected, model: MItemOption): MItemOption {
        val item = msg.item
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        val selected = ItemOption.byId(id)
        return when (selected) {

            ItemOption.settings -> {
                dispatch(Msg.Action.UIToast("Setting was clicked"))
                MItemOption(item = selected)
            }
            else -> model.copy(handled = false)
        }
    }

    fun update(msg: Msg.Option.Drawer, model: MDrawer): MDrawer {
        return when (msg.item) {
            DrawerOption.opened -> model.copy(i = DrawerOption.opened)
            DrawerOption.closed -> model.copy(i = DrawerOption.closed)
        }
    }

    fun update(msg: Msg.Fab, model: MFab): MFab {
        return when (msg) {
            is Msg.Fab.Clicked -> {
                model.copy(clicked = msg.v)
            }
            is Msg.Fab.ClickedDone ->
                model.copy(clicked = null)
        }
    }

    override fun view(model: Model, pre: Model?) {
        checkView({}, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = {
            me.setContentView(R.layout.nav_activity_drawer)
        }
        checkView(setup, model, pre) {
            view(model.toolbar, pre?.toolbar)
            view(model.fab, pre?.fab)
            view(model.options, pre?.options)
        }
    }

    private fun view(model: MOptions, pre: MOptions?) {
        val setup = {
            val navigationView = me.nav_view
            navigationView.setNavigationItemSelectedListener(this)
        }
        checkView(setup, model, pre) {
            view(model.drawer, pre?.drawer)
            view(model.itemOption, pre?.itemOption)
            view(model.navOption, pre?.navOption)
        }
    }

    private fun view(model: MDrawer, pre: MDrawer?) {
        val setup = {
            val drawer = me.drawer_layout

            val toolbar = me.toolbar

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

        checkView(setup, model, pre) {
            val drawer = me.drawer_layout
            when (model.i) {
                DrawerOption.opened -> drawer.openDrawer(GravityCompat.START)
                DrawerOption.closed -> drawer.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun view(model: MNavOption, pre: MNavOption?) {
        checkView({}, model, pre) {

        }
    }

    private fun view(model: MItemOption, pre: MItemOption?) {
        checkView({}, model, pre) {
        }
    }

    fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.toolbar
            me.setSupportActionBar(toolbar)
        }
        checkView(setup, model, pre) {
        }

    }

    fun view(model: MFab, pre: MFab?) {
        val setup = {
            val fab = me.fab
            // Listen for a click on a FAB view and send it's viewId.
            fab.setOnClickListener {
                dispatch(Msg.Fab.Clicked(v = ViewId(it.id)))
            }
        }
        checkView(setup, model, pre) {
            // create and show the snack
            if (model.clicked != null) {
                val v = me.findViewById(model.clicked.id)

                Snackbar.make(v, "Goto original studio generated activity", Snackbar.LENGTH_LONG)
                        .setAction("Goto", { dispatch(Msg.Action.GotOrig()) }).show()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        dispatch(Msg.Option.Navigation(item))
        return this.myModel.activity.options.navOption.toDisplay
    }

}


/**
 * Nicer Listener API for the drawer
 */
open class BlankDrawerListener : DrawerLayout.DrawerListener {
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View) {
    }

    override fun onDrawerClosed(drawerView: View) {
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    fun registerAt(drawerLayout: DrawerLayout) {
        drawerLayout.addDrawerListener(this)
    }

    operator fun invoke(drawerLayout: DrawerLayout) {
        registerAt(drawerLayout)
    }
}

open class OpenedDrawerListener(val f: (View) -> Unit) : BlankDrawerListener() {
    override fun onDrawerOpened(drawerView: View) {
        f(drawerView)
    }
}

open class ClosedDrawerListener(val f: (View) -> Unit) : BlankDrawerListener() {
    override fun onDrawerClosed(drawerView: View) {
        f(drawerView)
    }

}


