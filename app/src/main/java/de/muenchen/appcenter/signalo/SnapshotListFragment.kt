package de.muenchen.appcenter.signalo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.muenchen.appcenter.signalo.databinding.FragmentSnapshotListBinding
import de.muenchen.appcenter.signalo.databinding.TextInputLayoutBinding
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSnapshotListBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        snapshotAdapter = SnapshotAdapter(
            onClick = { snapshot ->
                Timber.d("Snapshot geklickt: ${snapshot.name}")
                findNavController().navigate(
                    SnapshotListFragmentDirections.actionSnapshotListToSnapshotDisplay(
                        snapshot.creationDate
                    )
                )
            },
            onDeleteClick = { snapshot ->
                Timber.d(
                    "onDelete was called, now calling viewmodel with snapshot%s",
                    snapshot.name
                )
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.snapshot_item_delete_dialog_title))
                    .setMessage(
                        getString(
                            R.string.snapshot_item_delete_dialog_message,
                            snapshot.name
                        )
                    )
                    .setPositiveButton(R.string.delete) { dialog, _ ->
                        snapshotViewModel.deleteSnapshot(snapshot.creationDate)
                        Toast.makeText(
                            requireContext(),
                            snapshot.name + getString(R.string.snapshot_deleted_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.speedtest_dialog_negative_button) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }, onRenameClick = { snapshot ->

                val dialogBinding = TextInputLayoutBinding.inflate(layoutInflater)
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.snapshot_rename_dialog_title))
                    .setView(dialogBinding.root)
                    .setMessage(getString(R.string.snapshot_rename_dialog_message))
                    .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                        val newName = dialogBinding.editSnapshotName.text?.toString()
                        snapshotViewModel.renameSnapshot(snapshot.creationDate, newName!!)
                        dialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.snapshot_rename_toast),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    .setNegativeButton(R.string.speedtest_dialog_negative_button) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

                val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                saveButton.isEnabled = !dialogBinding.editSnapshotName.text.isNullOrBlank()
            dialogBinding.editSnapshotName.doAfterTextChanged {
                saveButton.isEnabled = !dialogBinding.editSnapshotName.text.isNullOrBlank()

            }
            }

        )
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