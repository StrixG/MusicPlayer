package com.obrekht.musicplayer.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Album(
    val id: Long,
    val title: String,
    val subtitle: String,
    val artist: String,
    val published: String,
    val genre: String,
    val tracks: List<Track> = emptyList()
)
