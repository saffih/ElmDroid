package elmdroid.elmdroid.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example1.ExampleHelloWorldActivity
import elmdroid.elmdroid.example2.DrawerExample
import elmdroid.elmdroid.example3.MapsActivity
import elmdroid.elmdroid.example3orig.MapsActivityOrig
import elmdroid.elmdroid.example4.TabbedActivity
import elmdroid.elmdroid.example5.ItemListActivity
import kotlinx.android.synthetic.main.activity_show_case.*
import kotlinx.android.synthetic.main.app_bar_show_case.*
import kotlinx.android.synthetic.main.nav_header_showcase.view.*
import saffih.elmdroid.ElmBase


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */

// UI options
/**
 * class wrapping the id provided by ui,
 * providing strong typed data type
 * support reverse lookup
 */
enum class NavOption(val id: Int) {
    HelloWorld(R.id.nav_helloworld),
    Drawer(R.id.nav_drawer),
    Tabbed(R.id.nav_tabbed),
    MasterDetails(R.id.nav_masterdetails),
    MapsService(R.id.nav_maps_service),
    MapsLocalService(R.id.nav_maps_local_service),
    MapsOrig(R.id.nav_mapsorig);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}

/**
 * class wrapping the id provided by ui,
 * providing strong typed data type
 * support reverse lookup
 */
enum class ItemOption(val id: Int) {
    settings(R.id.action_settings);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}


/**
 * Nested hierarchy of handled messages
 */
sealed class Msg {
    class Init : Msg()
    sealed class Fab : Msg() {
        class Clicked(val view: View) : Fab()
    }

    sealed class Option : Msg() {
        class Navigation(val item: MenuItem) : Option()
        class ItemSelected(val item: MenuItem) : Option()
        class Drawer(val item: DrawerOption = DrawerOption.opened) : Option()
    }

    sealed class Action : Msg() {
        class OpenTwitter(val name: String) : Action()
        class UIToast(val txt: String, val duration: Int = Toast.LENGTH_SHORT) : Action()
    }
}

/**
 * shugaring extention
 */
fun Msg.Action.UIToast.show(me: Context) {
    val toast = Toast.makeText(me, txt, duration)
    toast.show()
}

data class Model(val activity: MActivity = MActivity())
data class MActivity(
        val pager: MViewPager = MViewPager(),
        val fab: MFab = MFab(),
        val toolbar: MToolbar = MToolbar(),
        val options: MOptions = MOptions())

data class MOptions(val drawer: MDrawer = MDrawer(),
                    val navOption: MNavOption = MNavOption(),
                    val itemOption: MItemOption = MItemOption())

data class MViewPager(val i: Int = 0)
data class MToolbar(val i: Int = 0)
data class MFab(val snackbar: MSnackbar = MSnackbar())
data class MSnackbar(val i: Int = 0)

data class MDrawer(val i: DrawerOption = DrawerOption.closed)
data class MNavOption(val toDisplay: Boolean = true, val nav: NavOption? = null)
data class MItemOption(val handled: Boolean = true, val item: ItemOption? = null)


sealed class DrawerOption {
    object opened : DrawerOption()
    object closed : DrawerOption()
}


