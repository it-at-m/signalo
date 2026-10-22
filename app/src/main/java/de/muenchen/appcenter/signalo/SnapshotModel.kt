package de.muenchen.appcenter.signalo

import kotlinx.serialization.Serializable

/*
 * Data structure for snapshots
 * This structure is saved as JSON (before it is encrypted)
 * Caution: renaming or removing fields might lead to data loss.
 * Always add default values to new values
 */


/**
 * One saved measurement(snapshot).
 *
 * @property name named by user
 * @property creationDate used to display time of creation
 * and the unique ID of a Snapshot
 * @property details the network specific values
 */
@Serializable
data class Snapshot(
    val name: String,
    val creationDate: Long,
    val details: SnapshotDetails
)

/**
 * A snapshot is always either Wifi or Cellular, never both.
 */
@Serializable
sealed class SnapshotDetails {
    @Serializable
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