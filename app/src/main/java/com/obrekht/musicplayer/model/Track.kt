package com.obrekht.musicplayer.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Track(
    val id: Long,
    val file: String
)
