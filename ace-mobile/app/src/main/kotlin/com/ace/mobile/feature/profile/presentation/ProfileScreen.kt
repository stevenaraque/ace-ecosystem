package com.ace.mobile.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                ProfileEvent.NavigateToLogin -> {
                    navController.navigate("login_screen_route") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            val state = uiState as ProfileUiState.Success
            if (state.saveSuccess) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearSaveSuccess()
            }
            state.saveError?.let {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSaveError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->

        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadProfile() },
                    onLogout = { viewModel.logout() }
                )
            }

            is ProfileUiState.Success -> {
                ProfileContent(
                    state = state,
                    padding = padding,
                    onEdit = { viewModel.startEditing() },
                    onCancel = { viewModel.cancelEditing() },
                    onSave = { viewModel.saveProfile() },
                    onFieldChange = { field, value -> viewModel.updateField(field, value) },
                    onLogout = { viewModel.logout() }
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Success,
    padding: PaddingValues,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onFieldChange: (ProfileField, String) -> Unit,
    onLogout: () -> Unit
) {
    val profile = state.profile
    val isEditing = state.isEditing
    val isSaving = state.isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Avatar",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = profile.email ?: "Sin email",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "ID: ${profile.userId.take(8)}...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Campos editables
        ProfileTextField(
            label = "Nombre de usuario",
            value = profile.username ?: "",
            onValueChange = { onFieldChange(ProfileField.USERNAME, it) },
            enabled = isEditing && !isSaving,
            icon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileTextField(
            label = "Nickname",
            value = profile.nickname ?: "",
            onValueChange = { onFieldChange(ProfileField.NICKNAME, it) },
            enabled = isEditing && !isSaving,
            icon = Icons.Default.Face
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ← CitySelector con constantes
        CitySelector(
            selectedCityId = profile.cityId,
            onCitySelected = { cityId -> onFieldChange(ProfileField.CITY_ID, cityId) },
            enabled = isEditing && !isSaving
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileTextField(
            label = "Peso (kg)",
            value = profile.weightKg?.toString() ?: "",
            onValueChange = { onFieldChange(ProfileField.WEIGHT_KG, it) },
            enabled = isEditing && !isSaving,
            icon = Icons.Default.Star,
            keyboardType = KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ← BirthDatePicker con DatePickerDialog
        BirthDatePicker(
            birthDate = profile.birthDate,
            onDateSelected = { onFieldChange(ProfileField.BIRTH_DATE, it) },
            enabled = isEditing && !isSaving
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mensajes de feedback
        if (state.saveSuccess) {
            Text(
                text = "✓ Perfil guardado correctamente",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        state.saveError?.let { error ->
            Text(
                text = "✗ $error",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Botones de acción
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            }
        } else {
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar perfil")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A.C.E v1.0.9",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitySelector(
    selectedCityId: String?,
    onCitySelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCityName = CityConstants.getDisplayName(selectedCityId)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCityName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ciudad") },
            enabled = enabled,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()

        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CityConstants.CITIES.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.displayName) },
                    onClick = {
                        onCitySelected(city.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePicker(
    birthDate: String?,
    onDateSelected: (String) -> Unit,
    enabled: Boolean
) {
    var showPicker by remember { mutableStateOf(false) }

    val currentDateMillis = remember(birthDate) {
        try {
            birthDate?.let {
                java.time.LocalDate.parse(it).toEpochDay() * 24 * 60 * 60 * 1000
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    OutlinedTextField(
        value = birthDate ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Fecha de nacimiento") },
        enabled = enabled,
        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { if (enabled) showPicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = java.time.Instant
                                .ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            val formatted = localDate.toString() // YYYY-MM-DD
                            onDateSelected(formatted)
                        }
                        showPicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onLogout) {
            Text("Cerrar sesión")
        }
    }
}