package com.smoothplayer

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.smoothplayer.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var videoUri: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pantalla completa y siempre encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        videoUri = intent.getStringExtra("VIDEO_URI") ?: ""
        if (videoUri.isEmpty()) { finish(); return }
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    private fun initPlayer() {
        // Selector de pistas optimizado para calidad
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        // Control de buffer optimizado para fluidez
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,   // min buffer
                30_000,  // max buffer
                1_500,   // buffer antes de reproducir
                3_000    // buffer al rebuffer
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .also { exo ->

                // Conectar al PlayerView
                binding.playerView.player = exo
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

                // Activar decodificación por hardware (clave para suavizado)
                exo.videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

                // Cargar el video
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.playWhenReady = true

                // Listener para ajustar el frame rate del display al video
                exo.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        super.onVideoSizeChanged(videoSize)
                        adjustDisplayRefreshRate(videoSize)
                    }
                })
            }
    }

    // Ajusta el refresh rate de la pantalla al frame rate del video
    private fun adjustDisplayRefreshRate(videoSize: VideoSize) {
        try {
            val display = windowManager.defaultDisplay
            val modes = display.supportedModes
            // Preferir 60fps si está disponible
            val best = modes.maxByOrNull { it.refreshRate }
            best?.let {
                val params = window.attributes
                params.preferredDisplayModeId = it.modeId
                window.attributes = params
            }
        } catch (e: Exception) {
            // Dispositivo no soporta cambio de refresh rate — continúa normal
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}
