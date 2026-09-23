package de.muenchen.appcenter.signalo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class SnapshotViewModel(private val repository: SnapshotRepository) : ViewModel() {
    private val _currentSnapshot = MutableLiveData<Snapshot>()
    val currentSnapshot: LiveData<Snapshot> = _currentSnapshot
    val snapshots: Flow<List<Snapshot>> = repository.snapshots


    fun getSnapshot(id: Long) {
        viewModelScope.launch {
            val allSnapshots = repository.snapshots.first()
            allSnapshots.find { it.creationDate == id }
                ?.let { _currentSnapshot.postValue(it) }
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch {
            val allSnapshots = repository.snapshots.first()
            val snapshotToDelete = allSnapshots.find { it.creationDate == id }
            if (snapshotToDelete != null) {
                Timber.d("Viewmodel is calling repo to delete " + snapshotToDelete.name)
                repository.delete(snapshotToDelete)
            }
        }
    }

}

