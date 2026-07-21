package de.muenchen.appcenter.signalo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.navArgs
import de.muenchen.appcenter.signalo.databinding.FragmentSnapshotDisplayBinding

class SnapshotDisplayFragment : Fragment() {
    private lateinit var _binding: FragmentSnapshotDisplayBinding
    val selectedSnapshotID = navArgs<SnapshotDisplayFragmentArgs>().value.creationDate
    private val snapshotViewModel: SnapshotViewModel by viewModels { factory }
    private val factory = viewModelFactory {
        initializer {
            val store = requireContext().applicationContext.snapshotDataStore
            SnapshotViewModel(SnapshotRepository(store))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSnapshotDisplayBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        snapshotViewModel.getSnapshot(selectedSnapshotID)
        snapshotViewModel.currentSnapshot.observe(viewLifecycleOwner) {
            DisplayValues()
        }
    }

    private fun DisplayValues() {


    }
}