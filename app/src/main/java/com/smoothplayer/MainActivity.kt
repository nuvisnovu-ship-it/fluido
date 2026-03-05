package com.smoothplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.smoothplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { openPlayer(it.toString()) }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickVideo.launch("video/*")
        else Toast.makeText(this, "Permiso necesario para acceder a videos", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón: abrir video local
        binding.btnLocal.setOnClickListener {
            checkPermissionAndPick()
        }

        // Botón: ingresar URL / IPTV
        binding.btnUrl.setOnClickListener {
            showUrlDialog()
        }

        // Info del suavizado
        binding.btnInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚡ Suavizado a 60fps")
                .setMessage(
                    "SmoothPlayer usa el decodificador de hardware de tu celular " +
                    "para reproducir video de la forma más fluida posible.\n\n" +
                    "• Decodificación por hardware activada\n" +
                    "• Sincronización de frames optimizada\n" +
                    "• Interpolación de tiempo entre frames\n" +
                    "• Soporte MP4, MKV, M3U8, IPTV"
                )
                .setPositiveButton("Entendido", null)
                .show()
        }
    }

    private fun checkPermissionAndPick() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_VIDEO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickVideo.launch("video/*")
        } else {
            requestPermission.launch(permission)
        }
    }

    private fun showUrlDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "https://... o rtmp://..."
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("🌐 URL del video o stream")
            .setView(input)
            .setPositiveButton("Reproducir") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) openPlayer(url)
                else Toast.makeText(this, "Ingresa una URL válida", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openPlayer(uriString: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("VIDEO_URI", uriString)
        }
        startActivity(intent)
    }
}
