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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    // Función que consume la API de Radio Browser
    private suspend fun fetchStations(): List<Station> = withContext(Dispatchers.IO) {
        val stationList = mutableListOf<Station>()
        try {
            val url = URL("https://all.api.radio-browser.info/json/stations/bycountry/colombia?limit=20")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseString)

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val name = jsonObject.optString("name", "Emisora Desconocida").trim()
                    val urlResolved = jsonObject.optString("url_resolved", "")
                    val country = jsonObject.optString("country", "Desconocido")
                    val favicon = jsonObject.optString("favicon", "")

                    if (urlResolved.isNotBlank()) {
                        stationList.add(Station(name, urlResolved, country, favicon))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext stationList
    }

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

            // Lista dinámica de emisoras desde la API
            var stationList by remember { mutableStateOf<List<Station>>(emptyList()) }
            var currentStationIndex by remember { mutableStateOf(0) }
            var currentStationName by remember { mutableStateOf("Ninguna seleccionada") }
            var isPlaying by remember { mutableStateOf(false) }
            var isLoading by remember { mutableStateOf(false) }

            // Cargar estaciones automáticamente al iniciar la app
            LaunchedEffect(Unit) {
                isLoading = true
                stationList = fetchStations()
                isLoading = false
            }

            // Función para reproducir emisora por streaming URL
            val playStationAtIndex: (Int) -> Unit = { index ->
                if (stationList.isNotEmpty()) {
                    currentStationIndex = index
                    val station = stationList[currentStationIndex]
                    try {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(station.url_resolved)
                            prepareAsync()
                            setOnPreparedListener { mp ->
                                mp.start()
                                isPlaying = true
                            }
                        }
                        currentStationName = station.name
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isPlaying = false
                    }
                }
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

                    // Título
                    Text(
                        text = "Radio Digital",
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

                        // lista de emisoras de la API
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
                                    text = "Emisoras en Vivo (API)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isLoading) {
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = y2kPink)
                                    }
                                } else {
                                    // Lista dinámica
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(stationList) { index, station ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        triggerHaptic()
                                                        playStationAtIndex(index)
                                                    },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (currentStationIndex == index && isPlaying) y2kPink.copy(alpha = 0.2f) else Color.White
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
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = station.name, color = Color.DarkGray, fontWeight = FontWeight.Medium, maxLines = 1)
                                                        Text(text = station.country ?: "", color = Color.Gray, fontSize = 11.sp)
                                                    }
                                                    Text(
                                                        text = if (currentStationIndex == index && isPlaying) "Sonando..." else "Play",
                                                        color = y2kPink,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Divider(color = Color.Gray, thickness = 1.dp)

                                Spacer(modifier = Modifier.height(8.dp))

                                // Reproductor actual y controles
                                Text(
                                    text = if (isPlaying) "Sonando: $currentStationName" else "Estado: Pausado",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray,
                                    maxLines = 1
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

                                    // Botón Siguiente con loop circular
                                    Button(
                                        onClick = {
                                            triggerHaptic()
                                            if (stationList.isNotEmpty()) {
                                                val nextIndex = (currentStationIndex + 1) % stationList.size
                                                playStationAtIndex(nextIndex)
                                            }
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