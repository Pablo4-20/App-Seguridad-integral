package com.example.ejemplo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.data.ResetPasswordRequest
import com.example.ejemplo.data.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(token: String, email: String, onSuccess: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val DarkBlue = Color(0xFF1A2B46)

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBlue).padding(24.dp).imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crear Nueva Contraseña", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Para el correo: $email", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = if (it.isEmpty() || isValidPassword(it)) null else "Mínimo 8 caracteres, 1 mayúscula, 1 número y 1 símbolo"
                    },
                    label = { Text("Nueva Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = { if (passwordError != null) Text(text = passwordError!!, color = MaterialTheme.colorScheme.error) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "Ver contraseña")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val request = ResetPasswordRequest(email, token, password)
                                val response = RetrofitClient.api.resetPassword(request)
                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                                onSuccess() // Volver al Login
                            } catch (e: HttpException) {
                                Toast.makeText(context, "El enlace de recuperación es inválido o ya expiró.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error de red. Verifica tu conexión.", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    enabled = isValidPassword(password) && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("GUARDAR CONTRASEÑA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}