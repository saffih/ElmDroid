package elmdroid.elmdroid.example4

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import elmdroid.elmdroid.ElmBase
import elmdroid.elmdroid.Que
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import elmdroid.elmdroid.R
import android.view.Menu
import android.view.MenuItem
import elmdroid.elmdroid.example2orig.MainActivityExample2Orig

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */

sealed class  Msg {
    class Init(savedInstanceState: Bundle?) : Msg()
    sealed class Fab: Msg() {
        class Clicked(val view: View) : Fab() {

        }

    }

    sealed class Options: Msg(){
            class ItemSelected(val item: MenuItem) : Options()
    }
    sealed class Action:Msg(){
        class GotOrig :Action()
    }
}

data class Model(val activity:MActivity = MActivity())
data class MActivity(
        val pager: MViewPager = MViewPager(),
        val fab:MFab = MFab(),
        val toolbar: MToolbar= MToolbar(),
        val options: MOptions= MOptions())
data class MOptions(val itemSelectedHandled:Boolean = false)
data class MViewPager(val i:Int = 0)
data class MToolbar(val i:Int = 0)
data class MFab(val snackbar:MSnackbar = MSnackbar())
data class MSnackbar(val i:Int=0)


// TOs transfer objects

class TabbedElmApp(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me){
    override fun init(savedInstanceState: Bundle?): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init(savedInstanceState))
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when(msg) {

            is Msg.Init -> {
                ret(model)
            }
            else -> {
                val (m,q) = update(msg, model.activity)
                ret(model.copy(activity=m), q)
            }

        }
    }


    fun update(msg: Msg, model: MActivity): Pair<MActivity, Que<Msg>> {
//        return ret(model)

        return when(msg) {

            is Msg.Init -> {
                ret(model)
            }
            is Msg.Fab.Clicked -> {
                Snackbar.make(msg.view, "Not configured", Snackbar.LENGTH_LONG)
                        .setAction("Goto", null).show()
                ret(model)
            }

            is Msg.Options-> {
                val (m,q) = update(msg, model.options)
                ret(model.copy(options= m), q)
            }
            is Msg.Action.GotOrig ->    ret(model)
        }


    }

    private fun update(msg: Msg.Options, model: MOptions): Pair<MOptions, Que<Msg>> {
        return when(msg) {

            is Msg.Options.ItemSelected -> {
                val item = msg.item
                // Handle action bar item clicks here. The action bar will
                // automatically handle clicks on the Home/Up button, so long
                // as you specify a parent activity in AndroidManifest.xml.
                val id = item.itemId


                val handled:Boolean = (id == R.id.action_settings)
                ret (model.copy(itemSelectedHandled = handled))
            }
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {

        }

        checkView(setup, model, pre) {
            view(model.activity, pre?.activity )
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = {
            me.setContentView(R.layout.activity_main_example4)
        }

        checkView(setup, model, pre) {
                view(model.fab, pre?.fab)
                view(model.toolbar, pre?.toolbar)
                view(model.pager, pre?.pager)
        }
    }


    private fun view(model: MViewPager, pre: MViewPager?){
        val setup = {

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            val mSectionsPagerAdapter = SectionsPagerAdapter(me.supportFragmentManager)

            // Set up the ViewPager with the sections adapter.
            val mViewPager = me.findViewById(R.id.container) as ViewPager
            mViewPager.adapter = mSectionsPagerAdapter

        }

        checkView(setup, model, pre) {
            //            view(model.activity, pre?.activity )
        }
    }


    private fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.findViewById(R.id.toolbar) as Toolbar
            me.setSupportActionBar(toolbar)

        }

        checkView(setup, model, pre) {
            //            view(model.activity, pre?.activity )
        }
    }

    private fun view(model: MFab, pre: MFab?) {
        val setup = {


            val fab = me.findViewById(R.id.fab) as FloatingActionButton
            fab.setOnClickListener { view -> dispatch(Msg.Fab.Clicked(view))}

        }

        checkView(setup, model, pre) {
                        view(model.snackbar, pre?.snackbar)
        }
    }

    private fun view(model: MSnackbar, pre: MSnackbar?) {
        val setup = {

        }

        checkView(setup, model, pre) {
            //            view(model.activity, pre?.activity )
        }
    }


//    val setup = {
//
//    }
//
//    checkView(setup, model, pre) {
////            view(model.activity, pre?.activity )
//    }
}






/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_main_activity_example4, container, false)
        val textView = rootView.findViewById(R.id.section_label) as TextView
        textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val args = Bundle()
            args.putInt(ARG_SECTION_NUMBER, sectionNumber)
            fragment.arguments = args
            return fragment
        }
    }
}

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return PlaceholderFragment.newInstance(position + 1)
    }

    override fun getCount(): Int {
        // Show 3 total pages.
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when (position) {
            0 -> return "SECTION 1"
            1 -> return "SECTION 2"
            2 -> return "SECTION 3"
        }
        return null
    }
}


