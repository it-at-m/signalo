package de.muenchen.appcenter.signalo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
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
            val snapshotWithCounter = snapshot.copy(id = current.counter)
            current.copy(
                snapshots = current.snapshots + snapshotWithCounter,
                counter = current.counter + 1
            )
        }
    }
}