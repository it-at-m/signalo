package de.muenchen.appcenter.signalo.utils

import de.muenchen.appcenter.signalo.R

object NetworkIcons {

    /**returns the resID (INT) of the icons
     * @param cellularType the value containing the current cellularType as a String,
     */
    fun getCellularTypeIcon(cellularType: String): Int {
        return when (cellularType) {
            Constants.NETWORKTYPE_5G_PLUS,
            Constants.NETWORKTYPE_5G_NSA,
            Constants.NETWORKTYPE_5G_SA -> R.drawable._g_24px

            Constants.NETWORKTYPE_4G_PLUS -> R.drawable._g_plus_mobiledata_24px
            Constants.NETWORKTYPE_LTE -> R.drawable.lte_mobiledata_24px
            Constants.NETWORKTYPE_3G_PLUS,
            Constants.NETWORKTYPE_3G -> R.drawable._g_mobiledata_24px

            Constants.NETWORKTYPE_EDGE,
            Constants.NETWORKTYPE_2G -> R.drawable.e_mobiledata_24px

            else -> R.drawable.help_center_24px
        }
    }
}