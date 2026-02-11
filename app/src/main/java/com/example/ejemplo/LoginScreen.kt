package com.example.ejemplo

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.components.NoInternetScreen
import com.example.ejemplo.utils.NetworkUtils
import com.example.ejemplo.data.LoginRequest
import com.example.ejemplo.data.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit // <--- NUEVO PARAMETRO
) {

    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }

    val checkConnection = {
        val connected = NetworkUtils.isNetworkAvailable(context)
        isConnected = connected
        if (!connected) {
            Toast.makeText(context, "Sigues sin conexión...", Toast.LENGTH_SHORT).show()
        }
    }

    if (!isConnected) {
        NoInternetScreen(
            onRetry = { checkConnection() },
            onBack = { (context as? android.app.Activity)?.finish() }
        )
        return
    }

    val DarkBlue = Color(0xFF1A2B46)
    val ButtonBlue = Color(0xFF1A2B46)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.urg),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Credenciales Institucionales",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = DarkBlue,
                            cursorColor = DarkBlue,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Ver contraseña")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = DarkBlue,
                            cursorColor = DarkBlue,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                isConnected = false
                                return@Button
                            }

                            if (email.isEmpty() || password.isEmpty()) {
                                Toast.makeText(context, "Complete los campos", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val request = LoginRequest(email, password)
                                        val response = RetrofitClient.api.login(request)
                                        isLoading = false
                                        Toast.makeText(context, "Bienvenido ${response.user.nombre}", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(response.access_token, response.user.nombre)
                                    } catch (e: Exception) {
                                        isLoading = false
                                        Toast.makeText(context, "Error: Credenciales inválidas", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(text = "INICIAR SESIÓN", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- NUEVO BOTÓN DE REGISTRO ---
                    TextButton(onClick = onNavigateToRegister) {
                        Text("¿No tienes cuenta? Regístrate aquí", color = DarkBlue)
                    }
                }
            }
        }
    }
}