class ShowCaseElm(override val me: ShowCase) : ElmBase<Model, Msg>(me),
        NavigationView.OnNavigationItemSelectedListener {

    override fun init(): Model {
        dispatch(Msg.Init())
        return Model()
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> {
                model
            }
            else -> {
                val m = update(msg, model.activity)
                model.copy(activity = m)
            }
        }
    }

    fun update(msg: Msg, model: MActivity): MActivity {
        return when (msg) {
            is Msg.Init -> {
                model
            }
            is Msg.Fab.Clicked -> {
                Snackbar.make(msg.view, "Exit", Snackbar.LENGTH_LONG)
                        .setAction("Finish", { me.finish() }).show()
                model
            }
            is Msg.Option -> {
                val m = update(msg, model.options)
                model.copy(options = m)
            }
            is Msg.Action.OpenTwitter -> {
                val name = msg.name
                me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${name}")))
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

    private fun update(msg: Msg.Option.ItemSelected, model: MItemOption): MItemOption {
//         return myModel)
        val item = msg.item
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        val selected = ItemOption.byId(id)
        return when (selected) {
            ItemOption.settings -> MItemOption(item = selected)
            else -> model.copy(handled = false)
        }
    }

    private fun update(msg: Msg.Option.Navigation, model: MNavOption): MNavOption {
        val item = msg.item
        // Handle navigation view item clicks here.
        val id = item.itemId
        val nav = NavOption.byId(id)
        return if (nav == null) {
            dispatch(Msg.Option.Drawer(DrawerOption.closed))
            model.copy(nav = null)
        } else {
            when (nav) {
                NavOption.HelloWorld -> me.startActivity(
                        Intent(me, ExampleHelloWorldActivity::class.java))
                NavOption.Drawer -> me.startActivity(
                        Intent(me, DrawerExample::class.java))
                NavOption.Tabbed -> me.startActivity(
                        Intent(me, TabbedActivity::class.java))
                NavOption.MasterDetails -> me.startActivity(
                        Intent(me, ItemListActivity::class.java))
                NavOption.MapsService -> me.startActivity(
                        Intent(me, MapsActivity::class.java).setAction("service"))
                NavOption.MapsLocalService -> me.startActivity(
                        Intent(me, MapsActivity::class.java).setAction("localService"))
                NavOption.MapsOrig -> me.startActivity(
                        Intent(me, MapsActivityOrig::class.java))
            }
            model.copy(nav = nav, toDisplay = true)

        }
    }

    fun update(msg: Msg.Option.Drawer, model: MDrawer): MDrawer {
        return when (msg.item) {
            DrawerOption.opened -> model.copy(i = DrawerOption.opened)
            DrawerOption.closed -> model.copy(i = DrawerOption.closed)
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {
        }
        checkView(setup, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = {
            me.setContentView(R.layout.activity_show_case)
            val markdownView = me.markdownView
            markdownView.loadMarkdownFile("https://raw.githubusercontent.com/saffih/ElmDroid/master/README.md")
        }
        checkView(setup, model, pre) {
            view(model.fab, pre?.fab)
            view(model.toolbar, pre?.toolbar)
            view(model.options, pre?.options)
        }
    }


    private fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.toolbar
            me.setSupportActionBar(toolbar)
        }
        checkView(setup, model, pre) {
            //view(myModel., pre?. )
        }
    }

    private fun view(model: MFab, pre: MFab?) {
        val setup = {
            val fab = me.fab
            fab.setOnClickListener { view -> dispatch(Msg.Fab.Clicked(view)) }
        }

        checkView(setup, model, pre) {
            view(model.snackbar, pre?.snackbar)
        }
    }

    private fun view(model: MSnackbar, pre: MSnackbar?) {
        val setup = {
        }
        checkView(setup, model, pre) {
            //view(myModel., pre?. )
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
            val navView = me.nav_view
            val parentLayout = navView.getHeaderView(0)
            val nameView = parentLayout.nameTextView
            val name = me.resources.getString(R.string.twitter_account)
            nameView.text = "@" + name
            nameView.setOnClickListener { view -> dispatch(Msg.Action.OpenTwitter(name)) }

            val toolbar = me.toolbar

            val toggle = ActionBarDrawerToggle(
                    me, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer.addDrawerListener(toggle)
            val openListener = OpenedDrawerListener {
                dispatch(Msg.Option.Drawer(DrawerOption.opened))
            }
            val closeListener = ClosedDrawerListener {
                dispatch(Msg.Option.Drawer(DrawerOption.closed))
            }
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

    private fun view(model: MNavOption?, pre: MNavOption?) {
        checkView({}, model, pre) {
            //view(myModel., pre?.a )
        }
    }

    private fun view(model: MItemOption?, pre: MItemOption?) {
        checkView({}, model, pre) {
            //view(myModel., pre?.a )
        }
    }

//    val setup = {
//
//    }
//
//    checkView(setup, myModel, pre) {
////            view(myModel., pre?. )
//    }

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


