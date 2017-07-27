package elmdroid.elmdroid.example4

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import elmdroid.elmdroid.R
import kotlinx.android.synthetic.main.activity_tabbed.*
import saffih.elmdroid.ElmBase
import saffih.elmdroid.Que

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */

enum class ItemOption(val id: Int) {
    settings(R.id.action_settings);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}

sealed class  Msg {
    class Init : Msg()
    sealed class Fab: Msg() {
        class Clicked(val view: View) : Fab()
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

data class MOptions(val itemOption: MItemOption = MItemOption(false))
data class MViewPager(val i:Int = 0)
data class MToolbar(val i:Int = 0)
data class MFab(val snackbar:MSnackbar = MSnackbar())
data class MSnackbar(val i:Int=0)
data class MItemOption(val handled: Boolean = true, val item: ItemOption? = null)


// TOs transfer objects

class TabbedElmApp(override val me: TabbedActivity) : ElmBase<Model, Msg>(me){
    override fun init(): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
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
//        return ret(myModel)

        return when(msg) {

            is Msg.Init -> {
                ret(model)
            }
            is Msg.Fab.Clicked -> {
                Snackbar.make(msg.view, "Goto original studio generated activity", Snackbar.LENGTH_LONG)
                        .setAction("Goto", {
                            me.startActivity(
                                    Intent(me, TabbedActivityOrig::class.java))
                        }).show()
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
                val (m, c) = update(msg, model.itemOption)
                ret(model.copy(itemOption = m), c)
            }
        }
    }

    fun update(msg: Msg.Options.ItemSelected, model: MItemOption): Pair<MItemOption, Que<Msg>> {
        val selected = ItemOption.byId(msg.item.itemId)
        if (selected == null) {
            return ret(MItemOption(handled = false))
        } else {
            return ret(MItemOption(item = selected))
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
            val mViewPager = me.container
            mViewPager.adapter = mSectionsPagerAdapter

        }

        checkView(setup, model, pre) {
            //            view(myModel.activity, pre?.activity )
        }
    }


    private fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.toolbar
            me.setSupportActionBar(toolbar)

        }

        checkView(setup, model, pre) {
            //            view(myModel.activity, pre?.activity )
        }
    }

    private fun view(model: MFab, pre: MFab?) {
        val setup = {


            val fab = me.fab
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
            //            view(myModel.activity, pre?.activity )
        }
    }


//    val setup = {
//
//    }
//
//    checkView(setup, myModel, pre) {
////            view(myModel.activity, pre?.activity )
//    }
}






/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_tabbed_activity_example4, container, false)
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



