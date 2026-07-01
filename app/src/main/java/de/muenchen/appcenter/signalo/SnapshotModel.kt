package de.muenchen.appcenter.signalo

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class Snapshot(
    val name: String,
    val creationDate: Long,
    val details: SnapshotDetails
)

@kotlinx.serialization.Serializable
sealed class SnapshotDetails {
    @kotlinx.serialization.Serializable
    data class Cellular(
        val dbm: Double,
        val cellId: String,
        val provider: String,
        val networkType: String,
        val frequencyBand: String
    ) : SnapshotDetails()

    @Serializable
    data class Wifi(
        val dbm: Double,
        val ssid: String,
        val frequency: String,
        val linkspeed: String,
        val encryption: String
    ) : SnapshotDetails()
}

@Serializable
data class SnapshotContainer(
    val snapshots: List<Snapshot> = emptyList(),
    val counter: Int = 0
)