package com.obrekht.musicplayer.ui.album

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.obrekht.musicplayer.databinding.ItemTrackBinding
import com.obrekht.musicplayer.model.Track

class AlbumTrackAdapter(
    private val interactionListener: TrackInteractionListener
) : ListAdapter<TrackItem, AlbumTrackAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding, interactionListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
    }

    class ViewHolder(
        val binding: ItemTrackBinding,
        interactionListener: TrackInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var track: Track? = null

        init {
            itemView.setOnClickListener {
                track?.let { track ->
                    interactionListener.onClick(track, bindingAdapterPosition, it)
                }
            }
        }

        fun bind(item: TrackItem) {
            val track = item.track.also { this.track = it }
            with(binding) {
                itemView.isSelected = item.isSelected

                trackId.text = track.id.toString()
                trackName.text = track.file
            }
        }

        fun recycle() {
            track = null
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TrackItem>() {
        override fun areItemsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean {
            return oldItem.track.id == newItem.track.id
        }

        override fun areContentsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean {
            return oldItem == newItem
        }
    }
}

interface TrackInteractionListener {
    fun onClick(track: Track, position: Int, view: View) {}
}

data class TrackItem(
    val track: Track,
    val isSelected: Boolean = false
)
