package com.obrekht.musicplayer.ui.album

import com.obrekht.musicplayer.MusicPlayerApplication
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.obrekht.musicplayer.data.AlbumRepository
import com.obrekht.musicplayer.model.Album
import com.obrekht.musicplayer.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlbumViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            albumRepository.refreshAlbums()
            _uiState.update { it.copy(album = albumRepository.getAll().firstOrNull()) }
        }
    }

    fun getTrackUrl(track: Track): String? = albumRepository.getTrackUrl(track.id)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicPlayerApplication)
                AlbumViewModel(application.albumRepository)
            }
        }
    }
}

data class AlbumUiState(
    val album: Album? = null
)
