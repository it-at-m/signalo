package de.muenchen.appcenter.signalo

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_PHONE_STATE
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.wifi.WifiInfo
import android.net.wifi.WifiInfo.LINK_SPEED_UNKNOWN
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.SIM_STATE_READY
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.transition.TransitionManager
import com.ekn.gruzer.gaugelibrary.Range
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import de.muenchen.appcenter.signalo.databinding.FragmentMainBinding
import de.muenchen.appcenter.signalo.utils.Constants
import kotlinx.coroutines.Job
import timber.log.Timber
/**
 * This class handles most of the logic of the mainpage, fetching Networkstats, changing UI stuff...
 */
class MainFragment : Fragment() {
    private lateinit var _binding: FragmentMainBinding
    private val viewmodel: MainViewModel by activityViewModels()
    private var networkCallbackWifi: ConnectivityManager.NetworkCallback? = null
    private var generalNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var subscriptionManager: SubscriptionManager? = null
    private var cellularCallBack: TelephonyCallback? = null
    private var wifiScanReceiver: BroadcastReceiver? = null
    private var cellularTypeCallBack: TelephonyCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private var cellularType = "[int]"
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var userManager: UserManager
    private lateinit var wifiManager: WifiManager
    private var isRunning = false
    private var switchJob: Job? = null
    private var permissionRequestedThisSession = false
    private var mainSimName = ""
    private var mainSimSubId = -1
    private var secondSimName = ""
    private var secondSimSubId = -1
    private var mainSimMCCMNC = ""
    private var secondSimMCCMNC = ""
    private var oldDbmWifi: Double = 0.0
    private var oldDbmCellular: Double = 0.0

    @SuppressLint("MissingPermission")
    private var locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.entries.all { it.value }
        if (allPermissionsGranted) {
            Timber.d("allPermissionsGranted")
            readUsableSimCards()
            readCellIdAndBandFromTelephonyManagerAndSetInUi()
        } else {
            Timber.d("Berechtigungen wurden abgelehnt")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startWelcomeDialog()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        checkPermissions()
        initRefreshGesture()
        initToggleButtons()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initGauge()
        defineObserver()
        telephonyManager =
            requireContext().getSystemService(TelephonyManager::class.java)
        connectivityManager =
            requireContext().getSystemService(ConnectivityManager::class.java)
        wifiManager =
            requireContext().getSystemService(WifiManager::class.java)
        subscriptionManager = requireContext().getSystemService(SubscriptionManager::class.java)
        userManager = requireContext().getSystemService(UserManager::class.java)
        readUsableSimCards()
    }

    private fun initRefreshGesture() {
        _binding.swiperefresh.setProgressViewOffset(true, 0, 150)
        _binding.swiperefresh.setOnRefreshListener {
            manualWifiRefresh()
        }
    }

    /**starts a manual wifi scan to receive newest wifi DBM values
     * registers a broadcast receiver to look for scans from Android system but only if SCAN_PENDING
     * catches the resultlist and extract the dbm value for the Network with the current connected BSSID
     *
     */
    private fun manualWifiRefresh() {
        Timber.d("Wifi Refresh has been triggered")
        wifiScanReceiver = object : BroadcastReceiver() {

            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                if (viewmodel.refreshState.value == Constants.SCAN_PENDING) {
                    viewmodel.refreshState.value = Constants.SCAN_RECEIVED
                    val success =
                        intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    if (success) {
                        Timber.d("Scan completed successfully")
                        val resultList = wifiManager.scanResults
                        for (wifi in resultList) {
                            if (wifi.BSSID == viewmodel.connectedBSSID.value) {
                                viewmodel.setWifiDbmValue(wifi.level.toDouble())
                                Timber.d("Wifi scan dbm Value: %s", wifi.level.toString())
                                startRefreshCooldown()
                            }
                        }
                        unregisterWifiScanReceiver()
                    } else {
                        viewmodel.refreshState.value = Constants.SCAN_RECEIVED
                        Toast.makeText(
                            requireContext(),
                            "Wifi scan failed :(",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        requireContext().registerReceiver(wifiScanReceiver, intentFilter)
        Timber.d("Starting manual wifi scan...")
        wifiManager.startScan()
        viewmodel.refreshState.value = Constants.SCAN_PENDING
    }

    /**
     *
     *starts the "timer" for the Cooldown, posts animator values for progressBar in mainActivity
     * 30000ms (30sek) because the Android systems only allows 4 manual scans in 2 minutes
     */
    private fun startRefreshCooldown() {
        Timber.d("refreshCooldown has been called with state")
        if (!viewmodel.onCellular.value!!) {
            viewmodel.refreshState.value = Constants.REFRESH_ON_COOLDOWN
            //configure and start Animator
            val animator = ValueAnimator.ofInt(3000, 0)
            animator.interpolator = LinearInterpolator()
            animator.duration = 30000
            animator.addUpdateListener {
                viewmodel.animatorProgress.value = animator.animatedValue as Int
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    viewmodel.activeCooldown.value = false
                    viewmodel.refreshState.value = Constants.REFRESH_IDLE
                    Timber.d("Cooldown and Animator has been finished")
                }
            })
            animator.start()
            viewmodel.activeCooldown.value = true
            Timber.d("Cooldown and Animator has been started")
        }
    }

    /**
     *sets the refreshlayout on or off accordingly to the refreshStates
     */
    private fun updateSwipeRefreshUi() {
        Timber.d(
            "updateSwipeRefreshUi has been called with refreshState: " + viewmodel.refreshState.value
                    + " and onCellular: " + viewmodel.onCellular.value
        )
        if (viewmodel.onCellular.value != true) {
            when (viewmodel.refreshState.value) {
                Constants.REFRESH_IDLE -> {
                    _binding.swiperefresh.isEnabled = true

                }

                Constants.REFRESH_ON_COOLDOWN -> {
                    _binding.swiperefresh.isEnabled = false
                }

                Constants.SCAN_RECEIVED -> {
                    _binding.swiperefresh.isRefreshing = false
                }
            }
        }
        if (viewmodel.onCellular.value == true) {
            Timber.d("OnCellular refresh settings getting set")
            //if OnCellular
            _binding.swiperefresh.isRefreshing = false
            _binding.swiperefresh.isEnabled = false
            unregisterWifiScanReceiver()
        }
    }

    /**
     *fetches the current wifi dbm value
     * if Interger.Min_value is the dbm value, set all stats to unknown and call noNetwork function
     *filter the minimal INT value (-2.147483648E9) so it doesn't get displayed in UI
     */
    private fun readWifiDbmFromConnectivityManagerAndSetInUI() {
        _binding.toggleButtonGroup.check(R.id.btnWifi)
        //wifi daten abfragen und anzeigen
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        if (caps !== null) {
            val dbmWifi = caps.signalStrength.toDouble()
            if (dbmWifi != -2.147483648E9) {
                if (dbmWifi != oldDbmWifi) {
                    Timber.d("New Wifi DBM Value is: $dbmWifi")
                    viewmodel.setWifiDbmValue(dbmWifi)
                    oldDbmWifi = dbmWifi
                }
            } else {
                Timber.d("New Wifi DBM Value is ungültig: $dbmWifi")
                viewmodel.setCurrentLinkspeed("[unknown]")
                viewmodel.setCurrentFrequency("[unknown]")
                viewmodel.setCurrentSSID("[unknown]")
                viewmodel.setCurrentEncryptionType("[unknown]")
            }
        }
    }

    /**
     * registeres a callback if Signalstregth has changed
     * fetch the newest Cellular DBM values
     * if no value gets delivered set all stats to unknown and call noNetwork
     */
    private fun readCellularDbmFromTelephonyManagerAndSetInUI() {
        Timber.d("getCellulardbm is called")
        //Cellurlar dbm daten abfragen und anzeigen
        cellularCallBack =
            object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    if (signalStrength.cellSignalStrengths.isNotEmpty()) {
                        val dbmCellular = signalStrength.cellSignalStrengths[0].dbm.toDouble()
                        if (dbmCellular != oldDbmCellular) {
                            Timber.d(

                                "New Cellular DBM Value is: " + dbmCellular.toString()
                            )
                            viewmodel.setCellularDbmValue(dbmCellular)
                            oldDbmCellular = dbmCellular
                        }
                    } else {
                        noNetwork()
                        viewmodel.setCellId("[unknown]")
                        viewmodel.setCurrentCellularBand("[unknown]")
                        viewmodel.setcurrentNetProvider("[unknown]")
                        Timber.d("noNetwork is Called by cellular")

                    }
                }
            }

        cellularCallBack?.let { callback ->
            telephonyManager.registerTelephonyCallback(
                getMainExecutor(requireContext()),
                callback
            )
            Timber.d("CellularCallBack is registered")
        }
    }

