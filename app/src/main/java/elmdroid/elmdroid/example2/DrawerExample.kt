package elmdroid.elmdroid.example2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import elmdroid.elmdroid.R

class DrawerExample : AppCompatActivity() {

    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.start()
    }

    override fun onBackPressed() {
        if (DrawerOption.opened == app.model.activity.options.drawer.i) {
            app.dispatch(Msg.Option.Drawer(DrawerOption.closed))
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.example2, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        val id = item.itemId
        if (id == R.id.action_settings) {
            // you chose Settings.
            app.dispatch(Msg.Option.ItemSelected(ItemOption.settings))

            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
