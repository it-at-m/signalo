package de.muenchen.appcenter.signalo.utils

import java.text.DateFormat

object Formatters {
    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
        return dateFormat.format(timestamp)
    }

    /**
     * helper function for Cellular band to format it accordingly to 3GPP standard
     */
    fun formatCellBand(cellBand: IntArray, praefix: String): String {
        val formattedCellBand = cellBand.joinToString(", ", transform = { praefix.plus(it) })
        return formattedCellBand
    }
}