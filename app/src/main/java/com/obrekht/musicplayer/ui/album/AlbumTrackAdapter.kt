package com.obrekht.musicplayer.ui.album

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.obrekht.musicplayer.databinding.ItemTrackBinding
import com.obrekht.musicplayer.model.Album
import com.obrekht.musicplayer.model.Track

class AlbumTrackAdapter(
    private val interactionListener: TrackInteractionListener
) : ListAdapter<Track, AlbumTrackAdapter.ViewHolder>(DiffCallback()) {

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
                track?.let { track -> interactionListener.onClick(track, it) }
            }
        }

        fun bind(track: Track) {
            this.track = track
            with(binding) {
                trackId.text = track.id.toString()
                trackName.text = track.file
            }
        }

        fun recycle() {
            track = null
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem == newItem
        }
    }
}

interface TrackInteractionListener {
    fun onClick(track: Track, view: View) {}
}
