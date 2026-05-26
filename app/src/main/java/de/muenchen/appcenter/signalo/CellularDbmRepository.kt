package de.muenchen.appcenter.signalo

import android.content.Context
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class CellularDbmRepository(context: Context) {
    private val telephonyManager =
        context.getSystemService(TelephonyManager::class.java)
    private val mainExecutor = context.mainExecutor

    fun observeSignalStrength(): Flow<Double?> = callbackFlow {
        val cellularCallBack: TelephonyCallback =
            object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    if (signalStrength.cellSignalStrengths.isNotEmpty()) {
                        val dbmCellular = signalStrength.cellSignalStrengths[0].dbm.toDouble()
                        Timber.d("Sending dbm Values...%s", dbmCellular)
                        trySend(dbmCellular)
                    } else {
                        trySend(null)
                    }
                }
            }
        telephonyManager.registerTelephonyCallback(
            mainExecutor,
            cellularCallBack
        )
        awaitClose {
            telephonyManager.unregisterTelephonyCallback(cellularCallBack)
        }
    }
}
