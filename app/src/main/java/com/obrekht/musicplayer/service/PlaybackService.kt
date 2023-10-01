package com.obrekht.musicplayer.service

import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build().also {
                mediaSession = MediaSession.Builder(this, it).build()
            }
    }

    override fun onGetSession(controllerInfo: ControllerInfo) = mediaSession

    override fun onDestroy() {
        player?.release()
        mediaSession?.release()
        player = null
        mediaSession = null
        super.onDestroy()
    }
}
