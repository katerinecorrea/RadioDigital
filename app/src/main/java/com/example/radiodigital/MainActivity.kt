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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class Song(val title: String, val resourceId: Int)

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val bgColor = Color(0xFF0D0D0D)    // Negro
            val cardColor = Color(0xFFE0E0E0)  // Plateado
            val y2kPink = Color(0xFFD27794)    // Rosado

            val context = LocalContext.current

            // Función de vibración háptica
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

            // Estados de la app
            var usernameInput by remember { mutableStateOf("") }
            var isProfileConfigured by remember { mutableStateOf(false) }
            var capturedImage by remember { mutableStateOf<Bitmap?>(null) }

            // Lista de canciones
            val songList = listOf(
                Song("Chill Track", R.raw.cancion1),
                Song("Club Music", R.raw.cancion2),
                Song("Water Pop", R.raw.cancion3),
                Song("Sinfonía", R.raw.cancion4),
            )

            var currentSongIndex by remember { mutableStateOf(0) }
            var currentSongTitle by remember { mutableStateOf("Ninguna seleccionada") }
            var isPlaying by remember { mutableStateOf(false) }

            // Función auxiliar para reproducir una canción por su índice
            val playSongAtIndex: (Int) -> Unit = { index ->
                currentSongIndex = index
                val song = songList[currentSongIndex]
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(context, song.resourceId)
                mediaPlayer?.start()
                currentSongTitle = song.title
                isPlaying = true
            }

            // Lanzadores de cámara y permisos
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap ->
                if (bitmap != null) {
                    capturedImage = bitmap
                }
            }

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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Título Y2K
                    Text(
                        text = "Radio Digital Y2K",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = y2kPink
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // vista inicial de perfil
                    if (!isProfileConfigured) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Configura tu Perfil",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )

                                OutlinedTextField(
                                    value = usernameInput,
                                    onValueChange = { usernameInput = it },
                                    label = { Text("Nombre de Usuario") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Avatar Preview
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, y2kPink, CircleShape)
                                        .background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (capturedImage != null) {
                                        Image(
                                            bitmap = capturedImage!!.asImageBitmap(),
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(text = "FOTO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        when {
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                                                cameraLauncher.launch(null)
                                            }
                                            else -> {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = y2kPink)
                                ) {
                                    Text(text = "Tomar foto de perfil", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        if (usernameInput.isNotBlank()) {
                                            isProfileConfigured = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Entrar a la radio", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // tarjeta de perfil superior
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, y2kPink, CircleShape)
                                        .background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (capturedImage != null) {
                                        Image(
                                            bitmap = capturedImage!!.asImageBitmap(),
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(text = "USR", color = Color.White, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = usernameInput.uppercase(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // lista de canciones
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Pistas",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Lista dinámica de canciones con índice
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(songList) { index, song ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    triggerHaptic()
                                                    playSongAtIndex(index)
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (currentSongIndex == index && isPlaying) y2kPink.copy(alpha = 0.2f) else Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = song.title, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                                Text(text = if (currentSongIndex == index && isPlaying) "Sonando..." else "Play", color = y2kPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Divider(color = Color.Gray, thickness = 1.dp)

                                Spacer(modifier = Modifier.height(8.dp))

                                // Reproductor actual y controles
                                Text(
                                    text = if (isPlaying) "Sonando: $currentSongTitle" else "Estado: Pausado",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Fila de Botones (Play, Pause, Siguiente)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = {
                                            triggerHaptic()
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = y2kPink)
                                    ) {
                                        Text(text = "Play", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

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

                                    // Botón Siguiente (con loop circular)
                                    Button(
                                        onClick = {
                                            triggerHaptic()
                                            val nextIndex = (currentSongIndex + 1) % songList.size
                                            playSongAtIndex(nextIndex)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = y2kPink)
                                    ) {
                                        Text(text = "Siguiente", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
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
        mediaPlayer?.release()
        mediaPlayer = null
    }
}