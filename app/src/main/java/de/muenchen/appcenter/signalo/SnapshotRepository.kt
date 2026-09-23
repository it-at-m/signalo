package de.muenchen.appcenter.signalo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import de.muenchen.appcenter.signalo.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

val Context.snapshotDataStore: DataStore<SnapshotContainer> by dataStore(
    fileName = "snapshots.json",
    serializer = SnapshotSerializer
)

class SnapshotRepository(private val snapshotDatastore: DataStore<SnapshotContainer>) {
    val snapshots: Flow<List<Snapshot>> =
        snapshotDatastore.data.map { it.snapshots }

    suspend fun add(snapshot: Snapshot) {
        snapshotDatastore.updateData { current ->
            val nextCounter = current.counter + 1
            var finalName = snapshot.name
            if (snapshot.name == Constants.EMPTY) {
                finalName = "Snapshot #$nextCounter"
            }
            val namedSnapshot = snapshot.copy(name = finalName)
            current.copy(
                snapshots = current.snapshots + namedSnapshot,
                counter = current.counter + 1
            )
        }
    }

    suspend fun delete(snapshot: Snapshot) {
        snapshotDatastore.updateData { current ->
            current.copy(
                snapshots = current.snapshots - snapshot
            )
        }
        Timber.d("repo has deleted snapshot: " + snapshot.name)
    }
}