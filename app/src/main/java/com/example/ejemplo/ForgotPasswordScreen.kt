package com.example.ejemplo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.data.ForgotPasswordRequest
import com.example.ejemplo.data.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    var cedula by remember { mutableStateOf("") }
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
                Text("Recuperar Contraseña", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Ingresa tu número de cédula. Si existe en el sistema, te enviaremos un correo con un enlace para cambiar tu contraseña.",
                    color = Color.DarkGray, fontSize = 14.sp, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = cedula,
                    onValueChange = { if (it.length <= 10) cedula = it },
                    label = { Text("Cédula") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (cedula.length == 10 && isValidCedula(cedula)) {
                            isLoading = true
                            scope.launch {
                                try {
                                    val response = RetrofitClient.api.forgotPassword(ForgotPasswordRequest(cedula))
                                    Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                                    onBack() // Volver al login después de enviar
                                } catch (e: HttpException) {
                                    if (e.code() == 404) {
                                        Toast.makeText(context, "No se encontró un usuario con esta cédula.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Error en el servidor. Intente más tarde.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error de conexión. Verifica tu internet.", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Ingrese una cédula ecuatoriana válida de 10 dígitos.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading && cedula.isNotBlank()
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("ENVIAR ENLACE", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onBack) { Text("Cancelar y volver", color = DarkBlue) }
            }
        }
    }
}