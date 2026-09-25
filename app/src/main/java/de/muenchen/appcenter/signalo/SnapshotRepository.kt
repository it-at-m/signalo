package de.muenchen.appcenter.signalo

import android.annotation.SuppressLint
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

    @SuppressLint("BinaryOperationInTimber")
    suspend fun delete(snapshot: Snapshot) {
        snapshotDatastore.updateData { current ->
            current.copy(
                snapshots = current.snapshots - snapshot
            )
        }
        Timber.d("repo has deleted snapshot: " + snapshot.name)
    }

    suspend fun rename(snapshot: Snapshot, newName: String) {
        snapshotDatastore.updateData { current ->
            val newList = mutableListOf<Snapshot>()
            for (i in current.snapshots) {
                if (i.creationDate == snapshot.creationDate) {
                    newList.add(i.copy(name = newName))
                } else {
                    newList.add(i)
                }
            }
            current.copy(snapshots = newList)
        }
    }
}