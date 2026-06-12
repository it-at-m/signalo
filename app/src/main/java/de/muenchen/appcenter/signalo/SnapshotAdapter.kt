package de.muenchen.appcenter.signalo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.muenchen.appcenter.signalo.databinding.ItemSnapshotBinding
import de.muenchen.appcenter.signalo.utils.Constants

class SnapshotAdapter(
    private val onClick: (SnapshotData) -> Unit
) : RecyclerView.Adapter<SnapshotAdapter.SnapshotViewHolder>() {

    private var items: List<SnapshotData> = emptyList()

    inner class SnapshotViewHolder(
        private val binding: ItemSnapshotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(snapshot: SnapshotData) {
            binding.textTitle.text = snapshot.title
            binding.textSubtitle.text = snapshot.createdDate
            binding.networkTypeIcon.setImageResource(
                when (snapshot.networkType) {
                    Constants.WIFI -> {
                        R.drawable.wifi_24px
                    }

                    Constants.CELLULAR -> {
                        R.drawable.cell_tower_24px
                    }

                    else -> {
                        R.drawable.help_center_24px
                    }
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

    fun submitList(newItems: List<SnapshotData>) {
        items = newItems
        notifyDataSetChanged()
    }
}