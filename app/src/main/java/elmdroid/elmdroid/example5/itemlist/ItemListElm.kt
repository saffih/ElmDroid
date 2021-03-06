package elmdroid.elmdroid.example5.itemlist

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import elmdroid.elmdroid.R
import elmdroid.elmdroid.example5.ItemDetailActivity
import elmdroid.elmdroid.example5.ItemDetailFragment
import kotlinx.android.synthetic.main.item_list.*
import saffih.elmdroid.ElmBase

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 28/04/17.
 */




class CDummyContent {

    private val COUNT = 25

    fun getDemo() = IntRange(1,COUNT).map { createDummyItem(it) }

    private fun createDummyItem(position: Int): DummyItem {
        return DummyItem(position.toString(), "Example 5: Elm Like - Item " + position, makeDetails(position))
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0..position - 1) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }

    /**
     * A dummy item representing a piece of content.
     */
    class DummyItem(val id: String, val content: String, val details: String) {

        override fun toString(): String {
            return content
        }
    }
}

typealias MItem = CDummyContent.DummyItem

sealed class Msg {
    object Init : Msg()

    sealed class Recycler: Msg(){
        class ItemHolderClicked(val position: Int, val v: View): Recycler()
    }
    sealed class Fab : Msg(){
        class Clicked(val v: ViewId?) : Fab()
    }

    sealed class Action: Msg(){
        class GotoOrig : Action()
    }
}


/**
 * Types used as in message properties
 */
class ViewId(val id:Int)
fun Int.viewId() = ViewId(this)
val View.viewId: ViewId get () = this.id.viewId()


/**
 * Model representing the state of the system
 * All Model types are Prefixed with M
 */
data class Model (val activity : MActivity = MActivity())
data class MActivity (
        val mTwoPane: Boolean = false,
        val toolbar : MToolbar = MToolbar(),
        val fab: MFab = MFab(),
        val items : List<MItem> = listOf()
    ){
        val byId by lazy { this.items.associateBy { it.id } }
}
data class MToolbar (val show : Boolean = false)
data class MFab (
        val clicked: ViewId?=null
)

class ItemListElm(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me) {
    override fun init() :Model{ dispatch(Msg.Init)
        return Model()}
    override fun update(msg: Msg, model: Model): Model {
        val activity=update(msg, model.activity)
            return model.copy(activity = activity)
    }

    private fun update(msg: Msg, model: MActivity): MActivity {
        return when ( msg ) {

            Msg.Init -> {
                val mTwoPane  = (me.findViewById(R.id.item_detail_container) != null)
                // The detail container view will be present only in the
                // large-screen layouts (res/values-w900dp).
                // If this view is present, then the
                // activity should be in two-pane mode.
                model.copy(mTwoPane = mTwoPane, items = CDummyContent().getDemo())
            }
            is Msg.Recycler -> {
                val m =
                        update(msg, model.items)
                 model.copy(items = m)
            }
            is Msg.Fab.Clicked -> {
                val m = update(msg, model.fab)
                 model.copy(fab = m)
            }
            is Msg.Action.GotoOrig ->      {
                me.startActivity(Intent(me,
                        elmdroid.elmdroid.example5orig.ItemListActivity::class.java))
                model
            }
        }
    }

    private fun  update(msg: Msg.Recycler, model: List<MItem>): List<MItem> {
        return when (msg) {
            is Msg.Recycler.ItemHolderClicked -> {
                val item = model.get(msg.position)

                if (this.myModel.activity.mTwoPane) {
                    val arguments = Bundle()
                    arguments.putString(ItemDetailFragment.ARG_ITEM_ID, item.id)//holder.mItem!!.id)
                    val fragment = ItemDetailFragment()
                    fragment.arguments = arguments
                    me.supportFragmentManager.beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit()
                } else {
                    val context = msg.v.context
                    val intent = Intent(context, ItemDetailActivity::class.java)
                    intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id) //.mItem!!.id)

                    context.startActivity(intent)
                }
                model
            }
        }
    }
    private fun  update(msg: Msg.Fab, model: MFab): MFab {
//        return myModel)
        return when (msg) {
            is Msg.Fab.Clicked -> model.copy(clicked = msg.v)
        }
    }



    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model, pre){
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {

        val setup = {
            me.setContentView(R.layout.activity_item_list)
        }
        checkView(setup, model, pre){
            view(model.fab, pre?.fab)
            view(model.toolbar, pre?.toolbar)
            view(model.items, pre?.items)
//            view(myModel.itemDetails, pre?.itemDetails)
        }
    }

//    private fun view(myModel: MItem?, pre: MItem?) {
//        val setup = {
//        }
//        checkView(setup, myModel, pre){
//        }
//
//    }

    private fun view(model: List<MItem>, pre: List<MItem>?) {
        val setup = {
            val recyclerView = me.item_list

            class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
                val mIdView: TextView
                val mContentView: TextView

                init {
                    mIdView = mView.findViewById(R.id.id) as TextView
                    mContentView = mView.findViewById(R.id.content) as TextView
                }

                override fun toString(): String {
                    return super.toString() + " '" + mContentView.text + "'"
                }
            }

            class Adapter : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_list_content, parent, false)
                    return ViewHolder(view)
                }

                /**
                 * Called by RecyclerView to display the data at the specified position. This method should
                 * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
                 * position.
                 */
                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val mActivity = this@ItemListElm.myModel.activity
                    val mItem = mActivity.items.get(position)

                    if (mItem!=model.get(position)){
                        throw RuntimeException() }

                    holder.mIdView.text = mItem.id // [position].id
                    holder.mContentView.text = mItem.content // mValues[position].content

                    holder.mView.setOnClickListener { v ->
                        dispatch(Msg.Recycler.ItemHolderClicked(position = position, v = v))
                    }
                }

                override fun getItemCount(): Int {
                    return model.size
                }
            }
            recyclerView.adapter = Adapter()

        }
        checkView(setup, model, pre){
            // dirty recreate
            setup()

            model.forEachIndexed { index, m ->
                val p = pre?.getOrNull(index)
                view(m, p)
            }
        }
    }

    private fun view(model: MItem, pre: MItem?) {
        val setup = {}
        checkView(setup, model, pre){
            //            view(myModel.activity, pre?.activity)
        }
    }

    private fun view(model: MToolbar, pre: MToolbar?) {

        val setup = {
            val toolbar = me.findViewById(R.id.toolbar) as Toolbar
            me.setSupportActionBar(toolbar)
            toolbar.title = me.title
        }
        checkView(setup, model, pre){
            //            view(myModel, pre)
        }
    }

    private fun view(model: MFab, pre: MFab?) {

        val setup = {
            val fab = me.findViewById(R.id.fab) as FloatingActionButton
            fab.setOnClickListener { view ->
                Snackbar.make(view, "Goto original studio generated activity", Snackbar.LENGTH_LONG)
                        .setAction("Goto", { dispatch(Msg.Action.GotoOrig()) }).show()
            }
        }
        checkView(setup, model, pre){
            //            view(myModel.snackbar, pre?.snackbar)
        }
    }


}
