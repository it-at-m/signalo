package de.muenchen.appcenter.signalo

import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import de.muenchen.appcenter.signalo.databinding.ActivityMainBinding
import de.muenchen.appcenter.signalo.utils.Constants
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var infoMenuItem: MenuItem? = null
    private var currentFragmentId = 0

    private val viewmodel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("App gestartet (on Create)")
        super.onCreate(savedInstanceState)
        this.binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(this.binding.root)
        initNavDrawer()
        initRefreshInfo()
        handleBackPressed()

        viewmodel.refreshState.observe(this) {
            updateRefreshCooldownUi()
        }

        viewmodel.onCellular.observe(this) {
            updateRefreshCooldownUi()
        }

        viewmodel.animatorProgress.observe(this) { progress ->
            this.binding.progressbar.progress = progress
        }
    }

    /**
     *hide or show refreshUI and info button accordingly to refreshstate
     * onCellular whole refreshUI gets hidden because only the wifi page needs refreshing
     */
    private fun updateRefreshCooldownUi() {
        Timber.d(

            "updateCooldownUi has been called with refreshState: " + viewmodel.refreshState.value
        )
        when (viewmodel.refreshState.value) {
            Constants.REFRESH_IDLE -> {
                this.binding.refreshLock.visibility = GONE
                this.binding.progressbar.visibility = GONE
                infoMenuItem?.isVisible = currentFragmentId == R.id.FirstFragment
                invalidateOptionsMenu()
            }

            Constants.REFRESH_ON_COOLDOWN -> {
                if (viewmodel.onCellular.value != true) {
                    this.binding.refreshLock.visibility = VISIBLE
                    this.binding.progressbar.visibility = VISIBLE
                    infoMenuItem?.isVisible = false
                } else {
                    Timber.d("Cooldown aktiv aber onCellular is true")
                    this.binding.refreshLock.visibility = GONE
                    this.binding.progressbar.visibility = GONE
                    infoMenuItem?.isVisible = currentFragmentId == R.id.FirstFragment
                    invalidateOptionsMenu()
                }
            }
        }
    }

    private fun initNavDrawer() {
        Timber.d("Navigation Drawer is setting up...")
        setSupportActionBar(this.binding.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navView: NavigationView = this.binding.navView
        val headerView = navView.getHeaderView(0)
        val versionText = getString(R.string.version, BuildConfig.VERSION_NAME)
        headerView.findViewById<TextView>(R.id.version_view).text = versionText
        val drawerLayout: DrawerLayout = this.binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.FirstFragment, R.id.OpenSourceBib, R.id.DatenschutzPage), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentFragmentId = destination.id
            invalidateOptionsMenu()

        }
    }

    private fun showMaterialDialog(title: String, message: String) {
        val formattedMessage = Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(formattedMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleBackPressed() {
        onBackPressedDispatcher.addCallback {
            val drawerLayout: DrawerLayout = this@MainActivity.binding.drawerLayout
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawers()
            } else {
                finishAndRemoveTask()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (currentFragmentId != R.id.FirstFragment) {
            infoMenuItem?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        Timber.d("Navigation Drawer got triggered")
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        infoMenuItem = menu.findItem(R.id.infoButton)
        if (viewmodel.refreshState.value == Constants.REFRESH_ON_COOLDOWN) {
            Timber.d("Info icon is not visible, because of running refresh")
            infoMenuItem?.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.infoButton -> {
                showMaterialDialog(
                    getString(R.string.title_infodialog_refresh),
                    getString(R.string.description_infodialog_refresh)
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initRefreshInfo() {
        this.binding.refreshLock.setOnClickListener {
            showMaterialDialog(
                getString(R.string.title_cooldown_info_button),
                getString(R.string.description_cooldown_info_button)
            )
        }
    }
}