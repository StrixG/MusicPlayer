package com.obrekht.musicplayer.data

import com.obrekht.musicplayer.model.Album
import com.obrekht.musicplayer.model.Track
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val BASE_URL =
    "https://raw.githubusercontent.com/netology-code/andad-homeworks/master/09_multimedia/data/"

@ExperimentalStdlibApi
class InMemoryAlbumRepository(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) : AlbumRepository {

    private val albumJsonAdapter = moshi.adapter<Album>()
    private val albumList = mutableListOf<Album>()
    private val trackList = mutableListOf<Track>()

    override fun getAll() = albumList.toList()

    override fun getAlbumById(albumId: Long): Album? =
        albumList.find { it.id == albumId }

    override fun addAlbum(album: Album) {
        albumList.add(album)
        trackList.addAll(album.tracks)
    }

    override fun getTrackUrl(trackId: Long): String? {
        return trackList.find { it.id == trackId }?.let {
            return "$BASE_URL/${it.file}"
        }
    }

    override suspend fun refreshAlbums() {
        val request = Request.Builder()
            .url("$BASE_URL/album.json")
            .build()

        suspendCoroutine { cont ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    cont.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        if (response.body == null) throw IOException("Response body is empty")

                        albumJsonAdapter.fromJson(response.body!!.source())?.let {
                            albumList.add(it)
                            trackList.addAll(it.tracks)
                        }

                        cont.resume(null)
                    }
                }
            })
        }
    }
}
