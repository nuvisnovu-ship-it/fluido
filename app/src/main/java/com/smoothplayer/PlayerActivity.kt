package com.smoothplayer

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.smoothplayer.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var videoUri: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        videoUri = intent.getStringExtra("VIDEO_URI") ?: ""
        if (videoUri.isEmpty()) { finish(); return }

        // Detectar chip y mostrar info
        val chipInfo = detectChip()
        Toast.makeText(this, "⚡ Optimizado para: $chipInfo", Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        acquireWakeLock()
        initPlayer()
        forceHighRefreshRate()
    }

    private fun detectChip(): String {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val model = Build.MODEL.uppercase()

        return when {
            hardware.contains("exynos") || board.contains("exynos") -> "Samsung Exynos"
            hardware.contains("qcom") || board.contains("taro") ||
            board.contains("lahaina") || board.contains("kona") -> "Snapdragon"
            hardware.contains("mt") || board.contains("mt") -> "MediaTek"
            hardware.contains("kirin") -> "Huawei Kirin"
            hardware.contains("tensor") -> "Google Tensor"
            else -> "${Build.HARDWARE} (Auto)"
        }
    }

    private fun initPlayer() {
        // Renderizado con extensiones de hardware habilitadas
        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        // Track selector — preferir calidad máxima
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
            )
        }

        // Buffer optimizado para fluidez máxima
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2_000,   // min buffer
                60_000,  // max buffer
                1_000,   // buffer antes de reproducir
                2_000    // buffer al rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .also { exo ->
                binding.playerView.player = exo
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

                // Modo de escalado de video — máxima calidad
                exo.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT

                // Listener para ajustar refresh rate al video
                exo.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        forceHighRefreshRate()
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            forceHighRefreshRate()
                        }
                    }
                })

                val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun forceHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = windowManager.defaultDisplay
            val modes = display.supportedModes
            
            // Ordenar por refresh rate descendente y elegir el más alto
            val bestMode = modes.maxByOrNull { it.refreshRate }
            
            bestMode?.let { mode ->
                val params = window.attributes
                params.preferredDisplayModeId = mode.modeId
                window.attributes = params
                
                // Para Android 11+ usar método más moderno
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.attributes.preferredRefreshRate = mode.refreshRate
                }
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "SmoothPlayer::VideoLock"
        )
        wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 horas max
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
        wakeLock?.release()
    }
}