    //start android permission request
    private fun getPermissions() {
        Timber.d("request permissions is called")
        val permissionsArray = arrayOf(
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        locationPermissionLauncher.launch(permissionsArray)
    }

    //only check if  permissions are present,if not present open up WelcomeDialog to ask the user again once in a session
    private fun checkPermissions() {
        if ((!hasSinglePermission(ACCESS_FINE_LOCATION) || !hasSinglePermission(READ_PHONE_STATE)) && !permissionRequestedThisSession) {
            permissionRequestedThisSession = true
            startWelcomeDialog(force = true)
        }
    }

    private fun hasSinglePermission(permission: String): Boolean {
        val granted = (ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED)
        Timber.d("$permission granted is $granted")
        return granted
    }

    /**
     *uses synchronous call to get cellinfo
     * because synchronous getallCellinfo get all radios if dual sim is used we have to match -
     * the selected sim with MNC and MCC to get the right values
     * fetch CellId to be displayed in the UI
     * fetch Cellular Frequency band, format it with helperfunktion and show it in UI
     */
    @RequiresPermission(ACCESS_FINE_LOCATION)
    private fun readCellIdAndBandFromTelephonyManagerAndSetInUi() {
        Timber.d("getCellID is Called")
        if ((hasSinglePermission(ACCESS_FINE_LOCATION)) && hasSinglePermission(READ_PHONE_STATE)) {
            fetchDualSimInfos()
            var cellId: Long = -1
            var formattedBand: String = "[unknown]"
            //demeter which MCCMNC to match to
            val targetMccMnc =
                if (_binding.btnSecondSim.isChecked) secondSimMCCMNC else mainSimMCCMNC
            val cellInfo = telephonyManager.allCellInfo
            if (cellInfo.isNullOrEmpty()) {
                viewmodel.setCurrentCellularBand("[unknown]")
                viewmodel.setCellId("[unknown]")
                Timber.d("getAllCellInfo is empty")
                return
            }
            for (info in cellInfo) {
                if (info.isRegistered && getMccMnc(info) == targetMccMnc) {
                    when (info) {
                        is CellInfoLte -> {
                            val prefix = "B"
                            val cellIdentity = info.cellIdentity
                            formattedBand =
                                formatCellBand(cellIdentity.bands, prefix)
                            Timber.d(

                                "Using LTE Band: $formattedBand"
                            )
                            cellId = cellIdentity.ci.toLong()
                            Timber.d("Using Cell: LTE")
                        }
                        // 5G
                        is CellInfoNr -> {
                            val cellIdentity = info.cellIdentity as CellIdentityNr
                            val prefix = "n"
                            formattedBand =
                                formatCellBand(cellIdentity.bands, prefix)
                            Timber.d("Using 5G Band: $formattedBand")
                            cellId = cellIdentity.nci
                            Timber.d("Using Cell: 5G")
                        }
                        // 3G
                        is CellInfoWcdma -> {
                            val cellIdentity = info.cellIdentity
                            cellId = cellIdentity.cid.toLong()
                            Timber.d("Using Cell: 3G")
                        }
                        //EDGE
                        is CellInfoGsm -> {
                            val cellIdentity = info.cellIdentity
                            cellId = cellIdentity.cid.toLong()
                        }
                    }
                    viewmodel.setCurrentCellularBand(formattedBand)
                    viewmodel.setCellId(cellId.toString())
                } else {
                    Timber.d("no CellID Match for: ${getMccMnc(info)}")
                }
            }

        } else {
            Timber.d("GetCellID wurde übersprungen - Berechtigung fehlt?")
            viewmodel.setCellId(getString(R.string.message_cellid_missing_permission))
            viewmodel.setCurrentCellularBand(getString(R.string.message_cellid_missing_permission))
        }
    }

    /**
     * helper fun of getcellID to get the MCCMNC of the current cellInfo
     */
    private fun getMccMnc(cellInfo: CellInfo): String? {
        Timber.d("getMccMnc called")

        val mccMnc = when (cellInfo) {
            is CellInfoLte -> {
                val id = cellInfo.cellIdentity
                "${id.mccString ?: ""}${id.mncString ?: ""}"
            }

            is CellInfoNr -> {
                val id = cellInfo.cellIdentity as CellIdentityNr
                "${id.mccString ?: ""}${id.mncString ?: ""}"
            }

            is CellInfoWcdma -> {
                val id = cellInfo.cellIdentity
                "${id.mccString ?: ""}${id.mncString ?: ""}"
            }

            is CellInfoGsm -> {
                val id = cellInfo.cellIdentity
                "${id.mccString ?: ""}${id.mncString ?: ""}"
            }

            else -> {
                Timber.d("getMccMnc Failed")
                return null
            }
        }
        if (mccMnc != "") {
            Timber.d("getMccMnc succsessful: $mccMnc")
            return mccMnc
        } else {
            Timber.d("getMccMnc Failed")
            return null
        }
    }

    /**
     * helper function for Cellular band to format it accordingly to 3GPP standard
     */
    private fun formatCellBand(cellBand: IntArray, praefix: String): String {
        val formattedCellBand = cellBand.joinToString(", ", transform = { praefix.plus(it) })
        return formattedCellBand
    }

    /**
     * fetch and demeter how many simcards are usable
     * if 2 Simcards are usable, call functions for dualsim
     * if none are useable deactivate cellular button and change to wifi
     */
    private fun readUsableSimCards() {
        Timber.d("readUsableSimCards is called")

        val simState0 = telephonyManager.getSimState(0)
        val simState1 = telephonyManager.getSimState(1)
        Timber.d("Simstate 1: $simState1 ")
        Timber.d("Simstate 0: $simState0 ")

        if (simState0 == SIM_STATE_READY && simState1 == SIM_STATE_READY) {
            //Dual Sim Mode
            Timber.d("Usable simcards detected: 2")
            viewmodel.simState.postValue("dual")
            fetchDualSimInfos()
            setDualSimUi(isDual = true)
        } else if (simState0 == SIM_STATE_READY && simState1 != SIM_STATE_READY) {
            Timber.d("Usable simcards detected: 1")
            viewmodel.simState.postValue("single")
            showSecondSimButton(false)
        } else if (simState0 != SIM_STATE_READY && simState1 != SIM_STATE_READY) {
            viewmodel.simState.postValue("none")
            switchNetworkType(Constants.WIFI)
            _binding.btnDefaultSim.isEnabled = false
            _binding.btnDefaultSim.text = "No Sim"
        }
    }

    /**
     * fetch main and secondary sim infos from sub...Manager and store them in class variable
     */
    @SuppressLint("MissingPermission")
    private fun fetchDualSimInfos() {
        if (hasSinglePermission(READ_PHONE_STATE)) {
            val simInfos = subscriptionManager?.activeSubscriptionInfoList ?: return
            for (sim in simInfos) {
                if (sim.subscriptionId == SubscriptionManager.getDefaultVoiceSubscriptionId()) {
                    mainSimName = sim.displayName.toString()
                    mainSimSubId = sim.subscriptionId
                    mainSimMCCMNC = sim.mccString + sim.mncString
                } else {
                    secondSimName = sim.displayName.toString()
                    secondSimSubId = sim.subscriptionId
                    secondSimMCCMNC = sim.mccString + sim.mncString
                }
            }
        } else {
            Timber.d("Permission READ PHONE STATE is missing")
        }
    }

    /**
     * change title of NetworkModeButtons accordingly to simcards
     * make secondSim button visible
     * reduce title textsize
     */
    private fun setDualSimUi(isDual: Boolean) {
        if (hasSinglePermission(READ_PHONE_STATE)) {
            Timber.d("setDualSimUi is Called")
            _binding.btnDefaultSim.text = getString(R.string.sim_slot1_displayname)
            _binding.btnSecondSim.text = getString(R.string.sim_slot2_displayname)
            _binding.btnWifi.textSize = 15.0F
            _binding.btnDefaultSim.textSize = 15.0F
            _binding.btnSecondSim.textSize = 15.0F
            showSecondSimButton(true)
        }
    }

    private fun showSecondSimButton(show: Boolean) {
        val toggleGroup = _binding.toggleButtonGroup
        val index = toggleGroup.indexOfChild(_binding.btnSecondSim)
        val secondSimButton = _binding.btnSecondSim
        if (show == true) {
            toggleGroup.removeView(secondSimButton)
            secondSimButton.visibility = VISIBLE
            toggleGroup.addView(secondSimButton, index)
        } else {
            toggleGroup.removeView(secondSimButton)
            secondSimButton.visibility = GONE
            toggleGroup.addView(secondSimButton, index)
        }
    }

    /**
     * @param subscriptionID of the simcard the telephonymanager should use
     * override the current Telephony manager to use the requested simcard for values
     */
    private fun overrideTelephonyManagerWithSim(subscriptionID: Int) {
        if (subscriptionID != -1) {
            Timber.d("TelephonyManager wird registiert auf SubID: $subscriptionID")
            telephonyManager = telephonyManager.createForSubscriptionId(subscriptionID)
        } else {
            Timber.d(

                "TelephonyManager wird nicht umgeschrieben da SubID= $subscriptionID"
            )
        }
    }

    /** registers a Display info Listener
     * sets value real5G= Yes:  Standalone, no: Non Standalone
     * calls a fun to set icons acordingly to the networktype
     */
    private fun fetchCellularType() {
        Timber.d("getCellularType is Called")
        cellularTypeCallBack =
            object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                    Timber.d("onDisplayInfoChanged Called")
                    cellularType = when (telephonyDisplayInfo.networkType) {
                        TelephonyManager.NETWORK_TYPE_GSM -> Constants.NETWORKTYPE_2G
                        TelephonyManager.NETWORK_TYPE_GPRS -> Constants.NETWORKTYPE_2G
                        TelephonyManager.NETWORK_TYPE_EDGE -> Constants.NETWORKTYPE_EDGE
                        TelephonyManager.NETWORK_TYPE_UMTS -> Constants.NETWORKTYPE_3G
                        TelephonyManager.NETWORK_TYPE_HSDPA -> Constants.NETWORKTYPE_3G
                        TelephonyManager.NETWORK_TYPE_HSUPA -> Constants.NETWORKTYPE_3G
                        TelephonyManager.NETWORK_TYPE_HSPA -> Constants.NETWORKTYPE_3G
                        TelephonyManager.NETWORK_TYPE_HSPAP -> Constants.NETWORKTYPE_3G_PLUS// (krasseres 3G)
                        TelephonyManager.NETWORK_TYPE_LTE -> Constants.NETWORKTYPE_LTE
                        TelephonyManager.NETWORK_TYPE_NR -> Constants.NETWORKTYPE_5G_SA
                        else -> "[unknown]"
                    }
                    when (telephonyDisplayInfo.overrideNetworkType) {
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ->
                            cellularType = Constants.NETWORKTYPE_5G_NSA

                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> cellularType =
                            Constants.NETWORKTYPE_4G_PLUS

                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> cellularType =
                            Constants.NETWORKTYPE_4G_PLUS

                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> cellularType =
                            Constants.NETWORKTYPE_5G_PLUS

                        else -> {}
                    }
                    viewmodel.setCellularType(cellularType)
                    setCellularTypeIcons(cellularType)
                }
            }
        cellularTypeCallBack?.let { callback ->
            telephonyManager.registerTelephonyCallback(
                getMainExecutor(requireContext()),
                callback
            )
            Timber.d("cellularTypeCallBack wurde registriert")
        }
    }

    /**sets the icons in the UI accordingly to the cellular type
     * @param cellularType the value containing the current cellularType as a String,
     */
    private fun setCellularTypeIcons(cellularType: String) {
        when (cellularType) {
            Constants.NETWORKTYPE_5G_PLUS -> _binding.imageViewNetType.setImageResource(R.drawable._g_24px)
            Constants.NETWORKTYPE_5G_NSA -> _binding.imageViewNetType.setImageResource(R.drawable._g_24px)
            Constants.NETWORKTYPE_5G_SA -> _binding.imageViewNetType.setImageResource(R.drawable._g_24px)
            Constants.NETWORKTYPE_4G_PLUS -> _binding.imageViewNetType.setImageResource(R.drawable._g_plus_mobiledata_24px)
            Constants.NETWORKTYPE_LTE -> _binding.imageViewNetType.setImageResource(R.drawable.lte_mobiledata_24px)
            Constants.NETWORKTYPE_3G_PLUS -> _binding.imageViewNetType.setImageResource(R.drawable._g_mobiledata_24px)
            Constants.NETWORKTYPE_3G -> _binding.imageViewNetType.setImageResource(R.drawable._g_mobiledata_24px)
            Constants.NETWORKTYPE_EDGE -> _binding.imageViewNetType.setImageResource(R.drawable.e_mobiledata_24px)
            Constants.NETWORKTYPE_2G -> _binding.imageViewNetType.setImageResource(R.drawable.e_mobiledata_24px)
            Constants.NETWORKTYPE_UNKNOWN -> _binding.imageViewNetType.setImageResource(R.drawable.help_center_24px)
            else -> _binding.imageViewNetType.setImageResource(R.drawable.help_center_24px)
        }
    }

    /**
     * fetch networkOperator from telephonymanager
     * this is the mnc+mcc value delivered by the current connected celltower
     * call setProviderIcons with this param
     * if networkOperator is empty, call setProviderIcons with 0 to set a fallback ? icon
     */
    private fun fetchCellularProviderCode() {
        if (!telephonyManager.networkOperator.isNullOrBlank()) {
            val currentProviderCode = telephonyManager.networkOperator.toInt()
            setProviderIcons(currentProviderCode)
        } else {
            Timber.d("Cellular Provider Code is null or Empty, using ? Icon")
            setProviderIcons(0)
        }
    }

    /**
     *map the providercode(currentProviderCode) to the suitable logos
     * displays the logos in the UI
     * @param currentProviderCode Int value fetched by fetchCellularProvider
     */
    private fun setProviderIcons(currentProviderCode: Int): Int? {
        val providerIcons = mapOf(
            //telekom
            26201 to R.drawable.deutsche_telekom_2022_svg,
            26206 to R.drawable.deutsche_telekom_2022_svg,
            //Vodafone
            26202 to R.drawable.vodafone_kabel_deutschland_logo_vector,
            26204 to R.drawable.vodafone_kabel_deutschland_logo_vector,
            26209 to R.drawable.vodafone_kabel_deutschland_logo_vector,
            //O2
            26203 to R.drawable.o2_svg,
            26205 to R.drawable.o2_svg,
            26207 to R.drawable.o2_svg,
            26208 to R.drawable.o2_svg,
            26211 to R.drawable.o2_svg,
            //1&1
            26223 to R.drawable.__1_logo,
            //Österreich
            //A1
            23201 to R.drawable.logo_of_a1,
            23202 to R.drawable.logo_of_a1,
            23209 to R.drawable.logo_of_a1,

            //Magenta (telekom Österreich)
            23203 to R.drawable.deutsche_telekom_2022_svg,
            23204 to R.drawable.deutsche_telekom_2022_svg,
            23207 to R.drawable.deutsche_telekom_2022_svg,
            //Schweiz
            //swiss com
            22801 to R.drawable.scmn_sw_38a30a24,
            //sunrise Commnunications
            22802 to R.drawable.sunrise_2022_svg,
            //Salt Mobile (Orange)
            22803 to R.drawable.icones_logosalt_black,

            )
        requireActivity().runOnUiThread {
            _binding.imageViewCellularProvider.setImageResource(
                providerIcons[currentProviderCode] ?: R.drawable.help_center_24px
            )
        }
        return providerIcons[currentProviderCode]
    }

    /**
     * fetches OperatorName from telephonymanager and Sets in UI
     * if its null or empty call a fallback fun
     */
    private fun fetchCellularProviderName() {
        val currentProviderName = telephonyManager.networkOperatorName
        Timber.d("NetworkOperatorname: " + currentProviderName)
        if (!currentProviderName.isNullOrBlank()) {
            viewmodel.setcurrentNetProvider(currentProviderName)
        } else {
            Timber.d("Cellular Provider/Operator Name is null or Blank, using fallback fun")
            fetchFallbackCellularProvidername()
        }
    }

    /**
     * calls setProviderIcons to demeter the provider name based on the selected logo (which is based on the MNC and MCC code)
     * if no logo was selected use fallback value "unknown Provider"
     */
    private fun fetchFallbackCellularProvidername() {
        var selectedIcon = 0
        if (!telephonyManager.networkOperator.isNullOrBlank()) {
            selectedIcon = setProviderIcons(telephonyManager.networkOperator.toInt()) ?: 0
        }
        when (selectedIcon) {
            R.drawable.deutsche_telekom_2022_svg -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_TELEKOM)
            }

            R.drawable.vodafone_kabel_deutschland_logo_vector -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_VODAFONE)
            }

            R.drawable.o2_svg -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_O2)
            }

            R.drawable.__1_logo -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_1UND1)
            }

            R.drawable.logo_of_a1 -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_A1)
            }

            R.drawable.scmn_sw_38a30a24 -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_SWISSCOM)
            }

            R.drawable.sunrise_2022_svg -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_SUNRISE)
            }

            R.drawable.icones_logosalt_black -> {
                viewmodel.setcurrentNetProvider(Constants.PROVIDER_SALT)
            }

            else -> {
                viewmodel.setcurrentNetProvider("Unknown Provider")
            }
        }
    }


    /**
     * a function to combine all cellular data gatherings except for the dbm value
     * registeres a onCapChanged callback for functions who dont have an own callback, so they are getting refreshed when anything network related changes
     */
    private fun fetchAllCellularData() {
        Timber.d("getAllCellularData is called")
        fetchCellularType()
        generalNetworkCallback =
            object : ConnectivityManager.NetworkCallback() {
                @RequiresPermission(ACCESS_FINE_LOCATION)
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    fetchCellularProviderCode()
                    fetchCellularProviderName()
                    readCellIdAndBandFromTelephonyManagerAndSetInUi()
                }
            }
        connectivityManager.registerDefaultNetworkCallback(generalNetworkCallback!!)
        Timber.d("General generalNetworkCallback is registered")
    }

    /**
     * a function to combine all wifi data gatherings, except for the dbm value
     * registeres a onCapChanged callback so the values are getting refreshed when something changes
     * if wifi is turned off in the settings set all values to unknown and call noNetwork
     */
    private fun fetchAllWifiData() {
        Timber.d("getAllWifiData is called")
        networkCallbackWifi =
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onLost(network: Network) {
                    super.onLost(network)
                    noNetwork()
                    viewmodel.setCurrentLinkspeed("[unknown]")
                    viewmodel.setCurrentFrequency("[unknown]")
                    viewmodel.setCurrentSSID("[unknown]")
                    viewmodel.setCurrentEncryptionType("[unknown]")
                    viewmodel.setWifiDbmValue(Constants.GAUGE_WIFI_MIN)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    //when caps changed, only continue if wifi is enabled, else call noNetwork and set values to unknown
                    if (!wifiManager.isWifiEnabled) {
                        Timber.d("Wifi wurde überprüft und ist Ausgeschaltet!")
                        noNetwork()
                        viewmodel.setCurrentLinkspeed("[unknown]")
                        viewmodel.setCurrentFrequency("[unknown]")
                        viewmodel.setCurrentSSID("[unknown]")
                        viewmodel.setCurrentEncryptionType("[unknown]")
                    } else {
                        Timber.d("Wifi wurde überprüft und ist Angeschaltet!")
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        if (!networkCapabilities.hasTransport(TRANSPORT_WIFI)) return
                        val wifiInfo = networkCapabilities.transportInfo as? WifiInfo ?: return
                        fetchSSID(wifiInfo)
                        fetchFrequency(wifiInfo)
                        fetchLinkspeed(wifiInfo)
                        fetchEncryptionType(wifiInfo)
                    }
                }
            }
        connectivityManager.registerDefaultNetworkCallback(networkCallbackWifi!!)
        Timber.d("networkCallback2 is registered")
    }

    /**
     * @param wifiInfo the object received by wifimanager(networkCapabilities.transportInfo)
     * if ACCESS_FINE_LOCATION is granted
     * fetch SSID from wifiInfo, filter the output and show in UI
     * else show
     */
    fun fetchSSID(wifiInfo: WifiInfo) {
        if (hasSinglePermission(ACCESS_FINE_LOCATION)) {
            Timber.d(wifiInfo.ssid)
            viewmodel.connectedBSSID.postValue(wifiInfo.bssid)
            viewmodel.setCurrentSSID(wifiInfo.ssid.replace("\"", ""))
        } else {
            viewmodel.setCurrentSSID("(Berechtigung fehlt)")
        }
    }

    /**
     * fetch current frequency and display in UI
     * @param wifiInfo the object received by wifimanager(networkCapabilities.transportInfo)
     */
    private fun fetchFrequency(wifiInfo: WifiInfo) {
        Timber.d("Raw Wifi Frequency" + wifiInfo.frequency.toString())
        val rawFrequency = wifiInfo.frequency
        when (rawFrequency) {
            in 2400..2500 -> {
                viewmodel.setCurrentFrequency("2,4 GHz")
            }

            in 5150..5850 -> {
                viewmodel.setCurrentFrequency("5 GHz")
            }

            in 5925..7125 -> {
                viewmodel.setCurrentFrequency("6 GHz")
            }

            else -> {
                viewmodel.setCurrentFrequency("[unknown]")
            }

        }
    }

    /**
     * fetch current Linkspeed and display in UI
     * @param wifiInfo the object received by wifimanager(networkCapabilities.transportInfo)
     */
    private fun fetchLinkspeed(wifiInfo: WifiInfo) {
        Timber.d("Linkspeed is: " + wifiInfo.linkSpeed)
        val rawLinkspeed = wifiInfo.linkSpeed
        val suffix = " Mbps"
        val formattedLinkspeed: String = ("$rawLinkspeed$suffix")
        viewmodel.setCurrentLinkspeed(formattedLinkspeed)
        Timber.d(wifiInfo.currentSecurityType.toString())
        if (rawLinkspeed == LINK_SPEED_UNKNOWN) {
            viewmodel.setCurrentLinkspeed("[unknown]")
        }
    }

    /**
     * fetch current EncryptionType and display in UI
     * @param wifiInfo the object received by wifimanager(networkCapabilities.transportInfo)
     */
    private fun fetchEncryptionType(wifiInfo: WifiInfo) {
        val encryptionType = when (wifiInfo.currentSecurityType) {
            WifiInfo.SECURITY_TYPE_OPEN -> "None"
            WifiInfo.SECURITY_TYPE_WEP -> "WEP"
            WifiInfo.SECURITY_TYPE_PSK -> "WPA/WPA2"
            WifiInfo.SECURITY_TYPE_EAP -> "WPA2-Enterprise"
            WifiInfo.SECURITY_TYPE_SAE -> "WPA3"
            WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> "WPA3-Enterprise +"
            WifiInfo.SECURITY_TYPE_OWE -> "Enhanced Open"
            WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE -> "WPA3-Enterprise"
            else -> "[unknown]"
        }
        viewmodel.setCurrentEncryptionType(encryptionType)
    }

    /**
     * should be called when the current network is not connected
     * sets dbm value to the minimum on all 2 Gauges
     * sends Snackbar to user to inform them about the missing network
     */
    private fun noNetwork() {
        Timber.d("no Network is Called")
        viewmodel.setWifiDbmValue(Constants.GAUGE_WIFI_MIN)
        viewmodel.setCellularDbmValue(Constants.GAUGE_CELLULAR_MIN)
        Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            getString(R.string.message_network_disconnected),
            Snackbar.LENGTH_SHORT
        ).show()

    }

    /**Initializes and configures the Wi-Fi and cellular signal gauges,
     * including value ranges, colors, min/max limits, and dBm formatting.
     * calls helper function createRange which returnes the Range Object
     */
    private fun initGauge() {
        val cellularGauge = _binding.cellularGauge
        val wifiGauge = _binding.wifiGauge

        val range = Range()
        val range2 = Range()
        val range3 = Range()
        //Wifi
        wifiGauge.addRange(createRange(-100.0, -81.0, Constants.GAUGE_RANGE1_COLOR.toColorInt()))
        wifiGauge.addRange(createRange(-80.0, -68.0, Constants.GAUGE_RANGE2_COLOR.toColorInt()))
        wifiGauge.addRange(createRange(-67.0, 30.0, Constants.GAUGE_RANGE3_COLOR.toColorInt()))
        wifiGauge.addRange(range)
        wifiGauge.addRange(range2)
        wifiGauge.addRange(range3)
        wifiGauge.minValue = Constants.GAUGE_WIFI_MIN
        wifiGauge.maxValue = Constants.GAUGE_WIFI_MAX
        wifiGauge.setValueColorAttr(android.R.attr.textColorPrimary)
        wifiGauge.setFormatter { value ->
            "${value.toInt()} dBm"
        }
        //Cellular
        cellularGauge.addRange(
            createRange(
                Constants.GAUGE_CELLULAR_MIN,
                -100.0, Constants.GAUGE_RANGE1_COLOR.toColorInt()
            )
        )
        cellularGauge.addRange(createRange(-99.0, -80.0, Constants.GAUGE_RANGE2_COLOR.toColorInt()))
        cellularGauge.addRange(
            createRange(
                -79.0,
                Constants.GAUGE_CELLULAR_MAX,
                Constants.GAUGE_RANGE3_COLOR.toColorInt()
            )
        )
        cellularGauge.addRange(range)
        cellularGauge.addRange(range2)
        cellularGauge.addRange(range3)
        cellularGauge.minValue = Constants.GAUGE_CELLULAR_MIN
        cellularGauge.maxValue = Constants.GAUGE_CELLULAR_MAX
        cellularGauge.setValueColorAttr(android.R.attr.textColorPrimary)
        cellularGauge.setFormatter { value ->
            "${value.toInt()} dBm"
        }
    }

    /**
     * helper fun for initGauge,
     * Creates the Object with the following parameters
     * @param from starting point of the Range
     * @param to ending point of the Range
     * @param color the color in hex which the Range should have
     * Returns the range object
     */
    private fun createRange(from: Double, to: Double, color: Int): Range {
        val range = Range()
        range.from = from
        range.to = to
        range.color = color
        return range
    }

    /**
     * initialize NetworkTypeButtons
     * SetOnClicked listener to automatically call switchNetworkType when it clicked
     * classic if conditions to also call switchNetworkType when app is started and one button is checked (please dont delete these xD)
     *
     */
    private fun initToggleButtons() {
        _binding.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnWifi -> {
                        switchNetworkType(Constants.WIFI)
                    }

                    R.id.btnDefaultSim -> {
                        switchNetworkType(Constants.CELLULAR)
                    }

                    R.id.btnSecondSim -> {
                        switchNetworkType(Constants.SECOND_SIM)
                    }
                }
            }

        }
        if (_binding.btnWifi.isChecked) {
            switchNetworkType(Constants.WIFI)
        }
        if (_binding.btnDefaultSim.isChecked) {
            switchNetworkType(Constants.CELLULAR)
        }
        if (_binding.btnSecondSim.isChecked) {
            switchNetworkType(Constants.SECOND_SIM)
        }
        readUsableSimCards()
    }

    /**
     * @param button the target button to animate to
     * define animations to use for UI changes for SwitchNetworkType
     * start animations and UI change
     */
    private fun uiSwitchAnimation(button: String) {
        val forwardTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        val backwardTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        forwardTransition.duration = Constants.SWITCH_ANIMATION_MS.toLong()
        backwardTransition.duration = Constants.SWITCH_ANIMATION_MS.toLong()
        when (button) {
            Constants.WIFI -> {
                TransitionManager.beginDelayedTransition(
                    _binding.layout1,
                    forwardTransition
                )
                Timber.d("Cellular gauge off")
                Timber.d("Wifi gauge on")

                _binding.cellularGauge.visibility = GONE
                _binding.wifiGauge.visibility = VISIBLE

                _binding.cellularContainer.visibility = GONE
                _binding.wifiContainer.visibility = VISIBLE
            }

            Constants.CELLULAR -> {
                TransitionManager.beginDelayedTransition(
                    _binding.layout1,
                    backwardTransition
                )
                Timber.d("Wifi gauge off")
                Timber.d("Cellular gauge on")

                _binding.wifiGauge.visibility = GONE
                _binding.cellularGauge.visibility = VISIBLE
                _binding.wifiContainer.visibility = GONE
                _binding.cellularContainer.visibility = VISIBLE
            }

            Constants.SECOND_SIM -> {
                TransitionManager.beginDelayedTransition(
                    _binding.layout1,
                    backwardTransition
                )
                _binding.wifiGauge.visibility = GONE
                _binding.cellularGauge.visibility = VISIBLE
                _binding.wifiContainer.visibility = GONE
                _binding.cellularContainer.visibility = VISIBLE
            }
        }
    }

    /**
     * Switches the app's monitoring mode between WiFi and Cellular.
     *
     * 1. Calls resetNetworkTypeCallbacks to delete old callbacks
     * 2. Resets cached variables
     * 3. Updates state variables
     * 4. Calls overrideTelephonyManagerWithSim to set telephony manager on the requested sim
     * 4. Fetches fresh data for the new mode
     * 5. Calls uiSwitchAnimation to trigger UI transition animation
     * @param button The target network mode (Constants.WIFI or Constants.CELLULAR)
     */
    private fun switchNetworkType(button: String) {
        when (button) {
            Constants.WIFI -> {
                Timber.d("Wifi Knopf wurde gedrückt")
                resetNetworkTypeCallbacks()
                oldDbmCellular = 0.0
                viewmodel.onCellular.value = false
                viewmodel.onWifi.value = true
                startTimer()
                fetchAllWifiData()
                uiSwitchAnimation(button)
            }

            Constants.CELLULAR -> {
                Timber.d("Cellular Knopf wurde gedrückt")
                resetNetworkTypeCallbacks()
                oldDbmWifi = 0.0
                viewmodel.onWifi.value = false
                viewmodel.onCellular.value = true
                overrideTelephonyManagerWithSim(mainSimSubId)
                readCellularDbmFromTelephonyManagerAndSetInUI()
                fetchAllCellularData()
                uiSwitchAnimation(button)
            }

            Constants.SECOND_SIM -> {
                Timber.d("Second Sim Knopf wurde gedrückt")
                resetNetworkTypeCallbacks()
                oldDbmWifi = 0.0
                viewmodel.onWifi.value = false
                viewmodel.onCellular.value = true
                overrideTelephonyManagerWithSim(secondSimSubId)
                readCellularDbmFromTelephonyManagerAndSetInUI()
                fetchAllCellularData()
                uiSwitchAnimation(button)
            }
        }
    }

    /**
     * unregister and stop every callback and timer
     */
    private fun resetNetworkTypeCallbacks() {
        unregisterCellularType()
        unregisterCellular()
        unregisterGeneralNetworkCallback()
        unregisterNetworkCallbackWifi()
        unregisterWifiScanReceiver()
        stopTimer()
    }

    /** if no entry in sharedPreferences was found or force is True:
     * Starts a Dialog fragment with a Welcome screen which explains why the app needs the permissions
     *  registers ResultListener to know if the user want to grant permissions or not,
     *  if yes getPermissions is called
     */
    private fun startWelcomeDialog(force: Boolean = false) {
        var firstOpen: Boolean? = null
        val prefs = requireContext().getSharedPreferences("mySettings", Context.MODE_PRIVATE)
        firstOpen = prefs.getBoolean("firstOpen", true)
        if (firstOpen || force) {
            prefs.edit() { putBoolean("firstOpen", false) }
            Timber.d("firstOpen is True")
            permissionRequestedThisSession = true
            val dialog = WelcomeDialogFragment()
            dialog.isCancelable = false
            dialog.show(getParentFragmentManager(), "welcomeDialog")
            prefs.edit() {
                setFragmentResultListener(Constants.DIALOG_REQUEST_KEY) { _, bundle ->
                    val buttonClicked = bundle.getBoolean("start_clicked", false)
                    if (buttonClicked) {
                        getPermissions()
                    }
                }
            }
        } else {
            Timber.d("firstOpen & force are false")
        }
    }

    /**
     * starts a timer to regularly call for the newest WIFI dbm values
     */
    private fun startTimer() {
        Timber.d("Timer Wifi Started")
        if (!isRunning) {
            isRunning = true
            handler.post(timerRunnable)
        }
    }

    private fun stopTimer() {
        Timber.d("Timer Wifi stopped")

        isRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            // Schedule the next run after 1 second
            readWifiDbmFromConnectivityManagerAndSetInUI()
            handler.postDelayed(this, 700)
        }
    }

    //all observers for livedata are getting defined here
    private fun defineObserver() {
        //Define Observer
//when refresh state has changed, update UI Accordingly
        viewmodel.refreshState.observe(viewLifecycleOwner) {
            updateSwipeRefreshUi()
        }
        viewmodel.onCellular.observe(viewLifecycleOwner) {
            updateSwipeRefreshUi()
        }
        viewmodel.wifiDbmValue.observe(viewLifecycleOwner) { dbmwifiValue ->
            if (dbmwifiValue != Int.MIN_VALUE.toDouble()) {
                _binding.wifiGauge.value = dbmwifiValue.toDouble()
            }
        }
        viewmodel.cellularDbmValue.observe(viewLifecycleOwner) { dbmcellularValue ->
            if (dbmcellularValue != Int.MIN_VALUE.toDouble()) {
                _binding.cellularGauge.value = dbmcellularValue.toDouble()
            }
        }
        viewmodel.currentCellularBand.observe(viewLifecycleOwner) { band ->
            val textFeld = getString(R.string.card_value3, band)
            _binding.textViewCellularIpValue.text = textFeld
        }
        viewmodel.cellularType.observe(viewLifecycleOwner) { cellularValue ->
            Timber.d("Der Cellular Network Type ist: " + cellularValue)
            val textFeld = getString(R.string.card_value2, cellularValue.toString())
            _binding.textViewNetTypeValue.text = textFeld
        }
        viewmodel.cellId.observe(viewLifecycleOwner) { cellIdValue ->
//            Timber.d("Der Cellular Network Type ist: " + cellularValue)
            val textFeld = getString(R.string.card_value4, cellIdValue.toString())
            _binding.textViewCellIdValue.text = textFeld
        }
        viewmodel.currentNetprovider.observe(viewLifecycleOwner) { netProviderValue ->
            Timber.d("Der aktuelle Provider des Netzwerks ist: " + netProviderValue)
            val textFeld = getString(R.string.card_value1, netProviderValue)
            _binding.textViewCellularProviderValue.text = textFeld
        }
        viewmodel.currentSSID.observe(viewLifecycleOwner) { currentSSIDValue ->
            Timber.d("Das akutell verbundene Wlan hat SSID: " + currentSSIDValue)
            val textFeld = getString(R.string.wifi_value_SSID, currentSSIDValue)
            _binding.textViewSSIDValue.text = textFeld
        }
        viewmodel.currentFrequency.observe(viewLifecycleOwner) { currentFrequencyValue ->
            Timber.d(

                "Das akutell verbundene Wlan hat Frequenz: " + currentFrequencyValue
            )
            val textFeld = getString(R.string.wifi_value_SSID, currentFrequencyValue)
            _binding.textViewFreqValue.text = textFeld
        }
        viewmodel.currentLinkspeed.observe(viewLifecycleOwner) { currentLinkspeedValue ->
            val textFeld = getString(R.string.wifi_value_linkspeed, currentLinkspeedValue)
            _binding.textViewLinkspeedValue.text = textFeld
        }
        viewmodel.currentEncryptionType.observe(viewLifecycleOwner) { currentEncryptionTypeValue ->
            val textFeld =
                getString(R.string.wifi_value_encryptionType, currentEncryptionTypeValue)
            _binding.textViewEncryptionTypeValue.text = textFeld
        }
    }

    private fun unregisterWifiScanReceiver() {
        if (wifiScanReceiver != null) {
            requireContext().unregisterReceiver(wifiScanReceiver)
            Timber.d("wifiScanReceiver is unregistered")
            wifiScanReceiver = null
        }
        if (viewmodel.refreshState.value == Constants.SCAN_PENDING) {
            viewmodel.refreshState.postValue(Constants.REFRESH_IDLE)
        }
    }

    private fun unregisterCellular() {
        cellularCallBack?.let { callback ->
            telephonyManager.unregisterTelephonyCallback(callback)
            cellularCallBack = null
            Timber.d("CellularCallBack is unregistered")

        }
    }

    private fun unregisterCellularType() {
        cellularTypeCallBack?.let { callback ->
            telephonyManager.unregisterTelephonyCallback(callback)
            cellularTypeCallBack = null
            Timber.d("cellularTypeCallBack is unregistered")
        }
    }

    private fun unregisterNetworkCallbackWifi() {
        networkCallbackWifi?.let { callback ->
            connectivityManager.unregisterNetworkCallback(callback)
            networkCallbackWifi = null
            Timber.d("networkCallbackWifi is unregistered")
        }
    }

    private fun unregisterGeneralNetworkCallback() {
        generalNetworkCallback?.let { callback ->
            connectivityManager.unregisterNetworkCallback(callback)
            generalNetworkCallback = null
            Timber.d("GenerealNetworkCallback is unregistered")
        }
    }

    override fun onPause() {
        Timber.d("onPause is called")
        super.onPause()
        stopTimer()
        switchJob?.cancel()
        oldDbmWifi = 0.0
        oldDbmCellular = 0.0
        unregisterCellular()
        unregisterWifiScanReceiver()
        unregisterCellularType()
        unregisterGeneralNetworkCallback()
        unregisterNetworkCallbackWifi()
        _binding.toggleButtonGroup.clearOnButtonCheckedListeners()
    }

    override fun onDestroyView() {
        Timber.d("onDestroy is called")
        super.onDestroyView()
        stopTimer()
        unregisterWifiScanReceiver()
        switchJob?.cancel()
        oldDbmWifi = 0.0
        oldDbmCellular = 0.0
        unregisterCellular()
        unregisterCellularType()
        unregisterGeneralNetworkCallback()
        unregisterNetworkCallbackWifi()
        _binding.toggleButtonGroup.clearOnButtonCheckedListeners()
    }
}