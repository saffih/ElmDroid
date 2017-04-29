package elmdroid.elmdroid.example4

import android.support.v7.app.AppCompatActivity

import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import elmdroid.elmdroid.R

class MainActivityExample4 : AppCompatActivity() {
    val app = TabbedElmApp(this)

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */

    /**
     * The [ViewPager] that will host the section contents.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.start(savedInstanceState)
//        setContentView(R.layout.activity_main_example4)
//
//        val toolbar = findViewById(R.id.toolbar) as Toolbar
//        setSupportActionBar(toolbar)
//        // Create the adapter that will return a fragment for each of the three
//        // primary sections of the activity.
//        val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
//        val mViewPager: ViewPager
//
//        // Set up the ViewPager with the sections adapter.
//        mViewPager = findViewById(R.id.container) as ViewPager
//        mViewPager.adapter = mSectionsPagerAdapter
//
//
//        val fab = findViewById(R.id.fab) as FloatingActionButton
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }

    }

    // happens once on creation
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main_activity_example4, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        app.dispatch(Msg.Options.ItemSelected(item))
        if (app.model.activity.options.itemSelectedHandled){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

//    /**
//     * A placeholder fragment containing a simple view.
//     */
//    class PlaceholderFragment : Fragment() {
//
//        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
//                                  savedInstanceState: Bundle?): View? {
//            val rootView = inflater!!.inflate(R.layout.fragment_main_activity_example4, container, false)
//            val textView = rootView.findViewById(R.id.section_label) as TextView
//            textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
//            return rootView
//        }
//
//        companion object {
//            /**
//             * The fragment argument representing the section number for this
//             * fragment.
//             */
//            private val ARG_SECTION_NUMBER = "section_number"
//
//            /**
//             * Returns a new instance of this fragment for the given section
//             * number.
//             */
//            fun newInstance(sectionNumber: Int): PlaceholderFragment {
//                val fragment = PlaceholderFragment()
//                val args = Bundle()
//                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
//                fragment.arguments = args
//                return fragment
//            }
//        }
//    }
//
//    /**
//     * A [FragmentPagerAdapter] that returns a fragment corresponding to
//     * one of the sections/tabs/pages.
//     */
//    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
//
//        override fun getItem(position: Int): Fragment {
//            // getItem is called to instantiate the fragment for the given page.
//            // Return a PlaceholderFragment (defined as a static inner class below).
//            return PlaceholderFragment.newInstance(position + 1)
//        }
//
//        override fun getCount(): Int {
//            // Show 3 total pages.
//            return 3
//        }
//
//        override fun getPageTitle(position: Int): CharSequence? {
//            when (position) {
//                0 -> return "SECTION 1"
//                1 -> return "SECTION 2"
//                2 -> return "SECTION 3"
//            }
//            return null
//        }
//    }
}