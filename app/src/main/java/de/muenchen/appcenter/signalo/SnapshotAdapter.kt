package de.muenchen.appcenter.signalo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.muenchen.appcenter.signalo.databinding.ItemSnapshotBinding

class SnapshotAdapter(
    private val onClick: (Snapshot) -> Unit
) : RecyclerView.Adapter<SnapshotAdapter.SnapshotViewHolder>() {

    private var items: List<Snapshot> = emptyList()

    inner class SnapshotViewHolder(
        private val binding: ItemSnapshotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(snapshot: Snapshot) {
            binding.textTitle.text = snapshot.name
            binding.textSubtitle.text = snapshot.creationDate.toString()
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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Snapshot>) {
        items = newItems
        notifyDataSetChanged()
    }
}