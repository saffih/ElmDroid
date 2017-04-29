package elmdroid.elmdroid.example5

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.view.View
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBar
import android.support.v4.app.NavUtils
import android.view.MenuItem

import elmdroid.elmdroid.R
import elmdroid.elmdroid.example5.detail.ItemDetailElm
import elmdroid.elmdroid.example5.detail.Msg

/**
 * An activity representing a single Item detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [ItemListActivity].
 */
class ItemDetailActivity : AppCompatActivity() {
    private val app = ItemDetailElm(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.start(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, Intent(this, ItemListActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
