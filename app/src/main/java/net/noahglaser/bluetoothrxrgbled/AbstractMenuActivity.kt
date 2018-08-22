package net.noahglaser.bluetoothrxrgbled

import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import io.vrinda.kotlinpermissions.PermissionsActivity

abstract class AbstractMenuActivity: PermissionsActivity() {

    /**
     * Event listener for when the Menu Option is clicked
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.bug_report -> {
                val browserIntent = Intent("android.intent.action.VIEW", Uri.parse("https://github.com/phptuts/Android-Bluetooth-RBG-LED/issues"))
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Boiler plate code that attaches the menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_layout, menu)
        return super.onCreateOptionsMenu(menu)
    }
}