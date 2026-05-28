package de.muenchen.appcenter.signalo

import android.content.Context
import android.telephony.TelephonyManager

class CellularInfosRepository(context: Context) {
    private var telephonyManager =
        context.getSystemService(TelephonyManager::class.java)
    private val mainExecutor = context.mainExecutor
    fun getNetworkOperator() {
        if (!telephonyManager.networkOperator.isNullOrBlank()) {
            val networkOperator = telephonyManager.networkOperator
        }
    }
}