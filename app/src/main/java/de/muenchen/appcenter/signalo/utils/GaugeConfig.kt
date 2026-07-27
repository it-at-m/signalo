package de.muenchen.appcenter.signalo.utils

import androidx.core.graphics.toColorInt
import com.ekn.gruzer.gaugelibrary.ArcGauge
import com.ekn.gruzer.gaugelibrary.Range

object GaugeConfig {
    fun createGauge(
        arcGauge: ArcGauge,
        minValue: Double,
        limit1: Double,
        limit2: Double,
        maxValue: Double
    ) {
        /**
         * Creates the Range Object with the following parameters
         * @param from starting point of the Range
         * @param to ending point of the Range
         * @param color the color in hex which the Range should have
         * Returns the range object
         */
        fun createRange(from: Double, to: Double, color: Int): Range {
            val range = Range()
            range.from = from
            range.to = to
            range.color = color
            return range
        }
        arcGauge.addRange(createRange(minValue, limit1, Constants.GAUGE_RANGE1_COLOR.toColorInt()))
        arcGauge.addRange(createRange(limit1, limit2, Constants.GAUGE_RANGE2_COLOR.toColorInt()))
        arcGauge.addRange(createRange(limit2, maxValue, Constants.GAUGE_RANGE3_COLOR.toColorInt()))
        arcGauge.minValue = minValue
        arcGauge.maxValue = maxValue
        arcGauge.setValueColorAttr(android.R.attr.textColorPrimary)
        arcGauge.setFormatter { value ->
            "${value.toInt()} dBm"
        }
    }
}