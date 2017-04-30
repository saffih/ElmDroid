package elmdroid.elmdroid.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import elmdroid.elmdroid.ElmBase
import elmdroid.elmdroid.Que
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import elmdroid.elmdroid.R
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import elmdroid.elmdroid.example1.ExampleHelloWorldActivity
import elmdroid.elmdroid.example2.DrawerExample
import elmdroid.elmdroid.example3.MapsActivity
import elmdroid.elmdroid.example3orig.MapsActivityOrig
import elmdroid.elmdroid.example4.Example4Tabbed
import elmdroid.elmdroid.example5.ItemListActivity
import us.feras.mdv.MarkdownView


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */

sealed class Msg {
    class Init(savedInstanceState: Bundle?) : Msg()
    sealed class Fab : Msg() {
        class Clicked(val view: View) : Fab() {

        }

    }

    sealed class Option : Msg() {
        class Navigation(val item: MenuItem) : Option()
        class ItemSelected(val item: MenuItem) : Option()
        class Drawer(val item: DrawerOption = DrawerOption.opened) : Option()
    }

    sealed class Action : Msg() {
        class OpenTwitter(val name: String) : Action()
    }
}

data class Model(val activity: MActivity = MActivity())
data class MActivity(
        val pager: MViewPager = MViewPager(),
        val fab: MFab = MFab(),
        val toolbar: MToolbar = MToolbar(),
        val options: MOptions = MOptions())

data class MOptions(val drawer: MDrawer = MDrawer(),
                    val navOption: NavOption? = null,
                    val itemOption: ItemOption? = null,
                    val itemSelectedHandled: Boolean = false
)

data class MViewPager(val i: Int = 0)
data class MToolbar(val i: Int = 0)
data class MFab(val snackbar: MSnackbar = MSnackbar())
data class MSnackbar(val i: Int = 0)

data class MDrawer(val i: DrawerOption = DrawerOption.closed)


//sealed class Action:Msg(){
//    class GotOrig :Action()
//}


/**
 * Types used as in message properties
 */
//class ViewId(val id:Int)

sealed class DrawerOption {
    object opened : DrawerOption()
    object closed : DrawerOption()
}

sealed class ItemOption(val handled: Boolean = true) {
    class Settings : ItemOption()
}

sealed class NavOption(val toDisplay: Boolean = true) {
    class NavHelloWorld : NavOption()
    class NavDrawer : NavOption()
    class NavTabbed : NavOption()
    class NavMasterDetails : NavOption()
    class NavMaps : NavOption()
    class NavMapsOrig : NavOption()
}


// TOs transfer objects

