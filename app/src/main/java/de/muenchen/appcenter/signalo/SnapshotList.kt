package de.muenchen.appcenter.signalo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import de.muenchen.appcenter.signalo.databinding.FragmentSnapshotListBinding
import de.muenchen.appcenter.signalo.utils.Constants
import timber.log.Timber

class SnapshotList : Fragment() {
    private lateinit var snapshotAdapter: SnapshotAdapter
    private lateinit var _binding: FragmentSnapshotListBinding
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
            Timber.d("Snapshot geklickt: ${snapshot.title}")
        }
        _binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = snapshotAdapter
        }

        val fakeSnapshots = listOf(
            SnapshotData(
                title = "Zuhause WLAN",
                createdDate = "11.06.2026",
                networkType = Constants.WIFI
            ),
            SnapshotData(
                title = "Büro Mobilfunk",
                createdDate = "10.06.2026",
                networkType = Constants.CELLULAR
            ),
            SnapshotData(
                title = "Keller Test",
                createdDate = "09.06.2026",
                networkType = Constants.WIFI
            ),
        )
        snapshotAdapter.submitList(fakeSnapshots)
    }
}
