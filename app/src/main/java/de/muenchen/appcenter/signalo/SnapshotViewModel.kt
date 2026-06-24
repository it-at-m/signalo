package de.muenchen.appcenter.signalo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow

class SnapshotViewModel(private val repository: SnapshotRepository) : ViewModel() {
    val snapshots: Flow<List<Snapshot>> = repository.snapshots

    
}