class ShowCaseElm(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me),
        NavigationView.OnNavigationItemSelectedListener {

    override fun init(savedInstanceState: Bundle?): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init(savedInstanceState))
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {

            is Msg.Init -> {
                ret(model)
            }
            else -> {
                val (m, q) = update(msg, model.activity)
                ret(model.copy(activity = m), q)
            }

        }
    }


    fun update(msg: Msg, model: MActivity): Pair<MActivity, Que<Msg>> {
//        return ret(model)

        return when (msg) {

            is Msg.Init -> {
                ret(model)
            }
            is Msg.Fab.Clicked -> {
                Snackbar.make(msg.view, "Exit", Snackbar.LENGTH_LONG)
                        .setAction("Finish", { me.finish() }).show()
                ret(model)
            }
            is Msg.Option -> {
                val (m, q) = update(msg, model.options)
                ret(model.copy(options = m), q)
            }
            is Msg.Action.OpenTwitter -> {
                val name = msg.name
                me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${name}")))
                ret(model)
            }
        }
    }

    fun update(msg: Msg.Option, model: MOptions): Pair<MOptions, Que<Msg>> {
        return when (msg) {
            is Msg.Option.ItemSelected -> {
                val item = msg.item
                // Handle action bar item clicks here. The action bar will
                // automatically handle clicks on the Home/Up button, so long
                // as you specify a parent activity in AndroidManifest.xml.
                val id = item.itemId

                val selected = when {
                    (id == R.id.action_settings) -> ItemOption.Settings()
                    else -> null
                }
                ret(model.copy(itemOption = selected))
            }
            is Msg.Option.Navigation -> {
                val item = msg.item
                // Handle navigation view item clicks here.
                val id = item.itemId
                val nav: NavOption? = when {
                    (id == R.id.nav_helloworld) ->
                        // Handle the camera action
                        NavOption.NavHelloWorld()
                    (id == R.id.nav_drawer) ->
                        NavOption.NavDrawer()
                    (id == R.id.nav_tabbed) ->
                        NavOption.NavTabbed()
                    (id == R.id.nav_masterdetails) ->
                        NavOption.NavMasterDetails()
                    (id == R.id.nav_maps) ->
                        NavOption.NavMaps()
                    (id == R.id.nav_mapsorig) ->
                        NavOption.NavMapsOrig()
                    else -> null // close the drawer
                }

                if (nav == null) {
                    // dispatch(Msg.Option.Drawer(DrawerOption.closed))
                    ret(model.copy(navOption = nav), Msg.Option.Drawer(DrawerOption.closed))
                } else {
                    when (nav){
                        is NavOption.NavHelloWorld -> me.startActivity(
                                Intent(me, ExampleHelloWorldActivity::class.java))
                        is NavOption.NavDrawer -> me.startActivity(
                                Intent(me, DrawerExample::class.java))
                        is NavOption.NavTabbed -> me.startActivity(
                                Intent(me, Example4Tabbed::class.java))
                        is NavOption.NavMasterDetails -> me.startActivity(
                                Intent(me, ItemListActivity::class.java))
                        is NavOption.NavMaps -> me.startActivity(
                                Intent(me, MapsActivity::class.java))
                        is NavOption.NavMapsOrig -> me.startActivity(
                                Intent(me, MapsActivityOrig::class.java))
                    }
                    ret(model.copy(navOption = nav))
                }
            }
            is Msg.Option.Drawer -> {
                val (sm, sc) = update(msg, model.drawer)
                ret(model.copy(drawer = sm), sc)
            }
        }
    }


    fun update(msg: Msg.Option.Drawer, model: MDrawer): Pair<MDrawer, Que<Msg>> {
        return when (msg.item) {
            DrawerOption.opened -> ret(model.copy(i = DrawerOption.opened))
            DrawerOption.closed -> ret(model.copy(i = DrawerOption.closed))
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
            val markdownView = me.findViewById(R.id.markdownView) as MarkdownView
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
            val toolbar = me.findViewById(R.id.toolbar) as Toolbar
            me.setSupportActionBar(toolbar)
        }
        checkView(setup, model, pre) {
            //view(model., pre?. )
        }
    }

    private fun view(model: MFab, pre: MFab?) {
        val setup = {
            val fab = me.findViewById(R.id.fab) as FloatingActionButton
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
            //view(model., pre?. )
        }
    }

    private fun view(model: MOptions, pre: MOptions?) {
        val setup = {
            val navigationView = me.findViewById(R.id.nav_view) as NavigationView
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
            val drawer = me.findViewById(R.id.drawer_layout) as DrawerLayout
            val navView = me.findViewById(R.id.nav_view) as NavigationView
            val parentLayout = navView.getHeaderView(0)
            val nameView = parentLayout.findViewById(R.id.nameTextView) as TextView
            val name = me.getResources().getString(R.string.twitter_account)
            nameView.text = "@" + name
            nameView.setOnClickListener { view -> dispatch(Msg.Action.OpenTwitter(name)) }

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

        checkView(setup, model, pre) {
            val drawer = me.findViewById(R.id.drawer_layout) as DrawerLayout
            when (model.i) {
                DrawerOption.opened -> drawer.openDrawer(GravityCompat.START)
                DrawerOption.closed -> drawer.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun view(model: NavOption?, pre: NavOption?) {
        checkView({}, model, pre) {
            //view(model., pre?.a )
        }
    }

    private fun view(model: ItemOption?, pre: ItemOption?) {
        checkView({}, model, pre) {
            //view(model., pre?.a )
        }
    }

//    val setup = {
//
//    }
//
//    checkView(setup, model, pre) {
////            view(model., pre?. )
//    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        dispatch(Msg.Option.Navigation(item))
        return this.model.activity.options.navOption?.toDisplay ?: false
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


