package de.muenchen.appcenter.signalo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import de.muenchen.appcenter.signalo.databinding.FragmentSnapshotListBinding
import kotlinx.coroutines.launch
import timber.log.Timber

class SnapshotListFragment : Fragment() {
    private lateinit var snapshotAdapter: SnapshotAdapter
    private lateinit var _binding: FragmentSnapshotListBinding

    //own factory is needed because repository is in the constructor which is non-standard
    private val factory = viewModelFactory {
        initializer {
            val store = requireContext().applicationContext.snapshotDataStore
            SnapshotViewModel(SnapshotRepository(store))
        }
    }
    private val snapshotViewModel: SnapshotViewModel by viewModels { factory }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSnapshotListBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        snapshotAdapter = SnapshotAdapter { snapshot ->
            Timber.d("Snapshot geklickt: ${snapshot.name}")
        }
        _binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = snapshotAdapter
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotViewModel.snapshots.collect { list: List<Snapshot> ->
                    snapshotAdapter.submitList(list)
                }
            }
        }

    }
}
