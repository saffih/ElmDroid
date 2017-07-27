package elmdroid.elmdroid.example5

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example5.itemlist.MItem
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListActivity]
 * in two-pane mode (on tablets) or a [ItemDetailActivity]
 * on handsets.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class ItemDetailFragment : Fragment() {

    private var mItem: MItem? = null

    /**
     * The dummy content this fragment is presenting.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments.containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            val key = arguments.getString(ItemDetailFragment.ARG_ITEM_ID)
            mItem = ItemListActivity.itemListActivity!!.model.activity.byId.get(key)

            val activity = this.activity as ItemDetailActivity
            val appBarLayout = activity.toolbar_layout
            appBarLayout.title = mItem!!.content
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.item_detail, container, false)

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            rootView.item_detail.text = mItem!!.details
        }

        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        val ARG_ITEM_ID = "item_id"
    }
}

