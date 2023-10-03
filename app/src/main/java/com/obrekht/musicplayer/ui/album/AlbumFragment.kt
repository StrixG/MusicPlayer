package com.obrekht.musicplayer.ui.album

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.obrekht.musicplayer.R
import com.obrekht.musicplayer.databinding.FragmentAlbumBinding
import com.obrekht.musicplayer.model.Track
import com.obrekht.musicplayer.service.PlaybackService
import com.obrekht.musicplayer.utils.viewBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Future

private const val TRACK_PROGRESS_UPDATE_INTERVAL = 100L

class AlbumFragment : Fragment(R.layout.fragment_album) {

    private val binding by viewBinding(FragmentAlbumBinding::bind)
    private val viewModel: AlbumViewModel by viewModels { AlbumViewModel.Factory }

    private var adapter: AlbumTrackAdapter? = null

    private var mediaControllerFuture: Future<MediaController>? = null
    private var mediaController: MediaController? = null

    private var trackList: List<Track> = emptyList()

    private val mediaControllerListener = object : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateTrackList(trackList)
            updateTrackInfo()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED
                )
            ) {
                if (player.isPlaying || player.playWhenReady) {
                    showBottomPlayer()
                }

                updateButtons()
                updateTrackProgress()
                updateTrackInfo()
            }
        }
    }

    private val trackInteractionListener = object : TrackInteractionListener {
        override fun onClick(track: Track, position: Int, view: View) {
            mediaController?.run {
                if (currentMediaItem == null) {
                    populatePlaylist()
                }
                if (currentMediaItemIndex != position) {
                    seekToDefaultPosition(position)
                    play()
                } else {
                    Util.handlePlayPauseButtonAction(this)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        adapter = AlbumTrackAdapter(trackInteractionListener)
        binding.trackListView.adapter = adapter

        with(binding) {
            buttonPlayAlbum.setOnClickListener {
                mediaController?.run {
                    if (Util.shouldShowPlayButton(this) && currentMediaItem == null) {
                        populatePlaylist()
                    }
                    Util.handlePlayPauseButtonAction(this)
                }
            }
        }

        with(binding.bottomSheetPlayer) {
            buttonPlay.setOnClickListener {
                mediaController?.run {
                    Util.handlePlayPauseButtonAction(this)
                }
            }

            buttonSkipNext.setOnClickListener {
                mediaController?.seekToNextMediaItem()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.onEach(::handleUiState).launchIn(this)

                launch {
                    while (isActive) {
                        mediaController?.run {
                            if (isConnected && currentMediaItem != null) {
                                updateTrackProgress()
                            }
                        }
                        delay(TRACK_PROGRESS_UPDATE_INTERVAL)
                    }
                }
            }
        }

        Unit
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    override fun onStart() {
        super.onStart()
        initializeMediaController()
    }

    override fun onStop() {
        releaseMediaController()
        super.onStop()
    }

    private fun handleUiState(state: AlbumUiState) = with(binding) {
        state.album?.let { album ->
            albumTitle.text = album.title
            artist.text = album.artist
            published.text = album.published

            publishedDivider.isVisible = true

            trackList = album.tracks
            updateTrackList(trackList)
        }

        Unit
    }

    private fun initializeMediaController() {
        if (mediaController != null) return

        val context = requireContext()
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            .apply {
                addListener(
                    {
                        mediaControllerFuture = null
                        onMediaControllerReady(get())
                    },
                    MoreExecutors.directExecutor()
                )
            }
    }

    private fun releaseMediaController() {
        mediaControllerFuture?.let(MediaController::releaseFuture)
        mediaController?.release()
        mediaControllerFuture = null
        mediaController = null
    }

    private fun onMediaControllerReady(mediaController: MediaController) {
        this.mediaController = mediaController
        mediaController.apply {
            prepare()

            repeatMode = Player.REPEAT_MODE_ALL
            addListener(mediaControllerListener)

            if (isPlaying || playWhenReady) {
                showBottomPlayer()
                updateTrackList(trackList)
            }
        }

        updateButtons()
        updateTrackInfo()
    }

    private fun populatePlaylist() {
        mediaController?.setMediaItems(trackList.mapNotNull { track ->
            viewModel.getTrackUrl(track)?.let {
                MediaItem.Builder()
                    .setMediaId(track.id.toString())
                    .setUri(viewModel.getTrackUrl(track))
                    .build()
            }
        })
    }

    private fun showBottomPlayer() {
        with(binding) {
            container.layoutParams = (container.layoutParams as MarginLayoutParams).apply {
                bottomMargin = resources.getDimension(R.dimen.bottom_player_height).toInt()
            }
            bottomSheetPlayer.root.isVisible = true
        }
    }

    private fun updateTrackList(trackList: List<Track>) {
        val currentTrackId = mediaController?.currentMediaItem?.mediaId?.toLongOrNull()

        adapter?.submitList(trackList.map {
            TrackItem(
                track = it,
                isSelected = (currentTrackId == it.id)
            )
        })
    }

    private fun updateButtons() {
        val shouldShowPlay = Util.shouldShowPlayButton(mediaController)

        val textResourceId = if (shouldShowPlay) {
            R.string.play
        } else {
            R.string.pause
        }

        val iconResourceId = if (shouldShowPlay) {
            R.drawable.round_play_arrow_24
        } else {
            R.drawable.round_pause_24
        }

        with(binding) {
            buttonPlayAlbum.setText(textResourceId)
            buttonPlayAlbum.setIconResource(iconResourceId)
            bottomSheetPlayer.buttonPlay.setIconResource(iconResourceId)
            bottomSheetPlayer.buttonSkipNext.isVisible = mediaController?.hasNextMediaItem() == true
        }
    }

    private fun updateTrackProgress() {
        with(binding.bottomSheetPlayer.trackProgress) {
            progress = mediaController?.run {
                (currentPosition.toDouble() / duration * max).toInt()
            } ?: 0
        }
    }

    private fun updateTrackInfo() {
        val mediaMetadata = mediaController?.mediaMetadata ?: return

        with(binding.bottomSheetPlayer) {
            trackName.text = if (mediaMetadata.title.isNullOrBlank()) {
                getString(R.string.unknown_track)
            } else {
                mediaMetadata.title
            }
            artist.text = mediaMetadata.artist
        }
    }
}
