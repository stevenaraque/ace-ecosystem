package com.ace.mobile.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ace.mobile.core.ui.components.AceBottomNav
import com.ace.mobile.core.ui.components.AceButtonFilled
import com.ace.mobile.core.ui.components.AceButtonOutlined
import com.ace.mobile.core.ui.components.AceTab
import com.ace.mobile.core.ui.theme.AceColors
import com.ace.mobile.core.ui.theme.AceTypography
import com.ace.mobile.feature.auth.presentation.AuthBackground
import kotlinx.coroutines.delay

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
                delay(2000)
                viewModel.clearSaveSuccess()
            }
            state.saveError?.let {
                delay(3000)
                viewModel.clearSaveError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PERFIL",
                        style = AceTypography.H2.copy(
                            fontSize = 18.sp,
                            color = AceColors.TextPrimary
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AceColors.BgBlack,
                    scrolledContainerColor = AceColors.BgBlack
                )
            )
        },
        bottomBar = {
            AceBottomNav(
                selectedTab = AceTab.STATS,
                onTabSelected = { tab ->
                    when (tab) {
                        AceTab.HOME -> navController.navigate("home_screen_route") {
                            popUpTo("home_screen_route") { inclusive = true }
                        }
                        AceTab.EXERCISE -> navController.navigate("session_screen_route")
                        AceTab.RANKING -> navController.navigate("ranking_screen_route")
                        AceTab.STATS -> {  }
                        AceTab.PROFILE -> navController.navigate("profile_screen_route") // <─── Agrega esto
                    }
                }
            )
        },
        containerColor = AceColors.BgBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AuthBackground()

            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AceColors.NeonRed,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(40.dp)
                        )
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
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ── Avatar ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AceColors.CardBg)
                .border(2.dp, AceColors.NeonRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (profile.nickname ?: profile.username ?: "U").take(1).uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AceColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Nickname ───────────────────────────────────────────────────
        Text(
            text = profile.nickname ?: profile.username ?: "Usuario",
            style = AceTypography.H1.copy(
                fontSize = 18.sp,
                color = AceColors.TextPrimary
            )
        )

        // ── Email ──────────────────────────────────────────────────────
        Text(
            text = profile.email ?: "Sin email",
            fontSize = 12.sp,
            color = AceColors.TextMuted
        )

        // ── Ciudad ─────────────────────────────────────────────────────
        Text(
            text = CityConstants.getDisplayName(profile.cityId),
            fontSize = 11.sp,
            color = AceColors.NeonRed.copy(alpha = 0.75f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(thickness = 0.5.dp, color = AceColors.BorderDim)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Campos editables ───────────────────────────────────────────

        ProfileTextFieldAce(
            label = "Nombre de usuario",
            value = profile.username ?: "",
            onValueChange = { onFieldChange(ProfileField.USERNAME, it) },
            enabled = isEditing && !isSaving
        )

        Spacer(modifier = Modifier.height(14.dp))

        ProfileTextFieldAce(
            label = "Nickname",
            value = profile.nickname ?: "",
            onValueChange = { onFieldChange(ProfileField.NICKNAME, it) },
            enabled = isEditing && !isSaving
        )

        Spacer(modifier = Modifier.height(14.dp))

        CitySelectorAce(
            selectedCityId = profile.cityId,
            onCitySelected = { cityId -> onFieldChange(ProfileField.CITY_ID, cityId) },
            enabled = isEditing && !isSaving
        )

        Spacer(modifier = Modifier.height(14.dp))

        ProfileTextFieldAce(
            label = "Peso (kg)",
            value = profile.weightKg?.toString() ?: "",
            onValueChange = { onFieldChange(ProfileField.WEIGHT_KG, it) },
            enabled = isEditing && !isSaving,
            keyboardType = KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(14.dp))

        BirthDatePickerAce(
            birthDate = profile.birthDate,
            onDateSelected = { onFieldChange(ProfileField.BIRTH_DATE, it) },
            enabled = isEditing && !isSaving
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Mensajes de feedback ───────────────────────────────────────
        if (state.saveSuccess) {
            Text(
                text = "✓ Perfil guardado correctamente",
                color = AceColors.SuccessGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        state.saveError?.let { error ->
            Text(
                text = "✗ $error",
                color = AceColors.NeonRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // ── Botones de acción ──────────────────────────────────────────
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AceButtonOutlined(
                    text = "CANCELAR",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !isSaving
                )
                AceButtonFilled(
                    text = "GUARDAR",
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(48.dp),
                    isLoading = isSaving,
                    textStyle = AceTypography.H1.copy(
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                )
            }
        } else {
            AceButtonFilled(
                text = "EDITAR PERFIL",
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                textStyle = AceTypography.H1.copy(
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider(thickness = 0.5.dp, color = AceColors.BorderDim)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Cerrar sesión ──────────────────────────────────────────────
        AceButtonOutlined(
            text = "CERRAR SESIÓN",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "A.C.E v1.0.9",
            fontSize = 11.sp,
            color = AceColors.TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── TextField estilizado A.C.E ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTextFieldAce(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                fontSize = 13.sp,
                color = if (enabled) AceColors.NeonRed else AceColors.TextMuted
            )
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AceColors.NeonRed,
            unfocusedBorderColor = AceColors.BorderDim,
            focusedLabelColor = AceColors.NeonRed,
            unfocusedLabelColor = AceColors.TextMuted,
            focusedTextColor = Color.White,
            unfocusedTextColor = AceColors.TextSecondary,
            cursorColor = AceColors.NeonRed,
            focusedContainerColor = Color(0xFF100808),
            unfocusedContainerColor = Color(0xFF0A0A0A),
            disabledBorderColor = AceColors.BorderDim,
            disabledTextColor = AceColors.TextMuted,
            disabledLabelColor = AceColors.TextMuted,
            disabledContainerColor = Color(0xFF0A0A0A)
        )
    )
}

// ─── City Selector estilizado A.C.E ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitySelectorAce(
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
            label = {
                Text(
                    "Ciudad",
                    fontSize = 13.sp,
                    color = if (enabled) AceColors.NeonRed else AceColors.TextMuted
                )
            },
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AceColors.NeonRed,
                unfocusedBorderColor = AceColors.BorderDim,
                focusedLabelColor = AceColors.NeonRed,
                unfocusedLabelColor = AceColors.TextMuted,
                focusedTextColor = Color.White,
                unfocusedTextColor = AceColors.TextSecondary,
                cursorColor = AceColors.NeonRed,
                focusedContainerColor = Color(0xFF100808),
                unfocusedContainerColor = Color(0xFF0A0A0A),
                disabledBorderColor = AceColors.BorderDim,
                disabledTextColor = AceColors.TextMuted,
                disabledLabelColor = AceColors.TextMuted,
                disabledContainerColor = Color(0xFF0A0A0A)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AceColors.CardBg)
        ) {
            CityConstants.CITIES.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Text(
                            city.displayName,
                            color = AceColors.TextPrimary
                        )
                    },
                    onClick = {
                        onCitySelected(city.id)
                        expanded = false
                    },
                    colors = MenuItemColors(
                        textColor = AceColors.TextPrimary,
                        leadingIconColor = AceColors.TextSecondary,
                        trailingIconColor = AceColors.TextSecondary,
                        disabledTextColor = AceColors.TextMuted,
                        disabledLeadingIconColor = AceColors.TextMuted,
                        disabledTrailingIconColor = AceColors.TextMuted
                    )
                )
            }
        }
    }
}

// ─── Birth Date Picker estilizado A.C.E ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerAce(
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
        label = {
            Text(
                "Fecha de nacimiento",
                fontSize = 13.sp,
                color = if (enabled) AceColors.NeonRed else AceColors.TextMuted
            )
        },
        enabled = enabled,
        trailingIcon = {
            IconButton(onClick = { if (enabled) showPicker = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Seleccionar fecha",
                    tint = if (enabled) AceColors.NeonRed else AceColors.TextMuted
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AceColors.NeonRed,
            unfocusedBorderColor = AceColors.BorderDim,
            focusedLabelColor = AceColors.NeonRed,
            unfocusedLabelColor = AceColors.TextMuted,
            focusedTextColor = Color.White,
            unfocusedTextColor = AceColors.TextSecondary,
            cursorColor = AceColors.NeonRed,
            focusedContainerColor = Color(0xFF100808),
            unfocusedContainerColor = Color(0xFF0A0A0A),
            disabledBorderColor = AceColors.BorderDim,
            disabledTextColor = AceColors.TextMuted,
            disabledLabelColor = AceColors.TextMuted,
            disabledContainerColor = Color(0xFF0A0A0A)
        )
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
                            onDateSelected(localDate.toString())
                        }
                        showPicker = false
                    }
                ) {
                    Text("Aceptar", color = AceColors.NeonRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar", color = AceColors.TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = AceColors.CardBg,
                titleContentColor = AceColors.TextPrimary,
                headlineContentColor = AceColors.TextPrimary,
                weekdayContentColor = AceColors.TextSecondary,
                subheadContentColor = AceColors.TextSecondary,
                yearContentColor = AceColors.TextSecondary,
                currentYearContentColor = AceColors.NeonRed,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = AceColors.NeonRed,
                dayContentColor = AceColors.TextPrimary,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = AceColors.NeonRed,
                todayContentColor = AceColors.NeonRed,
                todayDateBorderColor = AceColors.NeonRed
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── Error Content ─────────────────────────────────────────────────────────────

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
            tint = AceColors.NeonRed
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = AceColors.NeonRed,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        AceButtonFilled(
            text = "REINTENTAR",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        AceButtonOutlined(
            text = "CERRAR SESIÓN",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )
    }
}