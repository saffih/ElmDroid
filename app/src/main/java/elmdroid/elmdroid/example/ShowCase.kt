package elmdroid.elmdroid.example

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import elmdroid.elmdroid.R

class ShowCase : AppCompatActivity() {
    val app = ShowCaseElm(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.start(savedInstanceState)
    }


    override fun onBackPressed() {
        if (DrawerOption.opened == app.model.activity.options.drawer.i) {
            app.dispatch(Msg.Option.Drawer(DrawerOption.closed))
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.show_case, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        app.dispatch(Msg.Option.ItemSelected(item))
        if (app.model.activity.options.itemOption.handled) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }


}
