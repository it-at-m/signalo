package de.muenchen.appcenter.signalo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import de.muenchen.appcenter.signalo.databinding.ActivityMainBinding
import de.muenchen.appcenter.signalo.utils.Constants
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var currentFragmentId = 0

    private val viewmodel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("App gestartet (on Create)")
        super.onCreate(savedInstanceState)
        this.binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(this.binding.root)
        initNavDrawer()
        initButtons()
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
        /**
         * If location services are off, show LocationMissing Icon
         * If the user have not been asked this session, show user a dialog
         * if Location services are on, don't show icon
         */
        viewmodel.isLocationEnabled.observe(this) { enabled ->
            invalidateOptionsMenu()
            if (enabled == false && viewmodel.locationDialogShownThisSession.value != true) {
                openLocationDialog()
                viewmodel.locationDialogShownThisSession.postValue(true)
            }
        }
    }

    private fun isLockVisible() =
        viewmodel.refreshState.value == Constants.REFRESH_ON_COOLDOWN
                && viewmodel.onCellular.value != true

    /**
     * Shows a dialog informing the user that location services are disabled,
     * with an option to navigate directly to the system location settings.
     */
    private fun openLocationDialog() {
        val formattedMessage =
            Html.fromHtml(getString(R.string.location_services_missing), Html.FROM_HTML_MODE_LEGACY)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.location_missing_dialog_title))
            .setMessage(formattedMessage)
            .setNeutralButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.location_missing_dialog_settings)) { dialog, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                dialog.dismiss()
            }
            .show()
    }

    /**
     *hide or show refreshUI and info button accordingly to refreshstate
     * onCellular whole refreshUI gets hidden because only the wifi page needs refreshing
     */
    private fun updateRefreshCooldownUi() {
        val visibility = if (isLockVisible()) VISIBLE else GONE
        binding.refreshLock.visibility = visibility
        binding.progressbar.visibility = visibility
        invalidateOptionsMenu()
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
            setOf(R.id.FirstFragment, R.id.OpenSourceBib, R.id.DatenschutzPage, R.id.SnapshotList),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.externalSpeedtest) {
                drawerLayout.closeDrawer(GravityCompat.START)
                showExternalSpeedtestDialog()
                false
            } else {
                NavigationUI.onNavDestinationSelected(menuItem, navController)
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentFragmentId = destination.id
            invalidateOptionsMenu()

        }
    }

    private fun showExternalSpeedtestDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.speedtest_dialog_title))
            .setMessage(getString(R.string.speedtest_dialog_message))
            .setPositiveButton(getString(R.string.speedtest_dialog_positive_button)) { dialog, _ ->
                openSpeedtestBrowser()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.speedtest_dialog_negative_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openSpeedtestBrowser() {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            (getString(R.string.speedtest_URL)).toUri()
        )
        try {
            startActivity(urlIntent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                this.findViewById(android.R.id.content),
                getString(R.string.Speedtest_open_erorr),
                Snackbar.LENGTH_SHORT
            ).show()
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.infoButton).isVisible =
            currentFragmentId == R.id.FirstFragment && !isLockVisible()
        menu.findItem(R.id.locationMissing).isVisible =
            viewmodel.isLocationEnabled.value == false
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

            R.id.locationMissing -> {
                openLocationDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initButtons() {
        this.binding.refreshLock.setOnClickListener {
            showMaterialDialog(
                getString(R.string.title_cooldown_info_button),
                getString(R.string.description_cooldown_info_button)
            )
        }
    }
}