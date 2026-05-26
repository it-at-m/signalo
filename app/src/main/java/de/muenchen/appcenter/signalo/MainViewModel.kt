package de.muenchen.appcenter.signalo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.muenchen.appcenter.signalo.utils.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var refreshState: MutableLiveData<String> = MutableLiveData(Constants.REFRESH_IDLE)
    var onWifi: MutableLiveData<Boolean> = MutableLiveData(false)
    var onCellular: MutableLiveData<Boolean> = MutableLiveData(true)
    var activeCooldown: MutableLiveData<Boolean> = MutableLiveData(false)
    var connectedBSSID: MutableLiveData<String> = MutableLiveData("")
    private var oldDbmCellular: Double = 0.0
    var animatorProgress: MutableLiveData<Int> = MutableLiveData(0)
    var permissionRequestedThisSession: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _wifiDbmValue: MutableLiveData<Double> = MutableLiveData(Constants.GAUGE_WIFI_MIN)
    private val _cellularDbmValue: MutableLiveData<Double> =
        MutableLiveData(Constants.GAUGE_CELLULAR_MIN)
    var simState: MutableLiveData<String> = MutableLiveData(Constants.SCAN_PENDING)
    val wifiDbmValue: LiveData<Double> get() = _wifiDbmValue
    val cellularDbmValue: LiveData<Double> get() = _cellularDbmValue
    private val cellularDbmRepository = CellularDbmRepository(application)
    private var cellularDbmJob: Job? = null

    /**
     * Starts collecting cellular dBm values from repository.
     * Cancels any existing observation before starting a new one.
     * handles all the validation logic
     * sets the validated values for the UI to be displayed
     */
    fun startObservingCellularDbm() {
        cellularDbmJob?.cancel()
        cellularDbmJob = viewModelScope.launch {
            cellularDbmRepository.observeSignalStrength().collect { dbmCellular ->
                if (dbmCellular != null) {
                    if (dbmCellular != oldDbmCellular) {
                        Timber.d(

                            "New Cellular DBM Value is: %s", dbmCellular.toString()
                        )
                        setCellularDbmValue(dbmCellular)
                        oldDbmCellular = dbmCellular
                    }
                } else {
                    setCellularDbmValue(Constants.GAUGE_CELLULAR_MIN)
                    Timber.d(
                        "Cellular Dbm Value is null! setting value to %s",
                        Constants.GAUGE_CELLULAR_MIN
                    )
                }
            }
        }
    }

    fun stopObservingCellularDbm() {
        cellularDbmJob?.cancel()
        cellularDbmJob = null
    }

    fun setWifiDbmValue(value: Double) {
        _wifiDbmValue.postValue(value)
    }

    fun setCellularDbmValue(value: Double) {
        _cellularDbmValue.value = value
    }

    private val _cellularType: MutableLiveData<String> = MutableLiveData("[unknown]")
    val cellularType: LiveData<String> get() = _cellularType
    fun setCellularType(value: String) {
        _cellularType.postValue(value)
    }

    private val _cellId: MutableLiveData<String> = MutableLiveData("[unknown]")
    val cellId: LiveData<String> get() = _cellId
    fun setCellId(value: String) {
        _cellId.postValue(value)
    }

    private val _currentNetProvider: MutableLiveData<String> = MutableLiveData("[unknown]")
    val currentNetprovider: LiveData<String> get() = _currentNetProvider
    fun setcurrentNetProvider(value: String) {
        _currentNetProvider.postValue(value)
    }

    private val _currentSSID: MutableLiveData<String> = MutableLiveData("[unknown]")
    val currentSSID: LiveData<String> get() = _currentSSID
    fun setCurrentSSID(value: String) {
        _currentSSID.postValue(value)
    }

    private val _currentFrequency: MutableLiveData<String> = MutableLiveData("[unknown]")
    val currentFrequency: LiveData<String> get() = _currentFrequency
    fun setCurrentFrequency(value: String) {
        _currentFrequency.postValue(value)
    }

    private val _currentLinkspeed: MutableLiveData<String> = MutableLiveData("[unknown]")
    val currentLinkspeed: LiveData<String> get() = _currentLinkspeed
    fun setCurrentLinkspeed(value: String) {
        _currentLinkspeed.postValue(value)
    }

    private val _currentEncryptionType: MutableLiveData<String> = MutableLiveData("[unknown]")
    val currentEncryptionType: LiveData<String> get() = _currentEncryptionType
    fun setCurrentEncryptionType(value: String) {
        _currentEncryptionType.postValue(value)
    }

    private val _currentCellularBand: MutableLiveData<String> = MutableLiveData("[unknown]")
    val currentCellularBand: LiveData<String> get() = _currentCellularBand
    fun setCurrentCellularBand(value: String) {
        _currentCellularBand.postValue(value)
    }
}