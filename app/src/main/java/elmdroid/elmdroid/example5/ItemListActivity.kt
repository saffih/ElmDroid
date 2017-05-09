package elmdroid.elmdroid.example5

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import elmdroid.elmdroid.example5.itemlist.ItemListElm


/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private val app = ItemListElm(this)

    // expose myModel the details.
    val model get () = app.myModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemListActivity=this

        app.start()
    }

    companion object {
        // share the myModel
        var itemListActivity:ItemListActivity? =null

    }

}
