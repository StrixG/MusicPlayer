package com.obrekht.musicplayer.data

import com.obrekht.musicplayer.model.Album

interface AlbumRepository {

    fun getAll(): List<Album>
    fun getAlbumById(albumId: Long): Album?
    fun addAlbum(album: Album)
    suspend fun refreshAlbums()
    fun getTrackUrl(trackId: Long): String?
}
