package com.obrekht.musicplayer.ui.album

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Future

private const val TRACK_PROGRESS_UPDATE_INTERVAL = 100L

class AlbumFragment : Fragment(R.layout.fragment_album) {

    private val binding by viewBinding(FragmentAlbumBinding::bind)
    private val viewModel: AlbumViewModel by viewModels { AlbumViewModel.Factory }

    private var mediaControllerFuture: Future<MediaController>? = null
    private var mediaController: MediaController? = null

    private val mediaControllerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            with(binding.bottomSheetPlayer) {
                trackName.text = if (mediaMetadata.title.isNullOrBlank()) {
                    getString(R.string.unknown_track)
                } else {
                    mediaMetadata.title
                }
                artist.text = mediaMetadata.artist
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED
                )
            ) {
                updatePlayPauseButton()
            }
        }
    }

    private val trackInteractionListener = object : TrackInteractionListener {
        override fun onClick(track: Track, view: View) {
            mediaController?.apply {
                val trackUrl = viewModel.getTrackUrl(track) ?: return
                val mediaItem = MediaItem.fromUri(trackUrl)

                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        val adapter = AlbumTrackAdapter(trackInteractionListener)
        binding.trackList.adapter = adapter

        val playerBinding = binding.bottomSheetPlayer

        with(binding) {
            container.layoutParams = (container.layoutParams as MarginLayoutParams).apply {
                bottomMargin = resources.getDimension(R.dimen.bottom_player_height).toInt()
            }
        }

        with(playerBinding) {
            buttonPlay.setOnClickListener {
                mediaController?.run {
                    if (isPlaying) {
                        pause()
                    } else {
                        prepare()
                        play()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.onEach {
                    it.album?.let { album ->
                        albumTitle.text = album.title
                        artist.text = album.artist
                        published.text = album.published

                        publishedDivider.isVisible = true

                        adapter.submitList(album.tracks)
                    }
                }.launchIn(this)

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

    override fun onStart() {
        super.onStart()

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

    override fun onStop() {
        mediaControllerFuture?.let(MediaController::releaseFuture)
        mediaControllerFuture = null
        mediaController = null
        super.onStop()
    }

    private fun onMediaControllerReady(mediaController: MediaController) {
        this.mediaController = mediaController
        mediaController.addListener(mediaControllerListener)
    }

    private fun updatePlayPauseButton() {
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

        binding.buttonPlayAlbum.setText(textResourceId)
        binding.buttonPlayAlbum.setIconResource(iconResourceId)
        binding.bottomSheetPlayer.buttonPlay.setIconResource(iconResourceId)
    }

    private fun updateTrackProgress() {
        with(binding.bottomSheetPlayer.trackProgress) {
            setProgress(mediaController?.run {
                (currentPosition.toDouble() / duration * max).toInt()
            } ?: 0, true)
        }
    }
}
