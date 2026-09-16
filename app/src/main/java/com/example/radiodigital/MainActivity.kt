package com.example.radiodigital

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos el reproductor con tu archivo de audio (asegúrate de que coincida con el nombre que dejaste en raw, sin .mp3)
        mediaPlayer = MediaPlayer.create(this, R.raw.cancion1)

        setContent {
            // Colores
            val bgColor = Color(0xFF0D0D0D)      // Negro
            val cardColor = Color(0xFFE0E0E0)    // Plateado
            val y2kPink = Color(0xFFD27794)      // Rosado

            val context = LocalContext.current

            // Función para vibración háptica
            val triggerHaptic = {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }

            // Estado para la foto de perfil y el estado de reproducción
            var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
            var isPlaying by remember { mutableStateOf(false) }

            // Lanzador de cámara
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap ->
                if (bitmap != null) {
                    capturedImage = bitmap
                }
            }

            // Lanzador de permisos de cámara
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    cameraLauncher.launch(null)
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = bgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Título principal
                    Text(
                        text = " Radio Digital ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = y2kPink
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tarjeta principal (Perfil + Controles de Audio)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
                            // Avatar Circular
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, y2kPink, CircleShape)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (capturedImage != null) {
                                    Image(
                                        bitmap = capturedImage!!.asImageBitmap(),
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = "FOTO",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Botón de Cámara
                            Button(
                                onClick = {
                                    triggerHaptic()
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            cameraLauncher.launch(null)
                                        }
                                        else -> {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = y2kPink),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(text = " Tomar Foto", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                            // Controles de Reproducción de Audio
                            Text(
                                text = if (isPlaying) " Reproduciendo..." else " Pausado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Botón Play
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = y2kPink)
                                ) {
                                    Text(text = " Play", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                // Botón Pause
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) {
                                    Text(text = "Pause", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberamos memoria del reproductor al cerrar la app
        mediaPlayer?.release()
        mediaPlayer = null
    }
}