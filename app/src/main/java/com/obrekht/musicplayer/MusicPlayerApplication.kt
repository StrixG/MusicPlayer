package com.obrekht.musicplayer

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.obrekht.musicplayer.data.InMemoryAlbumRepository
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient

class MusicPlayerApplication : Application() {

    val albumRepository by lazy {
        InMemoryAlbumRepository(OkHttpClient(), Moshi.Builder().build())
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
