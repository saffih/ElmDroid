package elmdroid.elmdroid.example4

import android.os.Bundle
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import elmdroid.elmdroid.R

class TabbedActivity : AppCompatActivity() {
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
        app.onCreate()

    }

    // happens once on creation
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main_activity_example4, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        app.dispatch(Msg.Options.ItemSelected(item))
        if (app.myModel.activity.options.itemOption.handled) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
