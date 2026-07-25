package de.muenchen.appcenter.signalo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.navArgs
import de.muenchen.appcenter.signalo.databinding.FragmentSnapshotDisplayBinding
import de.muenchen.appcenter.signalo.utils.Formatters
import de.muenchen.appcenter.signalo.utils.NetworkIcons
import de.muenchen.appcenter.signalo.utils.NetworkIcons.getCellularTypeIcon

class SnapshotDisplayFragment : Fragment() {
    private lateinit var _binding: FragmentSnapshotDisplayBinding
    private val args: SnapshotDisplayFragmentArgs by navArgs()
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
        _binding = FragmentSnapshotDisplayBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selectedSnapshotID = args.creationDate
        snapshotViewModel.getSnapshot(selectedSnapshotID)
        snapshotViewModel.currentSnapshot.observe(viewLifecycleOwner) { currentSnapshot ->
            displaySnapshot(currentSnapshot)
        }
    }

    private fun displaySnapshot(currentSnapshot: Snapshot) {
        displayGeneralValues(currentSnapshot)
        when (val details = currentSnapshot.details) {
            is SnapshotDetails.Cellular -> displayCellularValues(details)
            is SnapshotDetails.Wifi -> displayWifiValues(details)
        }

    }

    fun displayGeneralValues(currentSnapshot: Snapshot) {
        _binding.textViewSnapshotName.text = currentSnapshot.name
        _binding.textViewSnapshotDate.text =
            Formatters.formatTimestamp(currentSnapshot.creationDate)

    }

    fun displayCellularValues(details: SnapshotDetails.Cellular) {
        _binding.cellularContainer.root.visibility = VISIBLE
        _binding.wifiContainer.root.visibility = GONE
        _binding.imageViewSnapshotType.setImageResource(R.drawable.cell_tower_24px)
        //setting NetworkType values
        _binding.cellularContainer.imageViewNetType.setImageResource(getCellularTypeIcon(details.networkType))
        _binding.cellularContainer.textViewNetTypeValue.text = details.networkType
        //setting Provider values
        _binding.cellularContainer.imageViewCellularProvider.setImageResource(
            NetworkIcons.providerNameToDrawable[details.provider] ?: R.drawable.help_center_24px
        )
        _binding.cellularContainer.textViewCellularProviderValue.text = details.provider
        //setting Frequency values
        _binding.cellularContainer.textViewCellularFrequencyValue.text = details.frequencyBand
        //setting Cell ID
        _binding.cellularContainer.textViewCellIdValue.text = details.cellId
    }

    fun displayWifiValues(details: SnapshotDetails.Wifi) {
        _binding.imageViewSnapshotType.setImageResource(R.drawable.wifi_24px)
        _binding.cellularContainer.root.visibility = GONE
        _binding.wifiContainer.root.visibility = VISIBLE
        //setting Wifi values
        _binding.wifiContainer.textViewFreqValue.text = details.frequency
        _binding.wifiContainer.textViewEncryptionTypeValue.text = details.encryption
        _binding.wifiContainer.textViewSSIDValue.text = details.ssid
        _binding.wifiContainer.textViewLinkspeedValue.text = details.linkspeed
    }
}