package de.muenchen.appcenter.signalo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SnapshotViewModel(private val repository: SnapshotRepository) : ViewModel() {
    var currentSnapshot: MutableLiveData<Snapshot?> = MutableLiveData(null)
    val snapshots: Flow<List<Snapshot>> = repository.snapshots

    fun getSnapshot(id: Long) {
        viewModelScope.launch {
            val allSnapshots = repository.snapshots.first()
            val selectedSnapshot = allSnapshots.find { it.creationDate == id }
            currentSnapshot.postValue(selectedSnapshot)

        }
    }

}

