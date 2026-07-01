package de.muenchen.appcenter.signalo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import de.muenchen.appcenter.signalo.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.snapshotDataStore: DataStore<SnapshotContainer> by dataStore(
    fileName = "snapshots.json",
    serializer = SnapshotSerializer
)

class SnapshotRepository(private val dataStore: DataStore<SnapshotContainer>) {
    val snapshots: Flow<List<Snapshot>> =
        dataStore.data.map { it.snapshots }

    suspend fun add(snapshot: Snapshot) {

        dataStore.updateData { current ->
            val nextCounter = current.counter + 1
            var finalName = snapshot.name
            if (snapshot.name == Constants.EMPTY) {
                finalName = "Snapshot #$nextCounter"
            }
            val snapshot = snapshot.copy(name = finalName)
            current.copy(
                snapshots = current.snapshots + snapshot,
                counter = current.counter + 1
            )
        }
    }
}