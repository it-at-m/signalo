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
    val providerNameToDrawable: Map<String, Int> = mapOf(
        Constants.PROVIDER_TELEKOM to R.drawable.deutsche_telekom_2022_svg,
        Constants.PROVIDER_VODAFONE to R.drawable.vodafone_kabel_deutschland_logo_vector,
        Constants.PROVIDER_O2 to R.drawable.o2_svg,
        Constants.PROVIDER_1UND1 to R.drawable.__1_logo,
        Constants.PROVIDER_A1 to R.drawable.logo_of_a1,
        Constants.PROVIDER_SWISSCOM to R.drawable.scmn_sw_38a30a24,
        Constants.PROVIDER_SUNRISE to R.drawable.sunrise_2022_svg,
        Constants.PROVIDER_SALT to R.drawable.icones_logosalt_black,
    )
}