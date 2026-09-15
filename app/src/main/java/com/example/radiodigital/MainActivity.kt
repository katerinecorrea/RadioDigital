package com.example.radiodigital

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Colores
            val bgColor = Color(0xFF0D0D0D)      // Negro
            val cardColor = Color(0xFFE0E0E0)    // Plateado
            val fucsiaColor = Color(0xFFD27794)  // Rosa fucsia

            // Obtenemos el contexto correctamente en la raíz del Composable
            val context = LocalContext.current

            // Estado para almacenar la foto que toma la cámara
            var capturedImage by remember { mutableStateOf<Bitmap?>(null) }

            // Lanzador para abrir la cámara nativa
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap ->
                if (bitmap != null) {
                    capturedImage = bitmap
                }
            }

            // Lanzador para solicitar el permiso de cámara
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    // Si el usuario acepta, abre la cámara
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
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = fucsiaColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tarjeta principal con el perfil y la cámara
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Avatar Circular Y2K
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, fucsiaColor, CircleShape)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (capturedImage != null) {
                                    // Muestra la foto real tomada con la cámara
                                    Image(
                                        bitmap = capturedImage!!.asImageBitmap(),
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Texto por defecto si aún no hay foto
                                    Text(
                                        text = "FOTO",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Botón para abrir la cámara
                            Button(
                                onClick = {
                                    // Verifica si ya tiene el permiso de la cámara
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            cameraLauncher.launch(null)
                                        }
                                        else -> {
                                            // Si no lo tiene se pide
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = fucsiaColor)
                            ) {
                                Text(
                                    text = " Tomar foto de perfil",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}