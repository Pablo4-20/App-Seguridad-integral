package com.example.ejemplo

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.data.RegisterRequest
import com.example.ejemplo.data.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Funciones de validación local
fun isValidCedula(cedula: String): Boolean {
    if (cedula.length != 10 || !cedula.all { it.isDigit() }) return false
    val provinceCode = cedula.substring(0, 2).toIntOrNull() ?: return false
    if (provinceCode < 1 || provinceCode > 24) return false
    val thirdDigit = cedula[2].toString().toIntOrNull() ?: return false
    if (thirdDigit >= 6) return false

    val coefficients = intArrayOf(2, 1, 2, 1, 2, 1, 2, 1, 2)
    var sum = 0
    for (i in 0 until 9) {
        var product = cedula[i].toString().toInt() * coefficients[i]
        if (product >= 10) product -= 9
        sum += product
    }
    val lastDigit = cedula[9].toString().toInt()
    val calculatedLastDigit = if (sum % 10 == 0) 0 else 10 - (sum % 10)
    return lastDigit == calculatedLastDigit
}

fun isValidEmail(email: String): Boolean {
    return email.trim().endsWith("@ueb.edu.ec")
}

fun isValidPassword(password: String): Boolean {
    val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}\$".toRegex()
    return passwordRegex.matches(password)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Estados del formulario
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    // Estados de error
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var cedulaError by remember { mutableStateOf<String?>(null) }
    var telefonoError by remember { mutableStateOf<String?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val DarkBlue = Color(0xFF1A2B46)

    // Función auxiliar para borrar todo el formulario instantáneamente
    val clearForm = {
        name = ""
        email = ""
        password = ""
        cedula = ""
        telefono = ""
        emailError = null
        cedulaError = null
        passwordError = null
        telefonoError = null
    }

    // --- VALIDACIÓN EN TIEMPO REAL CON EL SERVIDOR (AHORA ACTIVADA) ---

    // 1. Validar Correo en tiempo real
    LaunchedEffect(email) {
        if (email.isNotBlank() && isValidEmail(email)) {
            delay(800) // Espera 800ms para no saturar el servidor
            try {
                // LLAMADA REAL A LA API DESCOMENTADA
                RetrofitClient.api.checkEmail(email.trim())
                emailError = null
            } catch (e: HttpException) {
                if (e.code() == 422 || e.code() == 409) {
                    Toast.makeText(context, "Este correo ya está registrado.", Toast.LENGTH_LONG).show()
                    clearForm()
                }
            } catch (e: Exception) {
                // Ignoramos errores de red momentáneos mientras escribe
            }
        }
    }

    // 2. Validar Cédula en tiempo real
    LaunchedEffect(cedula) {
        if (cedula.length == 10 && isValidCedula(cedula)) {
            delay(800)
            try {
                // LLAMADA REAL A LA API DESCOMENTADA
                RetrofitClient.api.checkCedula(cedula)
                cedulaError = null
            } catch (e: HttpException) {
                if (e.code() == 422 || e.code() == 409) {
                    Toast.makeText(context, "Esta cédula ya está registrada.", Toast.LENGTH_LONG).show()
                    clearForm()
                }
            } catch (e: Exception) {
                // Ignoramos errores de red momentáneos
            }
        }
    }

    // 3. Validar Teléfono en tiempo real
    LaunchedEffect(telefono) {
        if (telefono.length == 10) {
            delay(800)
            try {
                // LLAMADA REAL A LA API DESCOMENTADA
                RetrofitClient.api.checkTelefono(telefono)
                telefonoError = null
            } catch (e: HttpException) {
                if (e.code() == 422 || e.code() == 409) {
                    Toast.makeText(context, "Este teléfono ya está registrado.", Toast.LENGTH_LONG).show()
                    clearForm()
                }
            } catch (e: Exception) {
                // Ignoramos errores de red momentáneos
            }
        }
    }

    // Evaluar que todo esté correcto para habilitar el botón final
    val isFormValid = name.isNotBlank() &&
            telefono.length == 10 &&
            isValidEmail(email) &&
            isValidCedula(cedula) &&
            isValidPassword(password) &&
            emailError == null && cedulaError == null && telefonoError == null

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBlue).imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
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
                    Text("Crear Cuenta", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Nombre
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = if (it.isEmpty() || isValidEmail(it)) null else "El correo debe terminar en @ueb.edu.ec"
                        },
                        label = { Text("Correo Institucional") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true, isError = emailError != null,
                        supportingText = { if (emailError != null) Text(text = emailError!!, color = MaterialTheme.colorScheme.error) }
                    )

                    // Cédula
                    OutlinedTextField(
                        value = cedula,
                        onValueChange = {
                            if (it.length <= 10) {
                                cedula = it
                                cedulaError = if (it.isEmpty() || isValidCedula(it)) null else "Cédula inválida"
                            }
                        },
                        label = { Text("Cédula") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, isError = cedulaError != null,
                        supportingText = { if (cedulaError != null) Text(text = cedulaError!!, color = MaterialTheme.colorScheme.error) }
                    )

                    // Teléfono
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { if (it.length <= 10) telefono = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true, isError = telefonoError != null,
                        supportingText = { if (telefonoError != null) Text(text = telefonoError!!, color = MaterialTheme.colorScheme.error) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = if (it.isEmpty() || isValidPassword(it)) null else "Mínimo 8 caracteres, 1 may, 1 num y 1 símb"
                        },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true, isError = passwordError != null,
                        supportingText = { if (passwordError != null) Text(text = passwordError!!, color = MaterialTheme.colorScheme.error) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Ver pass")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                try {
                                    val request = RegisterRequest(name, email.trim(), password, cedula, telefono)
                                    RetrofitClient.api.register(request)

                                    Toast.makeText(context, "Registro exitoso. Se ha enviado un correo de verificación.", Toast.LENGTH_LONG).show()
                                    onBack()

                                } catch (e: HttpException) {
                                    if (e.code() == 422 || e.code() == 409) {
                                        Toast.makeText(context, "Los datos ya existen. Formulario reiniciado.", Toast.LENGTH_LONG).show()
                                        clearForm()
                                    } else {
                                        Toast.makeText(context, "Error en el servidor. Intente más tarde.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Verifique su conexión a internet.", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        enabled = isFormValid && !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("REGISTRARSE", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onBack) { Text("¿Ya tienes cuenta? Inicia sesión", color = DarkBlue) }
                }
            }
        }
    }
}