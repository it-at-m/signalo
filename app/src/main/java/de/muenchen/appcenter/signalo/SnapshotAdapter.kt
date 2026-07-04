package de.muenchen.appcenter.signalo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.muenchen.appcenter.signalo.databinding.ItemSnapshotBinding
import java.text.DateFormat

class SnapshotAdapter(
    private val onClick: (Snapshot) -> Unit
) : ListAdapter<Snapshot, SnapshotAdapter.SnapshotViewHolder>(SnapshotDiffCallback()) {

    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)

    inner class SnapshotViewHolder(
        private val binding: ItemSnapshotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(snapshot: Snapshot) {
            binding.textTitle.text = snapshot.name
            binding.textSubtitle.text = dateFormat.format(snapshot.creationDate)
            binding.networkTypeIcon.setImageResource(
                when (snapshot.details) {
                    is SnapshotDetails.Wifi -> R.drawable.wifi_24px
                    is SnapshotDetails.Cellular -> R.drawable.cell_tower_24px
                }
            )
            binding.root.setOnClickListener { onClick(snapshot) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotViewHolder {
        val binding = ItemSnapshotBinding.inflate(
            LayoutInflater.from(parent.context), parent, false

        )
        return SnapshotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SnapshotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SnapshotDiffCallback : DiffUtil.ItemCallback<Snapshot>() {
        override fun areItemsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean {
            return oldItem.creationDate == newItem.creationDate
        }

        override fun areContentsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean {
            return oldItem == newItem
        }
    }
}